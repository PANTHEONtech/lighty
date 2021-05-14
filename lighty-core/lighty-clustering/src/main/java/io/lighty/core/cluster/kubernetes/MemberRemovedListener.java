/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster.kubernetes;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
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

public class MemberRemovedListener extends AbstractActor {

    private static final Logger LOG = LoggerFactory.getLogger(MemberRemovedListener.class);

    private final Cluster cluster = Cluster.get(getContext().getSystem());
    private final DataBroker dataBroker;
    private final ClusterAdminService clusterAdminRPCService;

    public MemberRemovedListener(final DataBroker dataBroker,
            final ClusterAdminService clusterAdminRPCService) {

        LOG.info("{} created", this.getClass());
        this.dataBroker = dataBroker;
        this.clusterAdminRPCService = clusterAdminRPCService;
    }

    public static Props props(DataBroker dataBroker,
            ClusterAdminService clusterAdminRPCService) {
        return Props.create(MemberRemovedListener.class, () ->
                new MemberRemovedListener(dataBroker, clusterAdminRPCService));
    }

    @Override
    public void preStart() {
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class,
                ClusterEvent.UnreachableMember.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ClusterEvent.MemberRemoved.class, removedMember -> {
            LOG.info("Member detected as removed, processing: {}", removedMember.member().address());
            processRemovedMember(removedMember.member());
        }).build();
    }

    private void processRemovedMember(Member member) {
        LOG.info("Removing member {}", member.address());
        List<String> removedMemberRoles = member.getRoles().stream()
                .filter(role -> !role.contains("default")).collect(Collectors.toList());

        try {
            for (String removedMemberRole : removedMemberRoles) {
                ListenableFuture<RpcResult<RemoveAllShardReplicasOutput>> rpcResultListenableFuture =
                        clusterAdminRPCService.removeAllShardReplicas(new RemoveAllShardReplicasInputBuilder()
                                .setMemberName(removedMemberRole).build());
                RpcResult<RemoveAllShardReplicasOutput> removeAllShardReplicasResult = rpcResultListenableFuture.get();
                if (removeAllShardReplicasResult.isSuccessful()) {
                    LOG.info("RPC RemoveAllShards for member {} executed successfully", removedMemberRole);
                } else {
                    LOG.warn("RPC RemoveAllShards for member {} failed: {}", removedMemberRole,
                            removeAllShardReplicasResult.getErrors());
                }
            }
            LOG.info("Delete-Candidates transaction was successful");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Delete-Candidates transaction failed", e);
        }
    }

    /**
     * Find all occurrences where the member is registered as candidate for entity ownership.
     *
     * @param removedMember - member which is being removed from cluster
     * @return list of candidates
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

        for (EntityType entityType : owners.getEntityType().values()) {
            for (Entity entity : entityType.getEntity().values()) {
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
}
