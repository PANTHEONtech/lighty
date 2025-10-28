/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.bgp.config;

import io.lighty.codecs.util.exception.DeserializationException;
import io.lighty.core.controller.impl.util.FileToDatastoreUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InitialBgpConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(InitialBgpConfigLoader.class);
    private static final String ROUTING_POLICY_PATH = "/initial/routing-policy-config.xml";
    private final DOMDataBroker dataBroker;
    private final EffectiveModelContext modelContext;

    public InitialBgpConfigLoader(final DOMDataBroker dataBroker, final EffectiveModelContext modelContext) {
        this.dataBroker = dataBroker;
        this.modelContext = modelContext;
    }

    public void init() {
        loadInitialRoutingPolicy();
    }

    private void loadInitialRoutingPolicy() {
        try (InputStream inputStream = InitialBgpConfigLoader.class.getResourceAsStream(ROUTING_POLICY_PATH)) {
            FileToDatastoreUtils.importConfigDataFile(inputStream, YangInstanceIdentifier.of(RoutingPolicy.QNAME),
                    FileToDatastoreUtils.ImportFileFormat.XML, modelContext, dataBroker, true);
        } catch (IOException | DeserializationException | ExecutionException | TimeoutException e) {
            LOG.warn("Failed to import initial BGP routing policies file {}", ROUTING_POLICY_PATH, e);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while importing initial BGP routing policies file {}", ROUTING_POLICY_PATH, e);
            Thread.currentThread().interrupt();
        }
    }

}
