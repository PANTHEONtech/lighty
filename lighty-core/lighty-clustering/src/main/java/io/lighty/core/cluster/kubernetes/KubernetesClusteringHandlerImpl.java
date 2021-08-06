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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lighty.core.cluster.ClusteringHandler;
import io.lighty.core.cluster.config.ClusteringConfigUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesClusteringHandlerImpl implements ClusteringHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClusteringHandlerImpl.class);

    private final Config akkaDeploymentConfig;
    private final ActorSystemProvider actorSystemProvider;
    private Optional<String> moduleShardsConfig;

    public KubernetesClusteringHandlerImpl(@NonNull ActorSystemProvider actorSystemProvider,
                                           @NonNull Config akkaDeploymentConfig) {
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
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    @Override
    public void initClustering() {
        LOG.info("Starting ClusterBootstrap");
        ClusterBootstrap clusterBootstrap = ClusterBootstrap.get(actorSystemProvider.getActorSystem());
        clusterBootstrap.start();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            LOG.info("Waiting for cluster to form");
            ListenableScheduledFuture clusterLeaderElectionFuture = getClusterLeaderElectionFuture(latch);
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
            try {
                List<String> memberRoles = akkaDeploymentConfig.getStringList("akka.cluster.roles");
                String data = ClusteringConfigUtils.generateModuleShardsForMembers(memberRoles);
                Files.write(Paths.get(ClusteringConfigUtils.MODULE_SHARDS_TMP_PATH),
                        data.getBytes(StandardCharsets.UTF_8));
                this.moduleShardsConfig = Optional.of(ClusteringConfigUtils.MODULE_SHARDS_TMP_PATH);
                return;
            } catch (IOException e) {
                LOG.info("Tmp module-shards.conf file was not created - error received {}", e.getMessage());
            }
        }
        LOG.info("Using default module-shards.conf");
    }

    @Override
    public void start(@NonNull ClusterAdminService clusterAdminRPCService) {
        this.actorSystemProvider.getActorSystem().actorOf(
                MemberRemovedListener.props(clusterAdminRPCService), "memberRemovedListener");
        this.askForShards(clusterAdminRPCService);
    }

    @Override
    public Optional<String> getModuleConfig() {
        return this.moduleShardsConfig;
    }

    /**
     * The first member of the cluster (leader) will create his shards. Other joining members will query
     * the leader for snapshots of the shards.
     */
    private void askForShards(ClusterAdminService clusterAdminRPCService) {
        if (!Cluster.get(actorSystemProvider.getActorSystem()).selfAddress()
                .equals(Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader())) {
            LOG.info("RPC call - Asking for Shard Snapshots");
            try {
                RpcResult<AddReplicasForAllShardsOutput> rpcResult = clusterAdminRPCService.addReplicasForAllShards(
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
