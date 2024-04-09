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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.datastore.admin.ClusterAdminRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.RemoveAllShardReplicasOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemberRemovedListener extends AbstractActor {

    private static final Logger LOG = LoggerFactory.getLogger(MemberRemovedListener.class);

    private final Cluster cluster;
    private final ClusterAdminRpcService clusterAdminRPCService;

    public MemberRemovedListener(final ClusterAdminRpcService clusterAdminRPCService) {
        LOG.info("{} created", this.getClass());
        this.cluster = Cluster.get(super.getContext().getSystem());
        this.clusterAdminRPCService = clusterAdminRPCService;
    }

    public static Props props(ClusterAdminRpcService clusterAdminRPCService) {
        return Props.create(MemberRemovedListener.class, () -> new MemberRemovedListener(clusterAdminRPCService));
    }

    @Override
    public void preStart() {
        LOG.info("Starting {}", this.getClass());
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberRemoved.class);
    }

    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClusterEvent.MemberRemoved.class, removedMember -> {
                    LOG.info("Member detected as removed, processing: {}", removedMember.member().address());
                    processRemovedMember(removedMember.member());
                })
                .build();
    }

    private void processRemovedMember(Member member) {
        LOG.info("Removing shard replicas for member {}. May result in WARN (DOES_NOT_EXIST) messages if already"
                + "removed by another member.", member.address());
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
            LOG.info("Shard replicas removed for member {}", member.address());
        } catch (ExecutionException e) {
            LOG.error("Unable to remove shard replicas for member {}", member.address(), e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while removing shard replicas for member {}", member.address(), e);
            Thread.currentThread().interrupt();
        }
    }

}
