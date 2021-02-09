package io.lighty.core.controller.impl.util;

import com.google.gson.stream.JsonReader;
import io.lighty.codecs.api.SerializationException;
import io.lighty.core.controller.api.LightyServices;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InitialDataImportUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InitialDataImportUtil.class);
    public static final long IMPORT_TIMEOUT_MILLIS = 20_000;

    private InitialDataImportUtil() {

    }

    private static void importConfigDatastoreFromJSON(File initialConfigDataFile, LightyServices services)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        LOG.info("Loading data into config datastore from file {}", initialConfigDataFile.getAbsolutePath());
        SchemaNode rootSchemaNode = DataSchemaContextTree.from(services.getSchemaContextProvider()
                .getSchemaContext()).getRoot().getDataSchemaNode();

        try (InputStream dataStream = new FileInputStream(initialConfigDataFile)) {
            final NormalizedNodeContainerBuilder<?, ?, ?, ?> builder = ImmutableContainerNodeBuilder.create()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                            rootSchemaNode.getQName()));
            try (NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(builder)) {
                try (JsonParserStream jsonParser =
                             JsonParserStream.create(writer,
                                     JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                                             .getShared(services.getDOMSchemaService().getGlobalContext()))) {
                    try (JsonReader reader =
                                 new JsonReader(new InputStreamReader(dataStream, Charset.defaultCharset()))) {
                        jsonParser.parse(reader);
                        NormalizedNode<?, ?> nodes = builder.build();
                        DOMDataTreeWriteTransaction wrTrx = services.getClusteredDOMDataBroker()
                                .newWriteOnlyTransaction();
                        wrTrx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), nodes);
                        wrTrx.commit().get(IMPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                        LOG.debug("Normalized nodes loaded on startup from file {} :\n {}",
                                initialConfigDataFile.getAbsolutePath(), nodes);
                    }
                }
            }
        }
    }

    public static void importInitialConfigDataFile(@NonNull File file, @NonNull LightyServices services)
            throws InterruptedException, ExecutionException, TimeoutException, IOException, SerializationException {
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        if (extension.equalsIgnoreCase("json")) {
            importConfigDatastoreFromJSON(file, services);
        } else if (extension.equalsIgnoreCase("xml")) {
            importConfigDatastoreFromXML(file, services);
        } else {
            throw new UnsupportedOperationException("File extension"
                    + extension
                    + " is not supported as initial config data format");
        }
    }

    private static void importConfigDatastoreFromXML(File initialConfigDataFile, LightyServices services)
            throws IOException, InterruptedException, ExecutionException, TimeoutException, SerializationException {
        LOG.info("Loading data into config datastore from file {}", initialConfigDataFile.getAbsolutePath());
        SchemaNode rootSchemaNode = DataSchemaContextTree.from(services.getSchemaContextProvider()
                .getSchemaContext()).getRoot().getDataSchemaNode();

        try (Reader reader =
                     new InputStreamReader(new FileInputStream(initialConfigDataFile), Charset.defaultCharset())) {
            NormalizedNode<?, ?> nodes = services.getXmlNodeConverter().deserialize(rootSchemaNode, reader);
            DOMDataTreeWriteTransaction wrTrx = services.getClusteredDOMDataBroker().newWriteOnlyTransaction();
            wrTrx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), nodes);
            wrTrx.commit().get(IMPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            LOG.debug("Normalized nodes loaded on startup from file {} :\n {}",
                    initialConfigDataFile.getAbsolutePath(), nodes);
        }
    }

}
