/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.config;

import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;

public class Configuration {

    private String output;
    private String format;
    private String simplify;
    private String checkUpdateFrom;
    private boolean debug;
    private boolean quiet;
    private boolean recursive;
    private List<String> moduleNames;
    private List<String> path;
    private List<String> yang;
    private Set<QName> supportedFeatures;
    private List<String> parseAll;
    private DependConfiguration dependConfiguration;
    private CheckUpdateFromConfiguration checkUpdateFromConfiguration;
    private TreeConfiguration treeConfiguration;

    Configuration() {
        //noop
    }

    void setUpdateFrom(final String newCheckUpdateFrom) {
        this.checkUpdateFrom = newCheckUpdateFrom;
    }

    void setCheckUpdateFromConfiguration(final CheckUpdateFromConfiguration checkUpdateFromConfiguration) {
        this.checkUpdateFromConfiguration = checkUpdateFromConfiguration;
    }

    void setDependConfiguration(final DependConfiguration dependConfiguration) {
        this.dependConfiguration = dependConfiguration;
    }

    void setSupportedFeatures(final Set<QName> supportedFeatures) {
        this.supportedFeatures = supportedFeatures;
    }

    void setSimplify(final String simplify) {
        this.simplify = simplify;
    }

    <E> void setModuleNames(final List<E> moduleNames) {
        this.moduleNames = (List<String>) moduleNames;
    }

    void setQuiet(final Boolean quiet) {
        this.quiet = quiet;
    }

    <E> void setParseAll(final List<E> parseAll) {
        this.parseAll = (List<String>) parseAll;
    }

    void setFormat(final String format) {
        this.format = format;
    }

    void setDebug(final Boolean debug) {
        this.debug = debug;
    }

    void setOutput(final String output) {
        this.output = output;
    }

    <E> void setYangModules(final List<E> newYang) {
        this.yang = (List<String>) newYang;
    }

    void setRecursive(final Boolean recursive) {
        this.recursive = recursive;
    }

    <E> void setPath(final List<E> path) {
        this.path = (List<String>) path;
    }

    void setTreeConfiguration(final TreeConfiguration treeConfiguration) {
        this.treeConfiguration = treeConfiguration;
    }

    public Set<QName> getSupportedFeatures() {
        return supportedFeatures;
    }

    public List<String> getModuleNames() {
        return moduleNames;
    }

    public String getOutput() {
        return output;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public List<String> getPath() {
        return path;
    }

    public List<String> getYang() {
        return yang;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String getFormat() {
        return format;
    }

    public String getSimplify() {
        return simplify;
    }

    public List<String> getParseAll() {
        return parseAll;
    }

    public DependConfiguration getDependConfiguration() {
        return dependConfiguration;
    }

    public TreeConfiguration getTreeConfiguration() {
        return treeConfiguration;
    }

    public String getCheckUpdateFrom() {
        return checkUpdateFrom;
    }

    public CheckUpdateFromConfiguration getCheckUpdateFromConfiguration() {
        return checkUpdateFromConfiguration;
    }
}
