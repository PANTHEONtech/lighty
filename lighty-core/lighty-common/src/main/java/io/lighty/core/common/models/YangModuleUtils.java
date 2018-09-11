/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.common.models;

import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public final class YangModuleUtils {

    private static final Logger LOG = LoggerFactory.getLogger(YangModuleUtils.class);

    private YangModuleUtils() {
        throw new UnsupportedOperationException("do not instantiate utility class");
    }

    /**
     * Get all Yang modules from classpath using ServiceLoader scanning.
     * @return
     *   Complete list of models found on classpath.
     */
    public static Set<YangModuleInfo> getAllModelsFromClasspath() {
        Set<YangModuleInfo> moduleInfos = new HashSet<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
            LOG.info("Adding [{}] module into known modules", yangModelBindingProvider.getModuleInfo());
        }
        return Collections.unmodifiableSet(moduleInfos);
    }

    /**
     * Filter top-level models from given entry set.
     * Top level model is considered the model which is never used as dependency for other model.
     * @param models
     *    Unfiltered entry set of models.
     * @return
     *    Filtered set of top-level models only.
     */
    public static Set<YangModuleInfo> filterTopLevelModels(Set<YangModuleInfo> models) {
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
     * @param models
     *   Unfiltered entry set of models.
     * @return
     *   Filtered set of unique models only.
     */
    public static Set<YangModuleInfo> filterUniqueModels(Set<YangModuleInfo> models) {
        Map<ModuleId, YangModuleInfo> result = new HashMap<>();
        for (YangModuleInfo yangModuleInfo: models) {
            result.put(ModuleId.from(yangModuleInfo), yangModuleInfo);
            for (YangModuleInfo yangModuleInfoDep: filterUniqueModels(yangModuleInfo.getImportedModules())) {
                result.put(ModuleId.from(yangModuleInfoDep), yangModuleInfoDep);
            }
        }
        return new HashSet<>(result.values());
    }

    /**
     * Get all Yang modules from classpath filtered by collection of top-level modules.
     * @param filter
     *   The collection of top-level modules represented by name and revision.
     * @return
     *   Collection top-level modules and all of imported yang module dependencies recursively.
     *   Empty collection is returned if no suitable modules are found.
     */
    public static Set<YangModuleInfo> getModelsFromClasspath(Set<ModuleId> filter) {
        Map<ModuleId, YangModuleInfo> resolvedModules = new HashMap<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (ModuleId moduleId: filter) {
            Set<YangModuleInfo> filteredSet = filterYangModelBindingProviders(moduleId, yangProviderLoader);
            for (YangModuleInfo yangModuleInfo : filteredSet) {
                resolvedModules.put(ModuleId.from(yangModuleInfo), yangModuleInfo);
                LOG.info("Adding [{}] module into known modules", yangModuleInfo);
                addDependencies(resolvedModules, yangModuleInfo.getImportedModules());
            }
        }
        return Collections.unmodifiableSet(resolvedModules.values().stream().collect(Collectors.toSet()));
    }


    /**
     * Recursively get list of URLs representing yang models from set of {@link YangModuleInfo}.
     * @param models
     *   Set of {@link YangModuleInfo}
     * @return
     *   Flat list of yang model URLs obtained recursively from {@link YangModuleInfo} set.
     * @throws YangClasspathResolutionException
     *   In case resource path to yang model file cannot be obtained using java reflection.
     */
    public static List<URL> searchYangsInYangModuleInfo(final Set<YangModuleInfo> models) throws YangClasspathResolutionException {
        Map<ModuleId, URL> modelUrls = getYangsFromYangModuleInfo(models);
        return Collections.unmodifiableList(new ArrayList<>(modelUrls.values()));
    }

    private static Map<ModuleId, URL> getYangsFromYangModuleInfo(final Set<YangModuleInfo> models) throws YangClasspathResolutionException {
        try {
            Map<ModuleId, URL> modelUrls = new HashMap<>();
            for (YangModuleInfo yangModuleInfo : models) {
                Field resourcePathField = yangModuleInfo.getClass().getDeclaredField("resourcePath");
                resourcePathField.setAccessible(true);
                String resourcePath = resourcePathField.get(yangModuleInfo).toString();
                resourcePath = resourcePath.substring(1, resourcePath.length());
                URL url = YangModuleUtils.class.getClassLoader().getResource(resourcePath);
                modelUrls.put(ModuleId.from(yangModuleInfo), url);
                Map<ModuleId, URL> importedModules = getYangsFromYangModuleInfo(yangModuleInfo.getImportedModules());
                modelUrls.putAll(importedModules);
            }
            return modelUrls;
        } catch (Exception e) {
            throw new YangClasspathResolutionException(e);
        }
    }

    private static void addDependencies(Map<ModuleId, YangModuleInfo> resolvedModules, Set<YangModuleInfo> importedModules) {
        for (YangModuleInfo yangModuleInfo: importedModules) {
            resolvedModules.put(ModuleId.from(yangModuleInfo), yangModuleInfo);
            LOG.info("Adding [{}] module into known modules", yangModuleInfo);
            addDependencies(resolvedModules, yangModuleInfo.getImportedModules());
        }
    }

    private static Set<YangModuleInfo> filterYangModelBindingProviders(ModuleId moduleId,
                                                                       ServiceLoader<YangModelBindingProvider> yangProviderLoader) {
        Set<YangModuleInfo> filteredSet = new HashSet<>();
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            if (moduleId.getName().equals(yangModelBindingProvider.getModuleInfo().getName())
                    && moduleId.getRevision().equals(yangModelBindingProvider.getModuleInfo().getRevision())) {
                filteredSet.add(yangModelBindingProvider.getModuleInfo());
            }
        }
        return filteredSet;
    }

    private static boolean isDependentModel(Set<YangModuleInfo> models, YangModuleInfo yangModuleInfo) {
        for (YangModuleInfo moduleInfo: models) {
            if (hasDependency(moduleInfo, yangModuleInfo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDependency(YangModuleInfo superiorModel, YangModuleInfo dependency) {
        for (YangModuleInfo moduleInfo:  superiorModel.getImportedModules()) {
            if (moduleInfo.getNamespace().equals(dependency.getNamespace())
                    && moduleInfo.getName().equals(dependency.getName())
                    && moduleInfo.getRevision().equals(dependency.getRevision()) ) {
                return true;
            } else {
                hasDependency(moduleInfo, dependency);
            }
        }
        return false;
    }

}
