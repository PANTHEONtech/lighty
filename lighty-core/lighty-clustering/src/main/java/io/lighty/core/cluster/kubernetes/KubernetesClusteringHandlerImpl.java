/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster.kubernetes;

import akka.cluster.Cluster;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.lighty.core.cluster.ClusteringHandler;
import io.lighty.core.cluster.config.ClusteringConfigUtils;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.AddReplicasForAllShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.AddReplicasForAllShardsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev250131.AddReplicasForAllShardsInputBuilder;
import org.opendaylight.yangtools.binding.Rpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesClusteringHandlerImpl implements ClusteringHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClusteringHandlerImpl.class);

    private final Config akkaDeploymentConfig;
    private final ActorSystemProvider actorSystemProvider;
    private Optional<Config> moduleShardsConfig;

    public KubernetesClusteringHandlerImpl(@NonNull final ActorSystemProvider actorSystemProvider,
                                           @NonNull final Config akkaDeploymentConfig) {
        this.actorSystemProvider = actorSystemProvider;
        this.akkaDeploymentConfig = akkaDeploymentConfig;
        this.moduleShardsConfig = Optional.empty();
    }

    /**
     * Initialize Cluster Bootstrap. If this instance is the cluster leader, create custom module-shards.conf
     * specifying that all shards should be created on this member. If this instance is not the leader the
     * default module-shards.conf will be used. In this case shards will not be created but received from leader
     * as snapshots and installed.
     */
    @Override
    public void initClustering() {
        LOG.info("Starting ClusterBootstrap");
        ClusterBootstrap clusterBootstrap = ClusterBootstrap.get(actorSystemProvider.getActorSystem());
        clusterBootstrap.start();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            LOG.info("Waiting for cluster to form");
            final ListenableScheduledFuture clusterLeaderElectionFuture = getClusterLeaderElectionFuture(latch);
            latch.await();
            clusterLeaderElectionFuture.cancel(true);
        } catch (InterruptedException e) {
            LOG.error("Error occurred while waiting for the Cluster to form", e);
            Thread.currentThread().interrupt();
            return;
        }

        LOG.info("Cluster is formed, leader= {}",
                Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader());
        if (Cluster.get(actorSystemProvider.getActorSystem()).selfAddress()
                .equals(Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader())) {
            LOG.info("I am leader, generating custom module-shards.conf");
            final List<String> memberRoles = akkaDeploymentConfig.getStringList("akka.cluster.roles");
            final String data = ClusteringConfigUtils.generateModuleShardsForMembers(memberRoles);
            moduleShardsConfig = Optional.of(ConfigFactory.parseString(data));
            return;
        }
        LOG.info("Using default module-shards.conf");
    }

    @Override
    public void start(@NonNull final RpcService clusterAdminRPCService) {
        this.actorSystemProvider.getActorSystem().actorOf(
                MemberRemovedListener.props(clusterAdminRPCService), "memberRemovedListener");
        this.askForShards(clusterAdminRPCService.getRpc(AddReplicasForAllShards.class));
    }

    @Override
    public Optional<Config> getModuleShardsConfig() {
        return this.moduleShardsConfig;
    }

    /**
     * The first member of the cluster (leader) will create his shards. Other joining members will query
     * the leader for snapshots of the shards.
     */
    private void askForShards(final Rpc<AddReplicasForAllShardsInput, ?> clusterAdminRpcService) {
        if (!Cluster.get(actorSystemProvider.getActorSystem()).selfAddress()
                .equals(Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader())) {
            LOG.info("RPC call - Asking for Shard Snapshots");
            try {
                final var rpcResult = clusterAdminRpcService.invoke(
                        new AddReplicasForAllShardsInputBuilder().build()).get();
                LOG.debug("RPC call - Asking for Shard Snapshots result: {}", rpcResult.getResult());
            } catch (ExecutionException e) {
                LOG.error("RPC call - Asking for Shard Snapshots failed", e);
            } catch (InterruptedException e) {
                LOG.error("RPC call - Asking for Shard Snapshots interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Wait for the cluster to form and then release the latch.
     */
    private ListenableScheduledFuture getClusterLeaderElectionFuture(CountDownLatch latch) {
        ListeningScheduledExecutorService listeningScheduledExecutorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        return listeningScheduledExecutorService.scheduleAtFixedRate(() -> {
            if (Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader() != null) {
                latch.countDown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
