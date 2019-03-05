/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the LIGHTY.IO LICENSE,
 * version 1.1. If a copy of the license was not distributed with this file,
 * You can obtain one at https://lighty.io/license/1.1/
 */
package io.lighty.modules.southbound.openflow.impl;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.modules.southbound.openflow.impl.config.OpenflowpluginConfiguration;
import io.lighty.modules.southbound.openflow.impl.config.SwitchConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.forwardingrules.manager.config.rev160511.ForwardingRulesManagerConfigBuilder;
import java.util.concurrent.ExecutorService;

/**
 * Builder for {@link OpenflowSouthboundPlugin}.
 */
public class OpenflowSouthboundPluginBuilder {

    private LightyServices lightyServices;
    private OpenflowpluginConfiguration ofpConfiguration;
    private SwitchConfig switchConnectionProviders;
    private ExecutorService executorService = null;
    private PacketProcessingListener ofpPacketListener;
    private ForwardingRulesManagerConfigBuilder forwardingRulesManagerConfigBuilder;

    /**
     * Create new instance of {@link OpenflowSouthboundPluginBuilder} from {@link OpenflowpluginConfiguration},
     * {@link SwitchConfig} and {@link LightyServices}.
     * @param openflowpluginConfiguration input openflow configuration
     * @param lightyServices services from {@link LightyController}.
     * @return instance of {@link OpenflowSouthboundPluginBuilder}.
     */
    public OpenflowSouthboundPluginBuilder from(
            OpenflowpluginConfiguration openflowpluginConfiguration,
            LightyServices lightyServices) {
        this.ofpConfiguration = openflowpluginConfiguration;
        this.switchConnectionProviders = openflowpluginConfiguration.getSwitchConfig();
        this.lightyServices = lightyServices;
        return this;
    }

    /**
     * Inject executor service to execute futures
     * @param executorService instance of {@link ExecutorService}.
     * @return instance of {@link OpenflowSouthboundPluginBuilder}.
     */
    public OpenflowSouthboundPluginBuilder withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Inject Packet Listener, can be used for handling packet-in messages.
     * @param ofpPacketListener instance of {@link PacketProcessingListener}.
     * @return instance of {@link OpenflowSouthboundPluginBuilder}.
     */
    public OpenflowSouthboundPluginBuilder withPacketListener(PacketProcessingListener ofpPacketListener) {
        this.ofpPacketListener = ofpPacketListener;
        return this;
    }

    /**
     * Build new instance of {@link OpenflowSouthboundPlugin} from {@link OpenflowSouthboundPluginBuilder}.
     * @return instance of {@link OpenflowSouthboundPlugin}.
     */
    public OpenflowSouthboundPlugin build() {
        // If is ForwardingRulesManager disabled, then sending flow can be done only by RPC call from SalFlowService.
        if (ofpConfiguration.isEnableForwardingRulesManager()) {
            //OFP will provide FRM service to synchronized OFP config data-store with device and persistent data.
            //OFP also will provide sending FLOWs directly to device by RPC call.
            this.forwardingRulesManagerConfigBuilder = ofpConfiguration.getFrmConfigBuilder();
        }

        return new OpenflowSouthboundPlugin(lightyServices,
                ofpConfiguration.getOpenflowProviderConfig(),
                switchConnectionProviders.getProviders(),
                executorService,
                this.forwardingRulesManagerConfigBuilder,
                this.ofpPacketListener);
    }
}