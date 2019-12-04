package io.lighty.core.controller.impl.services.infrautils;
/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
//package org.opendaylight.infrautils.metrics.internal;

import com.google.common.base.MoreObjects;
import java.util.Map;
import java.util.function.Consumer;

import io.lighty.core.controller.impl.services.infrautils.LightyMetricProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration properties for the metrics implementation.
 *
 * <p>Karaf's OSGi ConfigAdmin service, via the cm blueprint extension, sets this
 * from the etc/org.opendaylight.infrautils.metrics.cfg configuration file.
 *
 */
public final class LightyConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(LightyConfiguration.class);

    private final LightyMetricProvider metricProvider;

    // Apply any change to these defaults also to org.opendaylight.infrautils.metrics.cfg
    // (Just for clarity; they are commented out there, so these are the real defaults.)
    private int threadsWatcherIntervalMS = 500;
    private int maxThreads = 1000;
    private int fileReporterIntervalSecs = 0;
    private int maxThreadsMaxLogIntervalSecs = 60;
    private int deadlockedThreadsMaxLogIntervalSecs = 60;

    public LightyConfiguration(LightyMetricProvider metricProvider, Map<String, String> initialProperties) {
        this(metricProvider);
        updateProperties(initialProperties);
    }

    public LightyConfiguration(LightyMetricProvider metricProvider) {
        this.metricProvider = metricProvider;
    }

    public void updateProperties(Map<String, String> properties) {
        LOG.info("updateProperties({})", properties);
        doIfIntPropertyIsPresent(properties, "threadsWatcherIntervalMS", this::setThreadsWatcherIntervalMS);
        doIfIntPropertyIsPresent(properties, "maxThreads", this::setMaxThreads);
        doIfIntPropertyIsPresent(properties, "fileReporterIntervalSecs", this::setFileReporterIntervalSecs);
        doIfIntPropertyIsPresent(properties, "maxThreadsMaxLogIntervalSecs", this::setMaxThreadsMaxLogIntervalSecs);
        doIfIntPropertyIsPresent(properties, "deadlockedThreadsMaxLogIntervalSecs",
                this::setDeadlockedThreadsMaxLogIntervalSecs);

        metricProvider.updateConfiguration(this);
    }

    public void setFileReporterIntervalSecs(int fileReporterIntervalSecs) {
        this.fileReporterIntervalSecs = fileReporterIntervalSecs;
    }

    public int getFileReporterIntervalSecs() {
        return fileReporterIntervalSecs;
    }

    public void setThreadsWatcherIntervalMS(int ms) {
        this.threadsWatcherIntervalMS = ms;
    }

    public int getThreadsWatcherIntervalMS() {
        return this.threadsWatcherIntervalMS;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxThreads() {
        return this.maxThreads;
    }

    public void setMaxThreadsMaxLogIntervalSecs(int maxThreadsMaxLogIntervalSecs) {
        this.maxThreadsMaxLogIntervalSecs = maxThreadsMaxLogIntervalSecs;
    }

    public int getMaxThreadsMaxLogIntervalSecs() {
        return maxThreadsMaxLogIntervalSecs;
    }

    public void setDeadlockedThreadsMaxLogIntervalSecs(int deadlockedThreadsMaxLogIntervalSecs) {
        this.deadlockedThreadsMaxLogIntervalSecs = deadlockedThreadsMaxLogIntervalSecs;
    }

    public int getDeadlockedThreadsMaxLogIntervalSecs() {
        return deadlockedThreadsMaxLogIntervalSecs;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("threadsWatcherIntervalMS", threadsWatcherIntervalMS)
                .add("maxThreads", maxThreads)
                .add("maxThreadsMaxLogIntervalSecs", maxThreadsMaxLogIntervalSecs)
                .add("deadlockedThreadsMaxLogIntervalSecs", deadlockedThreadsMaxLogIntervalSecs)
                .add("fileReporterIntervalSecs", fileReporterIntervalSecs)
                .toString();
    }

    // When any other project want to deal with Configuration like this, perhaps this could be moved somewhere re-usable
    private static void doIfIntPropertyIsPresent(
            Map<String, String> properties, String propertyName, Consumer<Integer> consumer) {
        String propertyValueAsString = properties.get(propertyName);
        if (propertyValueAsString != null) {
            try {
                Integer propertyValueAsInt = Integer.parseInt(propertyValueAsString);
                consumer.accept(propertyValueAsInt);
            } catch (NumberFormatException nfe) {
                LOG.warn("Ignored property '{}' that was expected to be an Integer but was not: {}", propertyName,
                        propertyValueAsString, nfe);
            }
        }
    }
}
