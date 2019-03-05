/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the LIGHTY.IO LICENSE,
 * version 1.1. If a copy of the license was not distributed with this file,
 * You can obtain one at https://lighty.io/license/1.1/
 */
package io.lighty.modules.southbound.openflow.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.southbound.openflow.impl.config.ConfigurationServiceFactory;
import io.lighty.modules.southbound.openflow.impl.config.OpenflowpluginConfiguration;
import io.lighty.modules.southbound.openflow.impl.config.SwitchConfig;
import org.opendaylight.openflowjava.protocol.spi.connection.SwitchConnectionProvider;
import org.opendaylight.openflowplugin.api.diagstatus.OpenflowPluginDiagStatusProvider;
import org.opendaylight.openflowplugin.api.openflow.OpenFlowPluginProvider;
import org.opendaylight.openflowplugin.api.openflow.configuration.ConfigurationService;
import org.opendaylight.openflowplugin.api.openflow.mastership.MastershipChangeException;
import org.opendaylight.openflowplugin.applications.arbitratorreconciliation.impl.ArbitratorReconciliationManagerImpl;
import org.opendaylight.openflowplugin.applications.frm.impl.ForwardingRulesManagerImpl;
import org.opendaylight.openflowplugin.applications.frm.recovery.impl.OpenflowServiceRecoveryHandlerImpl;
import org.opendaylight.openflowplugin.applications.reconciliation.impl.ReconciliationManagerImpl;
import org.opendaylight.openflowplugin.applications.topology.manager.FlowCapableTopologyProvider;
import org.opendaylight.openflowplugin.applications.topology.manager.NodeChangeListenerImpl;
import org.opendaylight.openflowplugin.applications.topology.manager.OperationProcessor;
import org.opendaylight.openflowplugin.applications.topology.manager.TerminationPointChangeListenerImpl;
import org.opendaylight.openflowplugin.impl.OpenFlowPluginProviderFactoryImpl;
import org.opendaylight.openflowplugin.impl.mastership.MastershipChangeServiceManagerImpl;
import org.opendaylight.serviceutils.srm.impl.ServiceRecoveryRegistryImpl;
import org.opendaylight.serviceutils.upgrade.impl.UpgradeStateListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.arbitrator.reconcile.service.rev180227.ArbitratorReconcileService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.forwardingrules.manager.config.rev160511.ForwardingRulesManagerConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.frm.reconciliation.service.rev180227.FrmReconciliationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.UpgradeConfigBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class OpenflowSouthboundPlugin extends AbstractLightyModule implements OpenflowServices {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSouthboundPlugin.class);

    private final ConfigurationService configurationService;
    private final LightyServices lightyServices;
    private OpenFlowPluginProvider openFlowPluginProvider;
    private final List<SwitchConnectionProvider> providers;
    private ForwardingRulesManagerImpl forwardingRulesManagerImpl;
    private OpenflowServiceRecoveryHandlerImpl openflowServiceRecoveryHandlerImpl;
    private ForwardingRulesManagerConfigBuilder frmConfigBuilder;
    private ArbitratorReconciliationManagerImpl arbitratorReconciliationManager;
    private PacketProcessingListener packetProcessingListener;
    private ListenerRegistration<NotificationListener> packetListenerNotificationRegistration;
    private OperationProcessor operationProcessor;
    private FlowCapableTopologyProvider flowCapableTopologyProvider;

    public OpenflowSouthboundPlugin(final LightyServices lightyServices, final ExecutorService executorService) {

        super(executorService);

        this.lightyServices = lightyServices;
        this.providers = new SwitchConfig().getDefaultProviders();
        this.configurationService = new ConfigurationServiceFactory().newInstance(new OpenflowpluginConfiguration()
                .getDefaultProviderConfig());
    }

    /**
     * Create OFP instance.
     * @param lightyServices Required Lighty services.
     * @param openflowProviderConfig Required configuration for OFP.
     * @param providers Required Providers.
     * @param executorService Optional ExecutorService for LightyModule.
     * @param frmConfigBuilder If is not provide, OFP start without FRM.
     * @param packetProcessingListener If is not provide, OFP will be handling packets arrived to controller by default.
     */
    public OpenflowSouthboundPlugin(@Nonnull final LightyServices lightyServices,
                                    @Nonnull final OpenflowProviderConfig openflowProviderConfig,
                                    @Nonnull final List<SwitchConnectionProvider> providers,
                                    @Nullable final ExecutorService executorService,
                                    @Nullable final ForwardingRulesManagerConfigBuilder frmConfigBuilder,
                                    @Nullable final PacketProcessingListener packetProcessingListener) {

        super(executorService);

        this.lightyServices = lightyServices;
        this.providers = providers;
        this.configurationService = new ConfigurationServiceFactory().newInstance(openflowProviderConfig);
        this.frmConfigBuilder = frmConfigBuilder;
        this.packetProcessingListener = packetProcessingListener;
    }

    @Override
    protected boolean initProcedure() {
        if (this.openFlowPluginProvider == null) {
            MastershipChangeServiceManagerImpl mastershipChangeServiceManager = new MastershipChangeServiceManagerImpl();
            final OpenflowPluginDiagStatusProvider diagStat = new OpenflowPluginDiagStatusProvider(this.lightyServices
                    .getDiagStatusService(), this.providers);
            this.openFlowPluginProvider = new OpenFlowPluginProviderFactoryImpl()
                    .newInstance(
                            this.configurationService,
                            this.lightyServices.getControllerBindingDataBroker(),
                            this.lightyServices.getControllerRpcProviderRegistry(),
                            this.lightyServices.getControllerBindingNotificationPublishService(),
                            this.lightyServices.getEntityOwnershipService(),
                            this.providers,
                            this.lightyServices.getClusterSingletonServiceProvider(),
                            mastershipChangeServiceManager, diagStat, this.lightyServices.getSystemReadyMonitor());
            if (this.openFlowPluginProvider == null) {
                throw new RuntimeException("Openflow plugin provider initialization failed.");
            }

            //start ForwardingRulesManager in OFP
            if (frmConfigBuilder != null) {
                //ArbitratorReconciliation implementation
                final ReconciliationManagerImpl reconciliationManagerImpl
                        = new ReconciliationManagerImpl(mastershipChangeServiceManager);
                UpgradeStateListener upgradeStateListener
                        = new UpgradeStateListener(this.lightyServices.getControllerBindingDataBroker(),
                        new UpgradeConfigBuilder().build());
                try {
                    reconciliationManagerImpl.start();
                } catch (MastershipChangeException e) {
                    LOG.error("Failed registration ReconciliationManagerImpl", e);
                }
                this.arbitratorReconciliationManager
                        = new ArbitratorReconciliationManagerImpl(this.lightyServices.getControllerRpcProviderRegistry(),
                        reconciliationManagerImpl,
                        upgradeStateListener);
                this.arbitratorReconciliationManager.start();
                this.lightyServices.getControllerRpcProviderRegistry()
                        .addRpcImplementation(ArbitratorReconcileService.class,
                                this.arbitratorReconciliationManager);

                //FRM implementation
                final ServiceRecoveryRegistryImpl serviceRecoveryRegistryImpl = new ServiceRecoveryRegistryImpl();
                this.openflowServiceRecoveryHandlerImpl = new OpenflowServiceRecoveryHandlerImpl(serviceRecoveryRegistryImpl);
                this.forwardingRulesManagerImpl
                        = new ForwardingRulesManagerImpl(this.lightyServices.getControllerBindingDataBroker(),
                        this.lightyServices.getControllerRpcProviderRegistry(),
                        this.frmConfigBuilder.build(),
                        mastershipChangeServiceManager,
                        this.lightyServices.getClusterSingletonServiceProvider(),
                        this.configurationService,
                        reconciliationManagerImpl,
                        this.openflowServiceRecoveryHandlerImpl,
                        serviceRecoveryRegistryImpl);
                this.forwardingRulesManagerImpl.start();

                LOG.info("OFP started with FRM & ARM");
            }

            //Topology manager
            this.operationProcessor = new OperationProcessor(this.lightyServices.getControllerBindingDataBroker());
            this.operationProcessor.start();
            TerminationPointChangeListenerImpl terminationPointChangeListener
                    = new TerminationPointChangeListenerImpl(this.lightyServices.getControllerBindingDataBroker(),
                    this.operationProcessor);
            NodeChangeListenerImpl nodeChangeListener
                    = new NodeChangeListenerImpl(this.lightyServices.getControllerBindingDataBroker(),
                    this.operationProcessor);
            this.flowCapableTopologyProvider
                    = new FlowCapableTopologyProvider(this.lightyServices.getControllerBindingDataBroker(),
                    this.lightyServices.getControllerNotificationProviderService(),
                    this.operationProcessor,
                    this.lightyServices.getClusterSingletonServiceProvider());
            this.flowCapableTopologyProvider.start();

            //OFP packet listener initialize
            if (this.packetProcessingListener != null) {
                this.packetListenerNotificationRegistration
                        = this.lightyServices.getControllerNotificationProviderService()
                              .registerNotificationListener(this.packetProcessingListener);
                LOG.info("OfpPacketListener Started.");
            }

            return true;
        } else {
            LOG.warn("Openflow-plugin provider is not null. Instance is already running.");
            return false;
        }
    }

    @Override
    protected boolean stopProcedure() {
        destroy(this.packetListenerNotificationRegistration);
        destroy(this.flowCapableTopologyProvider);
        destroy(this.operationProcessor);
        destroy(this.forwardingRulesManagerImpl);
        destroy(this.arbitratorReconciliationManager);
        destroy(this.openFlowPluginProvider);

        return true;
    }

    /**
     * Start close() method in AutoCloseable instance
     * @param instance instance of {@link AutoCloseable}
     */
    private void destroy(AutoCloseable instance){
        if (instance != null) {
            try {
                instance.close();
            } catch (final Exception e) {
                LOG.warn("Exception was thrown during closing " + instance.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Expose FrmReconciliationService,
     * @return return null if ForwardingRulesManager wasn't initialized
     */
    @Override
    public FrmReconciliationService getFrmReconciliationService() {
        if (this.forwardingRulesManagerImpl == null) {
            return null;
        }
        return this.lightyServices.getControllerRpcProviderRegistry().getRpcService(FrmReconciliationService.class);
    }
}