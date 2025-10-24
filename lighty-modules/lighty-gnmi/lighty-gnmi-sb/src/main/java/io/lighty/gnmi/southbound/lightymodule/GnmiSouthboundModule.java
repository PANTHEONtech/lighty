/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils.OPENCONFIG_YANG_MODELS;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.provider.GnmiSouthboundProvider;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoaderService;
import io.lighty.gnmi.southbound.schema.loader.impl.ByClassPathYangLoaderService;
import io.lighty.gnmi.southbound.schema.loader.impl.ByPathYangLoaderService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GnmiSouthboundModule extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSouthboundModule.class);
    private final LightyServices lightyServices;
    private final AAAEncryptionService encryptionService;
    private final GnmiConfiguration gnmiConfiguration;
    private final ExecutorService gnmiExecutorService;
    private final CrossSourceStatementReactor customReactor;
    private GnmiSouthboundProvider gnmiProvider;

    public GnmiSouthboundModule(final LightyServices services, final ExecutorService gnmiExecutorService,
                                final AAAEncryptionService encryptionService,
                                @Nullable final GnmiConfiguration configuration,
                                @Nullable final CrossSourceStatementReactor customReactor) {

        this.lightyServices = Objects.requireNonNull(services);
        this.gnmiExecutorService = Objects.requireNonNull(gnmiExecutorService);
        this.encryptionService = encryptionService;
        this.gnmiConfiguration = configuration;
        this.customReactor = customReactor;
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Starting lighty gNMI Southbound Module");
        final List<YangLoaderService> initialLoaders = prepareByPathLoaders();
        try {
            gnmiProvider = new GnmiSouthboundProvider(lightyServices.getDOMMountPointService(),
                    lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(), gnmiExecutorService,
                    initialLoaders, encryptionService, customReactor);

            gnmiProvider.init();
            return true;
        } catch (ExecutionException | TimeoutException | YangLoadException e) {
            LOG.error("Unable to initialize gNMI Provider", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while initializing gNMI Provider", e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @SuppressWarnings({"checkstyle:illegalCatch"})
    @Override
    protected boolean stopProcedure() {
        LOG.info("Stopping lighty gNMI Southbound Module");
        try {
            gnmiProvider.close();
            return true;
        } catch (Exception e) {
            LOG.warn("Exception while closing gNMI Provider", e);
        }
        return false;
    }

    private List<YangLoaderService> prepareByPathLoaders() {
        final List<YangLoaderService> services = new ArrayList<>();
                services.add(new ByClassPathYangLoaderService(OPENCONFIG_YANG_MODELS));
        return services;
    }

}
