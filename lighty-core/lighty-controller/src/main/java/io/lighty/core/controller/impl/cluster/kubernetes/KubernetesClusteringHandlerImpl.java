/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.cluster.kubernetes;

import static io.lighty.core.controller.impl.util.ControllerConfigUtils.MODULE_SHARDS_TMP_PATH;
import static io.lighty.core.controller.impl.util.ControllerConfigUtils.generateModuleShardsForMembers;

import akka.cluster.Cluster;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.typesafe.config.Config;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.cluster.ClusteringHandler;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesClusteringHandlerImpl implements ClusteringHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClusteringHandlerImpl.class);

    private final Config akkaDeploymentConfig;
    private final LightyController controller;
    private Optional<String> moduleShardsConfig;

    public KubernetesClusteringHandlerImpl(@NonNull LightyController controller, @NonNull Config akkaDeploymentConfig) {
        this.akkaDeploymentConfig = akkaDeploymentConfig;
        this.controller = controller;
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
        ActorSystemProvider actorSystemProvider = controller.getServices().getActorSystemProvider();
        ClusterBootstrap clusterBootstrap = ClusterBootstrap.get(actorSystemProvider.getActorSystem());
        clusterBootstrap.start();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            LOG.info("Waiting for cluster to form");
            ListenableScheduledFuture clusterLeaderElectionFuture = getClusterLeaderElectionFuture(latch);
            latch.await();
            clusterLeaderElectionFuture.cancel(true);
        } catch (InterruptedException e) {
            LOG.error("Error occurred while waiting for the Cluster to form: {}", e.getMessage(), e);
            return;
        }

        LOG.info("Cluster is formed, leader= {}", Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader());
        if (Cluster.get(actorSystemProvider.getActorSystem()).selfAddress().
                equals(Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader())) {
            LOG.info("I am leader, generating custom module-shards.conf");
            try {
                List<String> memberRoles = akkaDeploymentConfig.getStringList("akka.cluster.roles");
                String data = generateModuleShardsForMembers(memberRoles);
                Files.write(Paths.get(MODULE_SHARDS_TMP_PATH), data.getBytes());
                this.moduleShardsConfig = Optional.of(MODULE_SHARDS_TMP_PATH);
                return;
            } catch (Exception e) {
                LOG.info("Tmp module-shards.conf file was not created - error received {}", e.getMessage());
            }
        }
        LOG.info("Using default module-shards.conf");
    }

    @Override
    public void start() {
        Long podRestartTimeout = null;
        if (this.akkaDeploymentConfig.hasPath(ControllerConfigUtils.K8S_POD_RESTART_TIMEOUT_PATH)) {
            podRestartTimeout = this.akkaDeploymentConfig.getLong(ControllerConfigUtils.K8S_POD_RESTART_TIMEOUT_PATH);
        }

        final LightyServices services = this.controller.getServices();
        controller.getServices().getClusterSingletonServiceProvider().registerClusterSingletonService(
                new UnreachableListenerService(services.getActorSystemProvider().getActorSystem(),
                        services.getBindingDataBroker(), services.getClusterAdminRPCService(), podRestartTimeout));
        this.askForShards();
    }

    @Override
    public Optional<String> getModuleConfig() {
        return this.moduleShardsConfig;
    }

    /**
     * The first member of the cluster (leader) will create his shards. Other joining members will query
     * the leader for snapshots of the shards.
     */
    private void askForShards() {
        final LightyServices services = this.controller.getServices();
        if (!Cluster.get(services.getActorSystemProvider().getActorSystem()).selfAddress().
                equals(Cluster.get(services.getActorSystemProvider().getActorSystem()).state().getLeader())) {
            LOG.debug("RPC call - Asking for Shard Snapshots");
            try {
                RpcResult<AddReplicasForAllShardsOutput> rpcResult =
                        services.getClusterAdminRPCService().addReplicasForAllShards(
                                new AddReplicasForAllShardsInputBuilder().build()).get();
                LOG.debug("RPC call - Asking for Shard Snapshots result: {}", rpcResult.getResult());
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("RPC call - Asking for Shard Snapshots failed: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Wait for the cluster to form and then release the latch.
     */
    private ListenableScheduledFuture getClusterLeaderElectionFuture(CountDownLatch latch) {
        ListeningScheduledExecutorService listeningScheduledExecutorService =
                MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        final ActorSystemProvider actorSystemProvider = controller.getServices().getActorSystemProvider();
        return listeningScheduledExecutorService.scheduleAtFixedRate(() -> {
            if (Cluster.get(actorSystemProvider.getActorSystem()).state().getLeader() != null) {
                latch.countDown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
