/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static org.opendaylight.gnmi.southbound.yangmodule.util.GnmiConfigUtils.OPENCONFIG_YANG_MODELS;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.gnmi.southbound.provider.GnmiSouthboundProvider;
import org.opendaylight.gnmi.southbound.schema.loader.api.YangLoadException;
import org.opendaylight.gnmi.southbound.schema.loader.api.YangLoaderService;
import org.opendaylight.gnmi.southbound.schema.loader.impl.ByClassPathYangLoaderService;
import org.opendaylight.gnmi.southbound.schema.loader.impl.ByPathYangLoaderService;
import org.opendaylight.gnmi.southbound.yangmodule.config.GnmiConfiguration;
import org.opendaylight.yangtools.yang.model.spi.source.YangTextToIRSourceTransformer;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.ri.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.source.ir.DefaultYangTextToIRSourceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightyGnmiSouthboundModule extends AbstractLightyModule {

    private static final Logger LOG = LoggerFactory.getLogger(LightyGnmiSouthboundModule.class);
    private final LightyServices lightyServices;
    private final AAAEncryptionService encryptionService;
    private final GnmiConfiguration gnmiConfiguration;
    private final ExecutorService gnmiExecutorService;
    private final YangParserFactory parserFactory;
    private final YangTextToIRSourceTransformer textToIrTransformer;
    private GnmiSouthboundProvider gnmiProvider;

    public LightyGnmiSouthboundModule(final LightyServices services, final ExecutorService gnmiExecutorService,
                                final AAAEncryptionService encryptionService,
                                @Nullable final GnmiConfiguration configuration,
                                @Nullable final YangParserFactory parserFactory,
                                @Nullable  final YangTextToIRSourceTransformer textToIrTransformer) {

        this.lightyServices = Objects.requireNonNull(services);
        this.gnmiExecutorService = Objects.requireNonNull(gnmiExecutorService);
        this.encryptionService = encryptionService;
        this.gnmiConfiguration = configuration;
        this.parserFactory = parserFactory != null ? parserFactory : new DefaultYangParserFactory();
        this.textToIrTransformer = textToIrTransformer != null ? textToIrTransformer :
            new DefaultYangTextToIRSourceTransformer();
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Starting lighty gNMI Southbound Module");
        final List<YangLoaderService> initialLoaders = prepareByPathLoaders(gnmiConfiguration);
        try {
            gnmiProvider = new GnmiSouthboundProvider(lightyServices.getDOMMountPointService(),
                    lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(), gnmiExecutorService,
                    initialLoaders, encryptionService, parserFactory, textToIrTransformer);

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

    private List<YangLoaderService> prepareByPathLoaders(final GnmiConfiguration config) {
        final List<YangLoaderService> services = new ArrayList<>();
        if (config != null) {
            config.getInitialYangsPaths().stream()
                    .map(path -> new ByPathYangLoaderService(Path.of(path), parserFactory, textToIrTransformer))
                    .forEach(services::add);
            if (config.getYangModulesInfo() != null) {
                services.add(new ByClassPathYangLoaderService(config.getYangModulesInfo(), parserFactory,
                    textToIrTransformer));
            }
        }
        services.add(new ByClassPathYangLoaderService(OPENCONFIG_YANG_MODELS, parserFactory, textToIrTransformer));
        return services;
    }

}
