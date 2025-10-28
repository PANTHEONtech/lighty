/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.loader.impl;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoaderService;
import io.lighty.gnmi.southbound.schema.loader.util.YangLoadModelUtil;
import io.lighty.gnmi.southbound.schema.yangstore.service.YangDataStoreService;
import io.lighty.gnmi.southbound.timeout.TimeoutUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.spi.source.FileYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
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

    public ByPathYangLoaderService(final Path yangsPath) {
        this.yangsPath = Objects.requireNonNull(yangsPath);
        final YangXPathParserFactory xpathFactory = new AntlrXPathParserFactory();
        this.yangParser = new DefaultYangParserFactory(xpathFactory).createParser();
    }

    @Override
    public List<GnmiDeviceCapability> load(final YangDataStoreService storeService) throws YangLoadException {
        try (Stream<Path> pathStream = Files.walk(yangsPath)) {
            final List<GnmiDeviceCapability> loadedModels = new ArrayList<>();
            final List<Path> paths = pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(YangConstants.RFC6020_YANG_FILE_EXTENSION))
                    .map(File::toPath)
                    .collect(Collectors.toList());

            for (Path path : paths) {
                try (InputStream bodyInputStream = Files.newInputStream(path)) {
                    final YangTextSource yangTextSchemaSource = new FileYangTextSource(path);
                    // This validates the yang
                    this.yangParser.addSource(yangTextSchemaSource);
                    final YangLoadModelUtil yangLoadModelUtil = new YangLoadModelUtil(yangTextSchemaSource,
                            bodyInputStream);
                    storeService.addYangModel(yangLoadModelUtil.getModelName(), yangLoadModelUtil.getVersionToStore(),
                                    yangLoadModelUtil.getModelBody())
                            .get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                    loadedModels.add(new GnmiDeviceCapability(yangLoadModelUtil.getModelName(),
                            yangLoadModelUtil.getModelSemVer(), yangLoadModelUtil.getModelRevision()));
                    LOG.info("Loaded yang model {} with version {}", yangLoadModelUtil.getModelName(),
                            yangLoadModelUtil.getVersionToStore());
                }
            }
            return loadedModels;
        } catch (IOException | YangParserException | ExecutionException | TimeoutException e) {
            throw new YangLoadException("Loading yang files failed!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new YangLoadException("Interrupted while loading yang files!", e);
        }
    }

}
