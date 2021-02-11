package io.lighty.core.controller.impl.util;

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
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InitialDataImportUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InitialDataImportUtil.class);
    public static final long IMPORT_TIMEOUT_MILLIS = 20_000;

    private InitialDataImportUtil() {
        throw new UnsupportedOperationException("Init of utility class is forbidden");
    }

    private static void importConfigDatastoreFromJSON(InputStream inputStream,
                                                      SchemaContext schemaContext,
                                                      EffectiveModelContext effectiveModelContext,
                                                      DOMDataBroker dataBroker)
            throws IOException, InterruptedException, ExecutionException, TimeoutException, SerializationException {
        LOG.info("Loading data into config datastore from JSON");
        SchemaNode rootSchemaNode = DataSchemaContextTree.from(effectiveModelContext).getRoot().getDataSchemaNode();
        JsonNodeConverter jsonNodeConverter = new JsonNodeConverter(schemaContext);
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
            DOMDataTreeWriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
            wrTrx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), nodes);
            wrTrx.commit().get(IMPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            LOG.debug("Normalized nodes loaded on startup from json file: {}", nodes);
        }
    }


    public static void importInitialConfigDataFile(@NonNull InputStream inputFileStream,
                                                   @NonNull ImportFileFormat fileFormat,
                                                   @NonNull SchemaContext schemaContext,
                                                   @NonNull EffectiveModelContext effectiveModelContext,
                                                   @NonNull DOMDataBroker dataBroker)
            throws InterruptedException, ExecutionException, TimeoutException, IOException, SerializationException,
            IllegalStateException, UnsupportedOperationException {
        if (fileFormat == ImportFileFormat.JSON) {
            importConfigDatastoreFromJSON(inputFileStream, schemaContext, effectiveModelContext, dataBroker);
        } else if (fileFormat == ImportFileFormat.XML) {
            importConfigDatastoreFromXML(inputFileStream, effectiveModelContext, dataBroker);
        } else {
            throw new UnsupportedOperationException("Unsupported format of init config data file detected");
        }
    }

    private static void importConfigDatastoreFromXML(InputStream inputStream,
                                                     EffectiveModelContext effectiveModelContext,
                                                     DOMDataBroker dataBroker)
            throws IOException, InterruptedException, ExecutionException, TimeoutException, SerializationException {
        LOG.info("Loading data into config datastore from XML");
        SchemaNode rootSchemaNode = DataSchemaContextTree.from(effectiveModelContext).getRoot().getDataSchemaNode();
        XmlNodeConverter xmlNodeConverter = new XmlNodeConverter(effectiveModelContext);
        try (Reader reader =
                     new InputStreamReader(inputStream, Charset.defaultCharset())) {
            NormalizedNode<?, ?> nodes = xmlNodeConverter.deserialize(rootSchemaNode, reader);
            DOMDataTreeWriteTransaction wrTrx = dataBroker.newWriteOnlyTransaction();
            wrTrx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), nodes);
            wrTrx.commit().get(IMPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            LOG.debug("Normalized nodes loaded on startup from json file: {}", nodes);
        }
    }

}
