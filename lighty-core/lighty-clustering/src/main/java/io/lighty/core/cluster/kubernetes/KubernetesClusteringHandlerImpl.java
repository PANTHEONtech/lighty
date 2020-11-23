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
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.AddReplicasForAllShardsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesClusteringHandlerImpl implements ClusteringHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesClusteringHandlerImpl.class);
    public static final String K8S_DEFAULT_POD_NAMESPACE = "default";

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
    public void start(@NonNull ClusterSingletonServiceProvider clusterSingletonServiceProvider,
                      @NonNull ClusterAdminService clusterAdminRPCService, @NonNull DataBroker bindingDataBroker) {
        Long podRestartTimeout = null;
        if (this.akkaDeploymentConfig.hasPath(ClusteringConfigUtils.K8S_POD_RESTART_TIMEOUT_PATH)) {
            podRestartTimeout = this.akkaDeploymentConfig.getLong(ClusteringConfigUtils.K8S_POD_RESTART_TIMEOUT_PATH);
        }
        Optional<String> optPodNamespace = ClusteringConfigUtils.getPodNamespaceFromConfig(this.akkaDeploymentConfig);
        String podNamespace;
        if (optPodNamespace.isPresent()) {
            podNamespace = optPodNamespace.get();
        } else {
            LOG.info("akka.discovery.kubernetes-api.pod-namespace wasn't specified in .conf file, "
                    + "using k8s default value: {} ", K8S_DEFAULT_POD_NAMESPACE);
            podNamespace = K8S_DEFAULT_POD_NAMESPACE;
        }

        Optional<String> optPodSelector = ClusteringConfigUtils.getPodSelectorFromConfig(this.akkaDeploymentConfig);
        String podSelector;
        if (optPodSelector.isPresent()) {
            podSelector = optPodSelector.get();
        } else {
            String defaultPodSelector = this.actorSystemProvider.getActorSystem().name();
            LOG.warn("akka.discovery.kubernetes-api.pod-label-selector wasn't specified in .conf file, "
                    + "using k8s default value (akka actor system name): {} "
                    + "Make sure that the value match the deployment label selector", defaultPodSelector);
            podSelector = defaultPodSelector;
        }

        clusterSingletonServiceProvider.registerClusterSingletonService(
                new UnreachableListenerService(actorSystemProvider.getActorSystem(), bindingDataBroker,
                        clusterAdminRPCService, podNamespace, podSelector, podRestartTimeout));
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
            LOG.debug("RPC call - Asking for Shard Snapshots");
            try {
                RpcResult<AddReplicasForAllShardsOutput> rpcResult = clusterAdminRPCService.addReplicasForAllShards(
                        new AddReplicasForAllShardsInputBuilder().build()).get();
                LOG.debug("RPC call - Asking for Shard Snapshots result: {}", rpcResult.getResult());
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("RPC call - Asking for Shard Snapshots failed", e);
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
