/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.bgp.config;

import io.lighty.modules.bgp.deployer.BgpModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public final class InitialBgpConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(InitialBgpConfigLoader.class);
    private static final long COMMIT_WAIT_TIME = 10_000;
    private static final String ROUTING_POLICY_PATH = "/initial/routing-policy-config.xml";
    private static final SchemaNodeIdentifier.Absolute ROUTING_POLICY = SchemaNodeIdentifier.Absolute.of(
            RoutingPolicy.QNAME);
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
        final SchemaInferenceStack inferenceStack = SchemaInferenceStack.of(modelContext, ROUTING_POLICY);
        try (InputStream inputStream = BgpModule.class
                .getResourceAsStream(ROUTING_POLICY_PATH)) {
            final NormalizedNode dto = toNormalizedNode(inferenceStack, inputStream);
            final DOMDataTreeWriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.CONFIGURATION,
                    YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(RoutingPolicy.QNAME)),
                    dto);
            wTx.commit().get(COMMIT_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (SAXException | URISyntaxException | IOException | XMLStreamException e) {
            LOG.warn("Failed to load routing policy file at {}", ROUTING_POLICY_PATH, e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while committing transaction to load initial Routing policy", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Error while committing transaction to load initial Routing policy", e);
        }
    }

    private static NormalizedNode toNormalizedNode(final SchemaInferenceStack schemaStack,
            final InputStream inputStream) throws XMLStreamException, SAXException, IOException, URISyntaxException {

        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        final XMLStreamReader reader = UntrustedXML.createXMLStreamReader(inputStream);
        try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, schemaStack.toInference())) {
            xmlParser.parse(reader);
        } finally {
            reader.close();
        }
        return result.getResult();
    }
}
