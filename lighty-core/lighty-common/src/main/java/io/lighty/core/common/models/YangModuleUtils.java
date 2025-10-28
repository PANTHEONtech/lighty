/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YangModuleUtils {
    private static final Logger LOG = LoggerFactory.getLogger(YangModuleUtils.class);
    private static final String ADDING_MODULE_INTO_KNOWN_MODULES = "Adding [{}] module into known modules";

    private YangModuleUtils() {
        throw new UnsupportedOperationException("do not instantiate utility class");
    }

    /**
     * Get all Yang modules from classpath using ServiceLoader scanning.
     * @return Complete list of models found on classpath.
     */
    public static Set<YangModuleInfo> getAllModelsFromClasspath() {
        Set<YangModuleInfo> moduleInfos = new HashSet<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
            LOG.info(ADDING_MODULE_INTO_KNOWN_MODULES, yangModelBindingProvider.getModuleInfo());
        }
        return Collections.unmodifiableSet(moduleInfos);
    }

    /**
     * Filter top-level models from given entry set.
     * Top level model is considered the model which is never used as dependency for other model.
     * @param models Unfiltered entry set of models.
     * @return Filtered set of top-level models only.
     */
    public static Set<YangModuleInfo> filterTopLevelModels(final Set<YangModuleInfo> models) {
        Set<YangModuleInfo> result = new HashSet<>();
        for (YangModuleInfo yangModuleInfo: models) {
            if (!isDependentModel(models, yangModuleInfo)) {
                result.add(yangModuleInfo);
            }
        }
        return result;
    }

    /**
     * Filter unique models from given entry set.
     * This filter scans recursively dependencies and returns minimal set of models that are unique.
     * @param models Unfiltered entry set of models.
     * @return Filtered set of unique models only.
     */
    public static Set<YangModuleInfo> filterUniqueModels(final Collection<YangModuleInfo> models) {
        Map<ModuleId, YangModuleInfo> result = new HashMap<>();
        for (YangModuleInfo yangModuleInfo: models) {
            result.put(ModuleId.from(yangModuleInfo), yangModuleInfo);
            for (YangModuleInfo yangModuleInfoDep : filterUniqueModels(yangModuleInfo.getImportedModules())) {
                result.put(ModuleId.from(yangModuleInfoDep), yangModuleInfoDep);
            }
        }
        return new HashSet<>(result.values());
    }

    /**
     * Get all Yang modules from classpath filtered by collection of top-level modules.
     *
     * @param filter The collection of top-level modules represented by name and revision.
     * @return Collection top-level modules and all of imported yang module dependencies recursively.
     *         Empty collection is returned if no suitable modules are found.
     */
    public static Set<YangModuleInfo> getModelsFromClasspath(final Set<ModuleId> filter) {
        Map<ModuleId, YangModuleInfo> resolvedModules = new HashMap<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (ModuleId moduleId: filter) {
            Set<YangModuleInfo> filteredSet = filterYangModelBindingProviders(moduleId, yangProviderLoader);
            for (YangModuleInfo yangModuleInfo : filteredSet) {
                resolvedModules.put(ModuleId.from(yangModuleInfo), yangModuleInfo);
                LOG.info(ADDING_MODULE_INTO_KNOWN_MODULES, yangModuleInfo);
                addDependencies(resolvedModules, yangModuleInfo.getImportedModules());
            }
        }
        return Collections.unmodifiableSet(resolvedModules.values().stream().collect(Collectors.toSet()));
    }


    private static void addDependencies(final Map<ModuleId, YangModuleInfo> resolvedModules,
            final Collection<YangModuleInfo> importedModules) {
        for (YangModuleInfo yangModuleInfo : importedModules) {
            resolvedModules.put(ModuleId.from(yangModuleInfo), yangModuleInfo);
            LOG.info(ADDING_MODULE_INTO_KNOWN_MODULES, yangModuleInfo);
            addDependencies(resolvedModules, yangModuleInfo.getImportedModules());
        }
    }

    private static Set<YangModuleInfo> filterYangModelBindingProviders(final ModuleId moduleId,
            final ServiceLoader<YangModelBindingProvider> yangProviderLoader) {
        Set<YangModuleInfo> filteredSet = new HashSet<>();
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            if (moduleId.getQName().equals(yangModelBindingProvider.getModuleInfo().getName())) {
                filteredSet.add(yangModelBindingProvider.getModuleInfo());
            }
        }
        return filteredSet;
    }

    private static boolean isDependentModel(final Set<YangModuleInfo> models, final YangModuleInfo yangModuleInfo) {
        for (YangModuleInfo moduleInfo: models) {
            if (hasDependency(moduleInfo, yangModuleInfo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDependency(final YangModuleInfo superiorModel, final YangModuleInfo dependency) {
        for (YangModuleInfo moduleInfo:  superiorModel.getImportedModules()) {
            if (moduleInfo.getName().equals(dependency.getName())) {
                return true;
            }

            hasDependency(moduleInfo, dependency);
        }
        return false;
    }

    /**
     * Generate JSON configuration snippet containing list of models from set of {@link YangModuleInfo}.
     * @param models input set of models.
     * @return JSON configuration snippet as {@link ArrayNode}.
     */
    public static ArrayNode generateJSONModelSetConfiguration(final Set<YangModuleInfo> models) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (YangModuleInfo yangModuleInfo: models) {
            ObjectNode modelObject = mapper.createObjectNode();
            ModuleId moduleId = ModuleId.from(yangModuleInfo);
            modelObject.put("nameSpace", moduleId.getNameSpace().toString());
            modelObject.put("name", moduleId.getName());
            if (moduleId.getRevision() != null) {
                modelObject.put("revision", moduleId.getRevision().toString());
            }
            arrayNode.add(modelObject);
        }
        return arrayNode;
    }

}
