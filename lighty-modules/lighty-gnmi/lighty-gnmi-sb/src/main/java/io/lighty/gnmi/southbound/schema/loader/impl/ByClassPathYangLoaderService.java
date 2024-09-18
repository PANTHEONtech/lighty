/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByClassPathYangLoaderService implements YangLoaderService {

    private static final Logger LOG = LoggerFactory.getLogger(ByClassPathYangLoaderService.class);

    private final Set<YangModuleInfo> yangModulesInfo;
    /**
     * Parser used for validation of yang files.
     */
    private final YangParser yangParser;

    public ByClassPathYangLoaderService(final Set<YangModuleInfo> yangModulesInfo) {
        this.yangModulesInfo = Objects.requireNonNull(yangModulesInfo);
        final YangXPathParserFactory xpathFactory = new AntlrXPathParserFactory();
        this.yangParser = new DefaultYangParserFactory(xpathFactory).createParser();
    }

    @Override
    public List<GnmiDeviceCapability> load(final YangDataStoreService storeService) throws YangLoadException {
        final List<GnmiDeviceCapability> loadedModels = new ArrayList<>();
        for (YangModuleInfo yangModuleInfo : this.yangModulesInfo) {
            final DelegatedYangTextSource yangTextSchemaSource = new DelegatedYangTextSource(
                    SourceIdentifier.ofYangFileName(
                            yangModuleInfo.getName().getLocalName() + ".yang"),
                    yangModuleInfo.getYangTextCharSource());
            try (InputStream yangTextStream = yangModuleInfo.openYangTextStream()) {
                // This validates the yang
                this.yangParser.addSource(yangTextSchemaSource);
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
}
