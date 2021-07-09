/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.util;

import com.google.common.base.Preconditions;
import io.lighty.codecs.JsonNodeConverter;
import io.lighty.codecs.XmlNodeConverter;
import io.lighty.codecs.api.SerializationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration.InitialConfigData.ImportFileFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InitialDataImportUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InitialDataImportUtil.class);
    public static final long IMPORT_TIMEOUT_MILLIS = 20_000;

    private InitialDataImportUtil() {
        throw new UnsupportedOperationException("Init of utility class is forbidden");
    }

    private static NormalizedNode<?, ?> inputStreamJSONtoNormalizedNodes(InputStream inputStream,
                                                                         EffectiveModelContext effectiveModelContext)
            throws IOException, SerializationException {
        SchemaNode rootSchemaNode = DataSchemaContextTree.from(effectiveModelContext).getRoot().getDataSchemaNode();
        JsonNodeConverter jsonNodeConverter = new JsonNodeConverter(effectiveModelContext);
        try (Reader reader =
                     new InputStreamReader(inputStream, Charset.defaultCharset())) {
            NormalizedNode<?, ?> nodes = jsonNodeConverter.deserialize(rootSchemaNode, reader);
            // For some reason JsonParserStream.parse() doesn't wrap deserialized NormalizedNode in root schema node
            // (urn:ietf:params:xml:ns:netconf:base:1.0)data as XMLParserStream does..
            // Wrap it here:
            nodes = ImmutableContainerNodeBuilder.create()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                            rootSchemaNode.getQName()))
                    .addChild((DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>) nodes)
                    .build();
            return nodes;
        }
    }

    private static NormalizedNode<?, ?> inputStreamXMLtoNormalizedNodes(InputStream inputStream,
                                                                        EffectiveModelContext effectiveModelContext)
            throws IOException, SerializationException {
        SchemaNode rootSchemaNode = DataSchemaContextTree.from(effectiveModelContext).getRoot().getDataSchemaNode();
        XmlNodeConverter xmlNodeConverter = new XmlNodeConverter(effectiveModelContext);
        try (Reader reader =
                     new InputStreamReader(inputStream, Charset.defaultCharset())) {
            return xmlNodeConverter.deserialize(rootSchemaNode, reader);
        }
    }

    public static void importInitialConfigDataFile(@NonNull InputStream inputFileStream,
                                                   @NonNull ImportFileFormat fileFormat,
                                                   @NonNull EffectiveModelContext effectiveModelContext,
                                                   @NonNull DOMDataBroker dataBroker)
            throws InterruptedException, ExecutionException, TimeoutException, IOException, SerializationException,
            IllegalStateException, UnsupportedOperationException {
        NormalizedNode<?, ?> nodes;
        if (fileFormat == ImportFileFormat.JSON) {
            LOG.info("Converting JSON initial config data file to nodes");
            nodes = inputStreamJSONtoNormalizedNodes(inputFileStream, effectiveModelContext);
        } else if (fileFormat == ImportFileFormat.XML) {
            LOG.info("Converting XML initial config data file to nodes");
            nodes = inputStreamXMLtoNormalizedNodes(inputFileStream, effectiveModelContext);
        } else {
            throw new UnsupportedOperationException("Unsupported format of init config data file detected");
        }
        Preconditions.checkNotNull(nodes, "Parsed nodes are null");
        LOG.info("Merging nodes parsed from config data init file");
        mergeConfigNormalizedNodes(nodes, dataBroker);
        LOG.info("Load of initial config data was successful");
        LOG.debug("Normalized nodes loaded on startup from file: {}", nodes);
    }

    private static void mergeConfigNormalizedNodes(NormalizedNode<?, ?> nodes, DOMDataBroker dataBroker)
            throws InterruptedException, ExecutionException, TimeoutException {
        DOMDataTreeWriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
        wrTrx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), nodes);
        wrTrx.commit().get(IMPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

}
