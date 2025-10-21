/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.gnmi.simulatordevice.yang;

import com.google.common.io.CharStreams;
import io.lighty.modules.gnmi.commons.util.DataConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class YangDataService {
    private static final Logger LOG = LoggerFactory.getLogger(YangDataService.class);

    private final EnumMap<DatastoreType, InMemoryDOMDataStore> datastoreMap;

    public YangDataService(final EffectiveModelContext schemaContext, final String initialConfigDataPath,
                           final String initialStateDataPath) throws IOException {
        this.datastoreMap = createDatastoreMap(schemaContext);
        initializeDataStore(initialConfigDataPath, initialStateDataPath, schemaContext);
    }

    public Optional<NormalizedNode> readDataByPath(final DatastoreType datastoreType,
                                                         final YangInstanceIdentifier path) {
        try (DOMStoreReadTransaction tx = datastoreMap.get(datastoreType).newReadOnlyTransaction()) {
            return tx.read(path).get();
        } catch (final ExecutionException e) {
            LOG.error("Unable to fetch data from DataStore", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while fetching data from DataStore", e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    public void mergeDataByPath(final DatastoreType datastoreType, final YangInstanceIdentifier path,
                                final NormalizedNode node) {
        modifyDataByPath(datastoreType, path, node, ModificationType.MERGE);
    }

    public void writeDataByPath(final DatastoreType datastoreType, final YangInstanceIdentifier path,
                                final NormalizedNode node) {
        modifyDataByPath(datastoreType, path, node, ModificationType.WRITE);
    }

    public void deleteDataByPath(final DatastoreType datastoreType, final YangInstanceIdentifier path) {
        modifyDataByPath(datastoreType, path, null, ModificationType.DELETE);
    }

    private void modifyDataByPath(final DatastoreType datastoreType, final YangInstanceIdentifier path,
                                  final NormalizedNode node, final ModificationType modificationType) {
        try (DOMStoreReadWriteTransaction tx = datastoreMap.get(datastoreType).newReadWriteTransaction()) {
            if (modificationType == ModificationType.WRITE) {
                tx.write(path, node);
            } else if (modificationType == ModificationType.DELETE) {
                tx.delete(path);
            } else {
                tx.merge(path, node);
            }
            final DOMStoreThreePhaseCommitCohort tpcc = tx.ready();
            tpcc.canCommit().get();
            tpcc.preCommit().get();
            tpcc.commit().get();
        } catch (final ExecutionException exception) {
            LOG.error("Unable to commit changes to datastore", exception);
            throw new RuntimeException("Unable to commit changes to datastore", exception);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while committing changes to datastore", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while committing changes to datastore", e);
        }
    }

    private void initializeDataStore(final String initialConfigDataPath, final String initialStateDataPath,
                                     final EffectiveModelContext schemaContext)
            throws IOException {
        // Init config data
        if (StringUtils.isNotEmpty(initialConfigDataPath)) {
            final InputStream configFile = Files.newInputStream(Path.of(initialConfigDataPath));
            initDataTree(configFile, DatastoreType.CONFIGURATION, schemaContext);
        }
        if (StringUtils.isNotEmpty(initialStateDataPath)) {
            final InputStream configFile = Files.newInputStream(Path.of(initialStateDataPath));
            initDataTree(configFile, DatastoreType.STATE, schemaContext);
        }
    }

    private void initDataTree(final InputStream stream, final DatastoreType datastoreType,
                              final EffectiveModelContext schemaContext) {
        try {
            // read json configuration from file
            final String configJson = CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
            /*
             ROOT YII because we are writing one/or multiple top-level elements
              (interfaces,alarms, components ...).
            */
            final NormalizedNode node =
                    DataConverter.nodeFromJsonString(YangInstanceIdentifier.empty(), configJson, schemaContext);
            /*
             If QName of parsed node is a root node (SchemaContext.NAME), that means we parsed multiple
              top-level element, in that case we need to write this node on ROOT YII.
            */
            if (node.name().getNodeType().equals(SchemaContext.NAME)) {
                writeDataByPath(datastoreType, YangInstanceIdentifier.empty(), node);
            // Else we parsed only one top-level element, in that case we write this node on it's identifier.
            } else {
                writeDataByPath(datastoreType, YangInstanceIdentifier.of(node.name().getNodeType()), node);
            }

        } catch (final IOException e) {
            LOG.error("Unable to get data from stream {}", stream, e);
        }
    }

    public void registerListener(final DatastoreType datastoreType, final YangInstanceIdentifier identifier,
                                 final DOMDataTreeChangeListener listener) {
        datastoreMap.get(datastoreType).registerTreeChangeListener(identifier, listener);
    }

    private EnumMap<DatastoreType, InMemoryDOMDataStore> createDatastoreMap(EffectiveModelContext schemaContext) {
        final InMemoryDOMDataStore configStore = new InMemoryDOMDataStore(DatastoreType.CONFIGURATION.getName(),
                LogicalDatastoreType.CONFIGURATION, createExecutorService(DatastoreType.CONFIGURATION.getName()),
                20, false);
        configStore.onModelContextUpdated(schemaContext);

        final InMemoryDOMDataStore operStore = new InMemoryDOMDataStore(DatastoreType.OPERATIONAL.getName(),
                LogicalDatastoreType.OPERATIONAL, createExecutorService(DatastoreType.OPERATIONAL.getName()),
                20, false);
        operStore.onModelContextUpdated(schemaContext);

        final InMemoryDOMDataStore stateStore = new InMemoryDOMDataStore(DatastoreType.STATE.getName(),
                LogicalDatastoreType.OPERATIONAL, createExecutorService(DatastoreType.STATE.getName()),
                20, false);
        stateStore.onModelContextUpdated(schemaContext);

        final EnumMap<DatastoreType, InMemoryDOMDataStore> dataStoreTypeMap = new EnumMap<>(DatastoreType.class);
        dataStoreTypeMap.put(DatastoreType.CONFIGURATION, configStore);
        dataStoreTypeMap.put(DatastoreType.OPERATIONAL, operStore);
        dataStoreTypeMap.put(DatastoreType.STATE, stateStore);
        return dataStoreTypeMap;
    }

    private ExecutorService createExecutorService(final String name) {
        return SpecialExecutors.newBlockingBoundedFastThreadPool(20, 20, name + "-DCL", InMemoryDOMDataStore.class);
    }

    private enum ModificationType {
        WRITE,
        MERGE,
        DELETE
    }
}
