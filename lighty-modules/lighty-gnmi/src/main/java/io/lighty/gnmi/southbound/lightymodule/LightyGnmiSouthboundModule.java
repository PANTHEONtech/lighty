/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.gnmi.southbound.yangmodule.util.GnmiConfigUtils.OPENCONFIG_YANG_MODELS;

import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.gnmi.southbound.provider.GnmiSouthboundProvider;
import org.opendaylight.gnmi.southbound.schema.loader.api.YangLoadException;
import org.opendaylight.gnmi.southbound.schema.loader.api.YangLoaderService;
import org.opendaylight.gnmi.southbound.schema.loader.impl.ByClassPathYangLoaderService;
import org.opendaylight.gnmi.southbound.schema.loader.impl.ByPathYangLoaderService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightyGnmiSouthboundModule {
    private static final Logger LOG = LoggerFactory.getLogger(LightyGnmiSouthboundModule.class);

    private final DataBroker dataBroker;
    private final RpcProviderService rpcProviderService;
    private final DOMMountPointService domMountPointService;
    private final AAAEncryptionService encryptionService;
    private final YangParserFactory parserFactory;
    private final YangXPathParserFactory xpathParserFactory;
    private final GnmiConfiguration gnmiConfiguration;

    private ExecutorService gnmiExecutor;
    private GnmiSouthboundProvider gnmiProvider;

    public LightyGnmiSouthboundModule(DataBroker dataBroker, RpcProviderService rpcProviderService,
        DOMMountPointService domMountPointService, AAAEncryptionService encryptionService,
        YangParserFactory parserFactory, YangXPathParserFactory xpathParserFactory,
        GnmiConfiguration gnmiConfiguration) {
        this.dataBroker = requireNonNull(dataBroker);
        this.rpcProviderService = requireNonNull(rpcProviderService);
        this.domMountPointService = requireNonNull(domMountPointService);
        this.encryptionService = requireNonNull(encryptionService);
        this.parserFactory = requireNonNull(parserFactory);
        this.xpathParserFactory = requireNonNull(xpathParserFactory);
        this.gnmiConfiguration = gnmiConfiguration;
    }

    public void init() {
        LOG.info("Starting ODL gNMI Southbound Component");
        gnmiExecutor = Executors.newFixedThreadPool(4);
        CrossSourceStatementReactor reactor = RFC7950Reactors.defaultReactorBuilder(xpathParserFactory).build();

        try {
            gnmiProvider = new GnmiSouthboundProvider(
                domMountPointService,
                dataBroker,
                rpcProviderService,
                gnmiExecutor,
                prepareByPathLoaders(gnmiConfiguration),
                encryptionService,
                reactor);

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

    private List<YangLoaderService> prepareByPathLoaders(final GnmiConfiguration config) {
        final List<YangLoaderService> services = new ArrayList<>();
        if (config != null) {
            config.getInitialYangsPaths().stream()
                .map(path -> new ByPathYangLoaderService(Path.of(path), parserFactory))
                .forEach(services::add);
            if (config.getYangModulesInfo() != null) {
                services.add(new ByClassPathYangLoaderService(config.getYangModulesInfo(), parserFactory));
            }
        }
        services.add(new ByClassPathYangLoaderService(OPENCONFIG_YANG_MODELS, parserFactory));

        return services;
    }
}