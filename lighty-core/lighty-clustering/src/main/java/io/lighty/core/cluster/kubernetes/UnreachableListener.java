/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster.kubernetes;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.EntityOwners;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.Entity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnreachableListener extends AbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(UnreachableListener.class);
    private static final String K8S_LIGHTY_SELECTOR = "lighty-k8s-cluster";
    private static final long DEFAULT_UNREACHABLE_RESTART_TIMEOUT = 30;

    private final Cluster cluster = Cluster.get(getContext().getSystem());
    private final ActorSystem actorSystem;
    private final DataBroker dataBroker;
    private final ClusterAdminService clusterAdminRPCService;
    private final Long podRestartTimeout;
    private final Set<Member> initialUnreachableSet;
    private CoreV1Api kubernetesApi;

    public UnreachableListener(final ActorSystem actorSystem, final DataBroker dataBroker,
                               final ClusterAdminService clusterAdminRPCService,
                               final Long podRestartTimeout) {
        LOG.info("UnreachableListener created");

        this.dataBroker = dataBroker;
        this.clusterAdminRPCService = clusterAdminRPCService;
        this.actorSystem = actorSystem;
        this.initialUnreachableSet = new HashSet<>();
        if (podRestartTimeout == null || podRestartTimeout == 0) {
            this.podRestartTimeout = DEFAULT_UNREACHABLE_RESTART_TIMEOUT;
            LOG.info("Pod-restart-timeout wasn't loaded from akka-config, using default:{}", this.podRestartTimeout);
        } else {
            this.podRestartTimeout = podRestartTimeout;
            LOG.info("Pod-restart-timeout value was loaded from akka-config:{}", this.podRestartTimeout);
        }
        try {
            ApiClient client = ClientBuilder.cluster().build();
            Configuration.setDefaultApiClient(client);
            this.kubernetesApi = new CoreV1Api();
        } catch (IOException e) {
            LOG.error("IOException while initializing cluster ApiClient", e);
        }
    }

    public static Props props(ActorSystem actorSystem, DataBroker dataBroker,
                              ClusterAdminService clusterAdminRPCService, Long podRestartTimeout) {
        return Props.create(UnreachableListener.class, () ->
                new UnreachableListener(actorSystem, dataBroker, clusterAdminRPCService, podRestartTimeout));
    }

    @Override
    public void preStart() {

        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class,
                ClusterEvent.UnreachableMember.class);

        initialUnreachableSet.addAll(cluster.state().getUnreachable());
        if (!initialUnreachableSet.isEmpty()) {
            for (Member member : initialUnreachableSet) {
                LOG.info("PreStart: Member detected as unreachable, preparing for downing: {}", member.address());
                processUnreachableMember(member);
            }
        }
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ClusterEvent.UnreachableMember.class, mUnreachable -> {
            if (initialUnreachableSet.contains(mUnreachable.member())) {
                initialUnreachableSet.remove(mUnreachable.member());
                LOG.info("Member {} was already removed during PreStart.", mUnreachable.member().address());
                return;
            }
            LOG.info("Member detected as unreachable, processing: {}", mUnreachable.member().address());
            processUnreachableMember(mUnreachable.member());
        }).match(ClusterEvent.MemberRemoved.class, mRemoved -> LOG.info("Member was Removed: {}", mRemoved.member()))
                .build();
    }

    /**
     * Decide whether its safe to down the unreachable member.
     *
     * @param member - the member detected as unreachable
     */
    private void processUnreachableMember(Member member) {
        ClusterEvent.CurrentClusterState currentState = cluster.state();
        if (isMajorityReachable(((Collection<Member>) currentState.getMembers()).size(),
                currentState.getUnreachable().size())) {
            if (!safeToDownMember(member)) {
                LOG.info("It is not safe to down member {}", member.address());
                return;
            }
            downMember(member);
            LOG.info("Downing complete");
        } else {
            LOG.warn("Majority of cluster seems to be unreachable. This is probably due to network "
                    + "partition in which case the other side will resolve it (since they are the majority). "
                    + "Downing members from this side isn't safe");
        }
    }

    private boolean isMajorityReachable(int totalMembers, int unreachableMembers) {
        return (totalMembers - unreachableMembers) >= Math.floor((totalMembers + 1) / 2.0) + (totalMembers + 1) % 2;
    }

    /**
     * Switch members status to Down and clean his data from datastore.
     *
     * @param member - unreachable member
     */
    private void downMember(Member member) {
        LOG.info("Downing member {}", member.address());
        List<String> removedMemberRoles = member.getRoles().stream()
                .filter(role -> !role.contains("default")).collect(Collectors.toList());
        cluster.down(member.address());
        final WriteTransaction deleteTransaction = dataBroker.newWriteOnlyTransaction();
        for (InstanceIdentifier<Candidate> candidateIID : getCandidatesFromDatastore(member)) {
            LOG.debug("Deleting candidate: {}", candidateIID);
            deleteTransaction.delete(LogicalDatastoreType.OPERATIONAL, candidateIID);
        }
        try {
            for (String removedMemberRole : removedMemberRoles) {
                ListenableFuture<RpcResult<RemoveAllShardReplicasOutput>> rpcResultListenableFuture =
                        clusterAdminRPCService.removeAllShardReplicas(new RemoveAllShardReplicasInputBuilder()
                                .setMemberName(removedMemberRole).build());
                RpcResult<RemoveAllShardReplicasOutput> removeAllShardReplicasResult = rpcResultListenableFuture.get();
                if (removeAllShardReplicasResult.isSuccessful()) {
                    LOG.debug("RPC RemoveAllShards for member {} executed successfully", removedMemberRole);
                } else {
                    LOG.warn("RPC RemoveAllShards for member {} failed: {}", removedMemberRole,
                            removeAllShardReplicasResult.getErrors());
                }
            }
            deleteTransaction.commit().get();
            LOG.debug("Delete-Candidates transaction was successful");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Delete-Candidates transaction failed", e);
        }
    }

    /**
     * Find all occurrences where the member is registered as candidate for entity ownership.
     *
     * @param removedMember - member which is being removed from cluster
     */
    private List<InstanceIdentifier<Candidate>> getCandidatesFromDatastore(Member removedMember) {
        List<String> removedMemberRoles = removedMember.getRoles().stream()
                .filter(role -> !role.contains("default")).collect(Collectors.toList());
        LOG.debug("Getting Candidates from model EntityOwners for member's roles: {}", removedMemberRoles);
        List<InstanceIdentifier<Candidate>> candidatesToDelete = new LinkedList<>();
        EntityOwners owners;
        try (ReadTransaction readOwners = dataBroker.newReadOnlyTransaction()) {
            owners = readOwners.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(EntityOwners.class))
                    .get().orElse(null);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldn't read data from model EntityOwners", e);
            return Collections.emptyList();
        }

        for (EntityType entityType : owners.getEntityType()) {
            for (Entity entity : entityType.getEntity()) {
                for (Candidate candidate : entity.getCandidate()) {
                    if (removedMemberRoles.contains(candidate.getName())) {
                        LOG.debug("Found candidate in shard: {}", entity.getId());
                        InstanceIdentifier<Candidate> cand = InstanceIdentifier.builder(EntityOwners.class)
                                .child(EntityType.class, entityType.key())
                                .child(Entity.class, entity.key())
                                .child(Candidate.class, candidate.key()).build();
                        candidatesToDelete.add(cand);
                    }
                }
            }
        }
        LOG.debug("The removed member is registered as candidate in {}", candidatesToDelete.size());
        LOG.trace("The removed member is registered as: {}", candidatesToDelete);
        return candidatesToDelete;
    }

    /**
     * Ask Kubernetes to delete Pod of the unreachable member. Before this request is sent to Kubernetes, wait
     * for configured amount of time. After the timeout check again whether the member became reachable again.
     * If member becomes reachable again, abort restart.
     * If member remains unreachable, send the request.
     *
     * @param unreachable        - the unreachable member
     * @param unreachablePodName - Pod name of the unreachable member
     */
    private ListenableScheduledFuture schedulePodRestart(Member unreachable, String unreachablePodName) {
        if (unreachablePodName == null || unreachablePodName.isEmpty()) {
            LOG.error("Pod name was missing or empty. Can't schedule Pod restart.");
            return null;
        }
        LOG.info("Before restarting wait {}s. If member becomes reachable again, restart will be aborted",
                podRestartTimeout);
        ListeningScheduledExecutorService listeningScheduledExecutorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        return listeningScheduledExecutorService.schedule(() -> {
            ClusterEvent.CurrentClusterState state = Cluster.get(actorSystem).state();
            if (((Collection<Member>) state.getMembers()).contains(unreachable)) {
                if (state.getUnreachable().contains(unreachable)) {
                    LOG.debug("Requesting Kubernetes to restart the pod {}", unreachablePodName);
                    sendRestartRequest(unreachable, unreachablePodName);
                } else {
                    LOG.debug("Member {} is reachable again. Aborting POD restart", unreachablePodName);
                }
            } else {
                LOG.warn("Member {} is no longer listed among other cluster members. Trying to restart it.",
                        unreachablePodName);
                sendRestartRequest(unreachable, unreachablePodName);
            }

        }, podRestartTimeout, TimeUnit.SECONDS);
    }

    private void sendRestartRequest(Member unreachableMember, String unreachablePodName) {
        LOG.info("Member didn't return to reachable state, trying to restart its Pod");
        try {
            ApiResponse<V1Pod> response = this.kubernetesApi.deleteNamespacedPodWithHttpInfo(unreachablePodName,
                    "default", null, null, null,
                    null, null, null);

            int responseStatusCode = response.getStatusCode();
            if (responseStatusCode >= 200 && responseStatusCode < 300) {
                LOG.info("Request successful. Kubernetes will restart Pod with name: {}", unreachablePodName);
                downMember(unreachableMember);
            } else if (responseStatusCode == 404) {
                LOG.info("Request to delete Pod {} failed because the pod no longer exists. Safe to down member.",
                        unreachablePodName);
                downMember(unreachableMember);
            } else {
                LOG.error("Request to delete Pod {} failed. Not safe to down member. Response from Kubernetes: {}",
                        unreachablePodName, response);
            }
            /*
                API calls can return ApiException with response codes even when used with WithHttpInfo(),
                so we have to handle the 404 in catch as well -(in v kubernetes client version 10.0.0)
             */
        } catch (ApiException e) {
            LOG.debug("ApiException on api.deleteNamespacedPodWithHttpInfo", e);
            if (e.getCode() == 404) {
                LOG.info("Request to delete Pod {} failed because the pod no longer exists. Safe to down member.",
                        unreachablePodName);
                downMember(unreachableMember);
            } else {
                LOG.error("Unhandled response from API on api.deleteNamedSpacedPod with response code {} ."
                        + " Not safe to down member. ", e.getCode());
            }
        }
    }

    /**
     * Decide, whether the member is safe to Down without the risk of causing Split-Brain. Data from Kubernetes
     * are used for this decision.
     */
    public boolean safeToDownMember(Member unreachableMember) {
        V1PodList podList = getAllLightyPods();
        if (podList == null) {
            LOG.error("List of Pods wasn't received. Can't decide whether it's safe to Down the unreachable member {}",
                    unreachableMember.address());
            return false;
        }
        // Check if pods contains IP of unreachableMember
        boolean containsUnreachableIP = false;
        V1Pod conflictingPod = null;
        String unreachableMemberHostIP = unreachableMember.address().host().get();
        LOG.debug("Address of unreachable member is: {}", unreachableMemberHostIP);
        for (V1Pod pod : podList.getItems()) {
            LOG.debug("Pod: {} has PodIP: {}", pod.getMetadata().getName(), pod.getStatus().getPodIP());
            if (pod.getStatus().getPodIP().equals(unreachableMemberHostIP)) {
                containsUnreachableIP = true;
                conflictingPod = pod;
                break;
            }
        }
        if (containsUnreachableIP) {
            LOG.debug("IP of unreachable was found in Pods List. Checking container state");
            return analyzePodState(unreachableMember, conflictingPod);
        } else {
            LOG.debug("IP of unreachable was not found in Pods List.. it is safe to delete it");
            return true;
        }
    }

    /**
     * Get data of all the Pods in Kubernetes running Lighty application.
     *
     * @return JsonObject containing data about Pods running Lighty
     */
    private V1PodList getAllLightyPods() {
        LOG.debug("Getting Lighty Pods from Kubernetes");
        try {
            ApiResponse<V1PodList> apiResponse = this.kubernetesApi.listNamespacedPodWithHttpInfo("default",
                    null, null,
                    null, null,
                    "app=" + K8S_LIGHTY_SELECTOR,
                    null, null,
                    null, null);

            int responseStatusCode = apiResponse.getStatusCode();
            if (responseStatusCode >= 200 && responseStatusCode < 300) {
                LOG.info("Successfully retrieved Pods List");
                return apiResponse.getData();
            } else {
                LOG.warn("Error retrieving Pods List , Http status code = {}", responseStatusCode);
            }
        } catch (ApiException e) {
            LOG.debug("ApiException on api.listNamespacedPodWithHttpInfo", e);
            LOG.warn("Error retrieving Pods List , Http status code = {}", e.getCode());
        }
        return null;
    }

    /**
     * In case the unreachable member's Pod exists, check it's container status since it may be in the process
     * of terminating.
     *
     * @param unreachableMember - unreachable member
     * @param pod               - unreachable member's pod
     */
    private boolean analyzePodState(Member unreachableMember, V1Pod pod) {
        List<V1ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();

        if (containerStatuses != null && !containerStatuses.isEmpty()) {
            if (!containerStatuses.get(0).getReady()) {
                LOG.debug("State of the container is - {} ", containerStatuses.get(0).getState().toString());
                if (containerStatuses.get(0).getState().getTerminated() != null) {
                    LOG.debug("Found state container - Terminated, safe to Down member");
                    return true;
                } else {
                    LOG.debug("State of the container is not terminated");
                }
            } else {
                LOG.debug("ContainerStatus is READY");
            }
        } else {
            LOG.warn("ContainerStatuses list missing or empty");
            LOG.debug("ContainerStatuses detail: {}", pod.getStatus().getContainerStatuses());
        }
        schedulePodRestart(unreachableMember, pod.getMetadata().getName());
        return false;
    }
}
