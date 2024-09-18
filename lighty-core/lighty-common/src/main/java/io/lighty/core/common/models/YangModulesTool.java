/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common.models;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YangModulesTool {

    private static final Logger LOG = LoggerFactory.getLogger(YangModulesTool.class);
    private static final int PREFIX = 2;
    private static final String TOP_LEVEL_MODELS_LIST = "# top-level models list: {}";
    private static final String TOP_LEVEL_MODELS_TREE = "# top-level models tree: {}";
    private static final String UNIQUE_MODELS_LIST = "# unique models list   : {}";

    private YangModulesTool() {

    }

    public static void main(final String[] args) {
        final Set<YangModuleInfo> allModelsFromClasspath = YangModuleUtils.getAllModelsFromClasspath();
        printModelInfo(allModelsFromClasspath);
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT", justification = "Utility")
    public static void printModelInfo(final Set<YangModuleInfo> allModelsFromClasspath) {
        final int prefixLength = 0;
        final Set<YangModuleInfo> topLevelModels = YangModuleUtils.filterTopLevelModels(allModelsFromClasspath);
        LOG.info(TOP_LEVEL_MODELS_TREE, topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo : topLevelModels) {
            logQNameInfo(yangModuleInfo.getName());
            printDependencies(yangModuleInfo.getImportedModules(), prefixLength + PREFIX);
        }
        LOG.info(TOP_LEVEL_MODELS_LIST, topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo : topLevelModels) {
            logQNameInfo(yangModuleInfo.getName());
        }
        final Set<YangModuleInfo> uniqueModels = YangModuleUtils.filterUniqueModels(allModelsFromClasspath);
        LOG.info(UNIQUE_MODELS_LIST, uniqueModels.size());
        for (final YangModuleInfo yangModuleInfo : uniqueModels) {
            logQNameInfo(yangModuleInfo.getName());
        }
    }

    private static void logQNameInfo(QName qname) {
        LOG.info("namespace: {}, localName: {}, revision {}", qname.getNamespace(), qname.getLocalName(),
                qname.getRevision());
    }

    @SuppressWarnings("checkstyle:regexpSinglelineJava")
    public static void printConfiguration(final Set<YangModuleInfo> allModelsFromClasspath) {
        final Set<YangModuleInfo> topLevelModels = YangModuleUtils.filterTopLevelModels(allModelsFromClasspath);
        LOG.info(TOP_LEVEL_MODELS_LIST, topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo : topLevelModels) {
            final QName qname = yangModuleInfo.getName();
            System.out.println("{ \"nameSpace\": \"" + qname.getNamespace() + "\", \"name\": \""
                    + qname.getLocalName() + "\", \"revision\": \"" + qname.getRevision().orElse(null) + "\" },");
        }
        LOG.info(TOP_LEVEL_MODELS_LIST, topLevelModels.size());
        for (final YangModuleInfo yangModuleInfo: topLevelModels) {
            System.out.println(yangModuleInfo.getClass().getCanonicalName() + ".getInstance(),");
        }
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT", justification = "Utility")
    private static void printDependencies(final Collection<YangModuleInfo> yangModuleInfos, final int prefixLength) {
        for (final YangModuleInfo yangModuleInfo : yangModuleInfos) {
            final QName qname = yangModuleInfo.getName();
            LOG.info("{}{} {} {}", Strings.repeat(" ", prefixLength), qname.getNamespace(), qname.getLocalName(),
                qname.getRevision());
            printDependencies(yangModuleInfo.getImportedModules(), prefixLength + PREFIX);
        }
    }

}
