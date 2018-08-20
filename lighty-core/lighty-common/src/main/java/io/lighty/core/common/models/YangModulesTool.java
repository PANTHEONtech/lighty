/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.common.models;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModulesTool {

    private static final Logger LOG = LoggerFactory.getLogger(YangModulesTool.class);
    private static final int PREFIX = 2;

    public static void main(final String[] args) {
        final Set<YangModuleInfo> allModelsFromClasspath = YangModuleUtils.getAllModelsFromClasspath();
        printModelInfo(allModelsFromClasspath);
    }

    public static void printModelInfo(final Set<YangModuleInfo> allModelsFromClasspath) {
        final int prefixLength = 0;
        final Set<YangModuleInfo> topLevelModels = YangModuleUtils.filterTopLevelModels(allModelsFromClasspath);
        LOG.info("# top-level models tree: {}", topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo : topLevelModels) {
            final QName qname = yangModuleInfo.getName();
            LOG.info("{}", qname.getNamespace(), qname.getLocalName(), qname.getRevision());
            printDependencies(yangModuleInfo.getImportedModules(), prefixLength + PREFIX);
        }
        LOG.info("# top-level models list: {}", topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo : topLevelModels) {
            final QName qname = yangModuleInfo.getName();
            LOG.info("{} {} {}", qname.getNamespace(), qname.getLocalName(), qname.getRevision());
        }
        final Set<YangModuleInfo> uniqueModels = YangModuleUtils.filterUniqueModels(allModelsFromClasspath);
        LOG.info("# unique models list   : {}", uniqueModels.size());
        for (final YangModuleInfo yangModuleInfo : uniqueModels) {
            final QName qname = yangModuleInfo.getName();
            LOG.info("{} {} {}", qname.getNamespace(), qname.getLocalName(), qname.getRevision());
        }
    }

    public static void printConfiguration(final Set<YangModuleInfo> allModelsFromClasspath) {
        final Set<YangModuleInfo> topLevelModels = YangModuleUtils.filterTopLevelModels(allModelsFromClasspath);
        LOG.info("# top-level models list: {}", topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo : topLevelModels) {
            final QName qname = yangModuleInfo.getName();
            System.out.println("{ \"nameSpace\": \"" + qname.getNamespace() + "\", \"name\": \""
                    + qname.getLocalName() + "\", \"revision\": \"" + qname.getRevision().orElse(null) + "\" },");
        }
        LOG.info("# top-level models list: {}", topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo: topLevelModels) {
            System.out.println(yangModuleInfo.getClass().getCanonicalName() + ".getInstance(),");
        }
    }

    private static void printDependencies(final Collection<YangModuleInfo> yangModuleInfos, final int prefixLength) {
        for (final YangModuleInfo yangModuleInfo : yangModuleInfos) {
            final QName qname = yangModuleInfo.getName();
            LOG.info("{}{} {} {}", Strings.repeat(" ", prefixLength), qname.getNamespace(), qname.getLocalName(), qname.getRevision());
            printDependencies(yangModuleInfo.getImportedModules(), prefixLength + PREFIX);
        }
    }

}
