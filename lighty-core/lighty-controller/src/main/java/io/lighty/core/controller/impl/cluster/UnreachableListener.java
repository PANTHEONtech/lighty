/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
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

    private final Cluster cluster = Cluster.get(getContext().getSystem());
    private final ActorSystem actorSystem;
    private final DataBroker dataBroker;
    private final ClusterAdminService clusterAdminRPCService;
    private final Long podRestartTimeout;
    private Set<Member> initialUnreachableSet;

    private static final String K8S_SCHEME = "https";
    private static final String K8S_HOST = "kubernetes";
    private static final String K8S_GET_PODS_PATH = "/api/v1/namespaces/default/pods";
    private static final String K8S_LIGHTY_SELECTOR = "lighty-k8s-cluster";
    private static final long DEFAULT_UNREACHABLE_RESTART_TIMEOUT = 30;

    public UnreachableListener(final ActorSystem actorSystem, final DataBroker dataBroker,
                               final ClusterAdminService clusterAdminRPCService, final Long podRestartTimeout) {
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
    }

    public static Props props(ActorSystem actorSystem, DataBroker dataBroker,
                              ClusterAdminService clusterAdminRPCService, Long podRestartTimeout) {
        return Props.create(UnreachableListener.class, () ->
                new UnreachableListener(actorSystem, dataBroker, clusterAdminRPCService, podRestartTimeout));
    }

    @Override
    public void preStart() {
        cluster.subscribe(
                getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class,
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
        return receiveBuilder()
                .match(
                        ClusterEvent.UnreachableMember.class,
                        mUnreachable -> {
                            if (initialUnreachableSet.contains(mUnreachable.member())) {
                                initialUnreachableSet.remove(mUnreachable.member());
                                LOG.info("Member {} was already removed during PreStart.", mUnreachable.member().address());
                                return;
                            }
                            LOG.info("Member detected as unreachable, processing: {}", mUnreachable.member().address());
                            processUnreachableMember(mUnreachable.member());
                        })
                .match(
                        ClusterEvent.MemberRemoved.class,
                        mRemoved -> LOG.info("Member was Removed: {}", mRemoved.member()))
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
            LOG.warn("Majority of cluster seems to be unreachable. This is probably due to network " +
                    "partition in which case the other side will resolve it (since they are the majority). " +
                    "Downing members from this side isn't safe");
        }
    }

    private boolean isMajorityReachable(int totalMembers, int unreachableMembers) {
        return (totalMembers - unreachableMembers) >= Math.floor((totalMembers + 1) / 2.0) + (totalMembers + 1) % 2;
    }

    /**
     * Switch members status to Down and clean his data from datastore
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
            LOG.error("Delete-Candidates transaction failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Find all occurrences where the member is registered as candidate for entity ownership
     *
     * @param removedMember - member which is being removed from cluster
     * @return
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
     * @return
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
                LOG.warn("Member {} is no longer listed among other cluster members. This is very unexpected, so " +
                        "lets try restarting him.", unreachablePodName);
                sendRestartRequest(unreachable, unreachablePodName);
            }

        }, podRestartTimeout, TimeUnit.SECONDS);
    }

    private void sendRestartRequest(Member unreachableMember, String unreachablePodName) {
        LOG.info("Member didn't return to reachable state, trying to restart its Pod");
        URIBuilder uriBuilder = getURIForKubernetesAPICall(K8S_GET_PODS_PATH + "/" + unreachablePodName);
        try {
            LOG.debug("Creating REST request for Deleting Pod: {}", uriBuilder.toString());
            HttpDelete deletePodRequest = new HttpDelete(uriBuilder.build());
            CloseableHttpClient httpClient = getHttpClient();
            Config k8sClient = new ConfigBuilder().build();
            deletePodRequest.addHeader("Authorization", "Bearer " + k8sClient.getOauthToken());
            deletePodRequest.addHeader("Content-Type", "application/json");
            LOG.debug("Executing REST request for Deleting Pod");
            CloseableHttpResponse response = httpClient.execute(deletePodRequest);
            LOG.trace("Response from Kubernetes: {}", response);
            String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            LOG.trace("Response Entity from Kubernetes: {}", result);
            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {
                LOG.info("Request successful. Kubernetes will restart Pod with name: {}", unreachablePodName);
                downMember(unreachableMember);
            } else if (response.getStatusLine().getStatusCode() == 404) {
                LOG.info("Request to delete Pod {} failed because the pod no longer exists. Safe to down member.",
                        unreachablePodName);
                downMember(unreachableMember);
            } else {
                LOG.error("Request to delete Pod {} failed. Not safe to down member. Response from Kubernetes: {}",
                        unreachablePodName, response);
            }
        } catch (Exception e) {
            LOG.error("Request to delete Pop failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Decide, whether the member is safe to Down without the risk of causing Split-Brain. Data from Kubernetes
     * are used for this decision.
     *
     * @param unreachableMember
     */
    public boolean safeToDownMember(Member unreachableMember) {
        Address unreachableAddress = unreachableMember.address();
        JSONObject podList = getAllLightyPods();
        if (podList == null) {
            LOG.error("List of Pods wasn't received. Can't decide whether it's safe to Down the unreachable member {}",
                    unreachableMember.address());
            return false;
        }
        JSONArray items = podList.getJSONArray("items");
        HashMap<String, JsonNode> podMap = new HashMap<>();
        for (int i = 0; i < items.length(); i++) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode podDetail = mapper.readTree(items.getJSONObject(i).toString());
                String podIP = podDetail.at("/status/podIP").asText();
                if (podIP != null && !podIP.isEmpty()) {
                    podMap.put(podIP, podDetail);
                } else {
                    LOG.debug("PodIP wasn't found in Pod info");
                    LOG.trace("Pod info: {}", podDetail);
                }
            } catch (IOException e) {
                LOG.warn("Couldn't get podIP from Pod info");
            }
        }
        LOG.debug("List of all Pod IPs: {}", podMap.keySet());
        if (unreachableAddress.host().nonEmpty()) {
            LOG.debug("Address of unreachable member is: {}", unreachableAddress.host().get());
            if (podMap.containsKey(unreachableAddress.host().get())) {
                LOG.debug("IP of unreachable was found in Pods List. Checking container state");
                return analyzePodState(unreachableMember, podMap.get(unreachableAddress.host().get()));
            } else {
                LOG.debug("IP of unreachable was not found in Pods List.. it is safe to delete it");
                return true;
            }
        }
        return false;
    }

    /**
     * Get data of all the Pods in Kubernetes running Lighty application
     *
     * @return JsonObject containing data about Pods running Lighty
     */
    private JSONObject getAllLightyPods() {
        LOG.debug("Getting Lighty Pods from Kubernetes");
        try {
            CloseableHttpClient httpClient = getHttpClient();
            HttpGet request = new HttpGet(getURIForKubernetesAPICall(K8S_GET_PODS_PATH).
                    setParameter("labelSelector", "app=" + K8S_LIGHTY_SELECTOR).build());

            Config k8sClient = new ConfigBuilder().build();
            request.addHeader("Authorization", "Bearer " + k8sClient.getOauthToken());
            request.addHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(request);
            String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            LOG.debug("Get Lighty Pods from Kubernetes result: {}", result);
            return new JSONObject(result);
        } catch (Exception e) {
            LOG.error("Requesting Pods from Kubernetes failed = {}", e.toString(), e);
        }
        return null;
    }

    private CloseableHttpClient getHttpClient() throws KeyStoreException, NoSuchAlgorithmException,
            KeyManagementException {
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
                NoopHostnameVerifier.INSTANCE);
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        LOG.debug("SSL factory and HttpClient created");
        return httpClient;
    }

    private URIBuilder getURIForKubernetesAPICall(String path) {
        URIBuilder uri = new URIBuilder();
        uri.setScheme(K8S_SCHEME);
        uri.setHost(K8S_HOST);
        uri.setPath(path);
        return uri;
    }

    /**
     * In case the unreachable member's Pod exists, check it's container status since it may be in the process
     * of terminating.
     *
     * @param unreachableMember - unreachable member
     * @param podInfo           - data of the unreachable member's pod
     * @return
     */
    private boolean analyzePodState(Member unreachableMember, JsonNode podInfo) {
        try {
            JsonNode containerStatusesNode = podInfo.at("/status/containerStatuses");
            if (!containerStatusesNode.isMissingNode() && containerStatusesNode.isArray()
                    && containerStatusesNode.size() > 0) {
                ArrayNode containerStatuses = (ArrayNode) containerStatusesNode;
                if (!containerStatuses.get(0).at("/ready").asBoolean()) {
                    if (!containerStatuses.get(0).at("/state/terminated").isMissingNode()) {
                        LOG.debug("Found state container - Terminated, safe to Down member");
                        return true;
                    }
                    LOG.debug("State container doesn't say Terminated");
                } else {
                    LOG.debug("ContainerStatus is READY");
                }
            } else {
                LOG.warn("ContainerStatuses list missing or empty");
                LOG.debug("ContainerStatuses detail: {}", podInfo);
            }
        } catch (Exception e) {
            LOG.error("Failed to analyze Pod info of unreachable member. Reason: {}", e.getMessage(), e);
        }
        String name = podInfo.at("/metadata/name").asText();
        schedulePodRestart(unreachableMember, name);
        return false;
    }
}
