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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByClassPathYangLoaderService implements YangLoaderService {

    private static final Logger LOG = LoggerFactory.getLogger(ByClassPathYangLoaderService.class);

    private final Set<YangModuleInfo> yangModulesInfo;
    private final YangParserFactory yangParser;

    public ByClassPathYangLoaderService(final Set<YangModuleInfo> yangModulesInfo, final YangParserFactory yangParser) {
        this.yangModulesInfo = Objects.requireNonNull(yangModulesInfo);
        this.yangParser = Objects.requireNonNull(yangParser);
    }

    @Override
    public List<GnmiDeviceCapability> load(final YangDataStoreService storeService) throws YangLoadException {
        final List<GnmiDeviceCapability> loadedModels = new ArrayList<>();
        final Set<YangModuleInfo> expandedModules = getAllModules(this.yangModulesInfo);
        for (YangModuleInfo yangModuleInfo : expandedModules) {
            final DelegatedYangTextSource yangTextSchemaSource = new DelegatedYangTextSource(
                SourceIdentifier.ofYangFileName(
                    yangModuleInfo.getName().getLocalName() + ".yang"),
                yangModuleInfo.getYangTextCharSource());
            try (InputStream yangTextStream = yangModuleInfo.openYangTextStream()) {
                this.yangParser.createParser().addSource(yangTextSchemaSource);

                final YangLoadModelUtil yangLoadModelUtil = new YangLoadModelUtil(yangTextSchemaSource, yangTextStream);
                storeService.addYangModel(yangLoadModelUtil.getModelName(), yangLoadModelUtil.getVersionToStore(),
                        yangLoadModelUtil.getModelBody())
                    .get(TimeoutUtils.DATASTORE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                loadedModels.add(new GnmiDeviceCapability(yangLoadModelUtil.getModelName(),
                    yangLoadModelUtil.getModelSemVer(), yangLoadModelUtil.getModelRevision()));
                LOG.info("Loaded yang model {} with version {}", yangLoadModelUtil.getModelName(),
                    yangLoadModelUtil.getVersionToStore());
            } catch (YangSyntaxErrorException | ExecutionException | TimeoutException | IOException e) {
                throw new YangLoadException(
                    String.format("Loading YangModuleInfo [%s] failed!", yangModuleInfo.getName()), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new YangLoadException(String.format("Interrupted while loading YangModuleInfo [%s] failed!",
                    yangModuleInfo.getName()), e);
            }
        }
        return loadedModels;
    }

    private Set<YangModuleInfo> getAllModules(final Set<YangModuleInfo> explicitModules) {
        final Set<YangModuleInfo> allModules = new HashSet<>();
        for (YangModuleInfo module : explicitModules) {
            collectRecursively(module, allModules);
        }
        return allModules;
    }

    private void collectRecursively(final YangModuleInfo module, final Set<YangModuleInfo> collector) {
        // If we haven't added this module yet, add it and process its children
        if (collector.add(module)) {
            for (YangModuleInfo imported : module.getImportedModules()) {
                collectRecursively(imported, collector);
            }
        }
    }
}
