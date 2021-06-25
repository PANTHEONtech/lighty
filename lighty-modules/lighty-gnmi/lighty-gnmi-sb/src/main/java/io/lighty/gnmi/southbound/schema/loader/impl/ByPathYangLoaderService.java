/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.loader.impl;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.SchemaConstants;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoaderService;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.SemVer;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.model.parser.api.YangParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.impl.YangParserFactoryImpl;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangModelDependencyInfo;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads yangs used for constructing schema for gNMI devices from provided path.
 */
public class ByPathYangLoaderService implements YangLoaderService {

    private static final Logger LOG = LoggerFactory.getLogger(ByPathYangLoaderService.class);
    private final Path yangsPath;
    /**
     * Parser used for validation of yang files.
     */
    private final YangParser yangParser;

    public ByPathYangLoaderService(final Path yangsPath, @Nullable final CrossSourceStatementReactor customReactor) {
        this.yangsPath = Objects.requireNonNull(yangsPath);
        this.yangParser = new YangParserFactoryImpl(Objects.requireNonNullElse(customReactor,
                SchemaConstants.DEFAULT_YANG_REACTOR)).createParser();
    }

    @Override
    public List<GnmiDeviceCapability> load(final YangDataStoreService storeService) throws YangLoadException {
        try {
            final List<GnmiDeviceCapability> loadedModels = new ArrayList<>();
            final List<File> filesInFolder = Files.walk(yangsPath)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(YangConstants.RFC6020_YANG_FILE_EXTENSION))
                    .collect(Collectors.toList());

            for (File file : filesInFolder) {
                final String modelBody = IOUtils.toString(
                        Files.newInputStream(file.toPath()), Charset.defaultCharset());
                final YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource
                        .forFile(file);
                final YangModelDependencyInfo yangModelDependencyInfo =
                        YangModelDependencyInfo.forYangText(yangTextSchemaSource);
                final String modelName = yangModelDependencyInfo.getName();
                // If revision is present in fileName, prefer that
                final Optional<Revision> modelRevision = yangTextSchemaSource.getIdentifier().getRevision()
                        .or(yangModelDependencyInfo::getRevision);
                final Optional<SemVer> modelSemVer = yangModelDependencyInfo.getSemanticVersion();
                final String versionToStore;
                if (modelSemVer.isPresent()) {
                    versionToStore = modelSemVer.get().toString();
                } else if (modelRevision.isPresent()) {
                    versionToStore = modelRevision.get().toString();
                } else {
                    versionToStore = "";
                }
                // This validates the yang
                this.yangParser.addSource(yangTextSchemaSource);
                storeService.addYangModel(modelName, versionToStore, modelBody)
                        .get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                loadedModels.add(new GnmiDeviceCapability(modelName, modelSemVer.orElse(null),
                        modelRevision.orElse(null)));
                LOG.info("Loaded yang model {} with version {}", modelName, versionToStore);
            }
            return loadedModels;
        } catch (IOException | YangParserException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new YangLoadException("Loading yang files failed!", e);
        }
    }

}
