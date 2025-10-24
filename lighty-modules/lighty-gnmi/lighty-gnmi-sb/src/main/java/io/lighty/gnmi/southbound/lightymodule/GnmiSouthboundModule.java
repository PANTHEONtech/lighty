/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils.OPENCONFIG_YANG_MODELS;

import io.lighty.gnmi.southbound.provider.GnmiSouthboundProvider;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoaderService;
import io.lighty.gnmi.southbound.schema.loader.impl.ByClassPathYangLoaderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Declarative Service component starting the gNMI southbound provider.
 */
@Component(immediate = true, service = GnmiSouthboundModule.class)
public final class GnmiSouthboundModule {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSouthboundModule.class);

    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderService;
    private final DOMMountPointService domMountPointService;
    private final AAAEncryptionService encryptionService;

    private ExecutorService gnmiExecutor;
    private GnmiSouthboundProvider gnmiProvider;

    @Activate
    public GnmiSouthboundModule(@Reference DataBroker dataBroker, @Reference RpcProviderService rpcProviderService,
            @Reference DOMMountPointService domMountPointService, @Reference AAAEncryptionService encryptionService) {
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.rpcProviderService = Objects.requireNonNull(rpcProviderService);
        this.domMountPointService = Objects.requireNonNull(domMountPointService);
        this.encryptionService = Objects.requireNonNull(encryptionService);

    }

    @Activate
    public void init() {
        LOG.info("Starting ODL gNMI Southbound Component");
        gnmiExecutor = Executors.newFixedThreadPool(4);
        try {
            gnmiProvider = new GnmiSouthboundProvider(
                domMountPointService,
                dataBroker,
                rpcProviderService,
                gnmiExecutor,
                prepareByPathLoaders(),
                encryptionService,
                null);
            gnmiProvider.init();
            LOG.info("gNMI Southbound Provider initialized");
        } catch (ExecutionException | TimeoutException | YangLoadException e) {
            LOG.error("Unable to initialize gNMI Provider", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while initializing gNMI Provider", e);
        }
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    @Deactivate
    public void close() {
        LOG.info("Stopping ODL gNMI Southbound Component");
        if (gnmiProvider != null) {
            try {
                gnmiProvider.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (gnmiExecutor != null) {
            gnmiExecutor.shutdownNow();
        }
    }

    private List<YangLoaderService> prepareByPathLoaders() {
        final List<YangLoaderService> services = new ArrayList<>();
        services.add(new ByClassPathYangLoaderService(OPENCONFIG_YANG_MODELS));
        return services;
    }
}
