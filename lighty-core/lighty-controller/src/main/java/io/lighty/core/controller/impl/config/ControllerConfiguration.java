/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.typesafe.config.Config;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Complete configuration for Lighty controller.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ControllerConfiguration {

    private String restoreDirectoryPath = "./clustered-datastore-restore";
    private int maxDataBrokerFutureCallbackQueueSize = 1000;
    private int maxDataBrokerFutureCallbackPoolSize = 10;
    private boolean metricCaptureEnabled = false;
    private int mailboxCapacity = 1000;
    private String moduleShardsConfig = "configuration/initial/module-shards.conf";
    private String modulesConfig = "configuration/initial/modules.conf";

    private DOMNotificationRouterConfig domNotificationRouterConfig;
    private ActorSystemConfig actorSystemConfig;
    private InitialConfigData initialConfigData;

    @JsonIgnore
    private SchemaServiceConfig schemaServiceConfig;
    private Properties distributedEosProperties;

    @JsonIgnore
    private DatastoreContext configDatastoreContext;
    @JsonIgnore
    private DatastoreContext operDatastoreContext;

    private Map<String, Object> datastoreProperties;

    public ControllerConfiguration() {
        this.domNotificationRouterConfig = new DOMNotificationRouterConfig();
        this.actorSystemConfig = new ActorSystemConfig();
        this.schemaServiceConfig = new SchemaServiceConfig();
        this.distributedEosProperties = new Properties();
        this.configDatastoreContext = DatastoreConfigurationUtils.createDefaultConfigDatastoreContext();
        this.operDatastoreContext = DatastoreConfigurationUtils.createDefaultOperationalDatastoreContext();
        this.datastoreProperties = DatastoreConfigurationUtils.getDefaultDatastoreProperties();
    }

    public static class InitialConfigData {

        public enum ImportFileFormat {
            JSON("json"),
            XML("xml"),
            NOT_SUPPORTED("");

            private String fileFormat;

            ImportFileFormat(String formatString) {
                this.fileFormat = formatString;
            }

            public String getFormatString() {
                return fileFormat;
            }

            @JsonCreator
            public static ImportFileFormat getFormatType(String fullName) {
                for (ImportFileFormat formatType : ImportFileFormat.values()) {
                    if (formatType.fileFormat.equalsIgnoreCase(fullName)) {
                        return formatType;
                    }
                }
                return NOT_SUPPORTED;
            }
        }

        private String pathToInitDataFile = null;
        @JsonProperty(value = "format")
        private ImportFileFormat fileFormat = null;

        public String getPathToInitDataFile() {
            return pathToInitDataFile;
        }

        public void setPathToInitDataFile(String pathToInitDataFile) {
            this.pathToInitDataFile = pathToInitDataFile;
        }

        public ImportFileFormat getFormat() {
            return fileFormat;
        }

        public void setFormat(ImportFileFormat newFileFormat) {
            this.fileFormat = newFileFormat;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            InitialConfigData that = (InitialConfigData) obj;

            if (fileFormat != that.fileFormat) {
                return false;
            }
            return pathToInitDataFile.equals(that.pathToInitDataFile);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this);
        }
    }

    public static class DOMNotificationRouterConfig {

        private int queueDepth = 65536;
        private long spinTime = 0;
        private long parkTime = 0;
        private TimeUnit unit = TimeUnit.MILLISECONDS;

        public int getQueueDepth() {
            return queueDepth;
        }

        public void setQueueDepth(final int queueDepth) {
            this.queueDepth = queueDepth;
        }

        public long getSpinTime() {
            return spinTime;
        }

        public void setSpinTime(final long spinTime) {
            this.spinTime = spinTime;
        }

        public long getParkTime() {
            return parkTime;
        }

        public void setParkTime(final long parkTime) {
            this.parkTime = parkTime;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(final TimeUnit unit) {
            this.unit = unit;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            DOMNotificationRouterConfig that = (DOMNotificationRouterConfig) obj;

            if (queueDepth != that.queueDepth) {
                return false;
            }
            if (spinTime != that.spinTime) {
                return false;
            }
            if (parkTime != that.parkTime) {
                return false;
            }
            return unit == that.unit;
        }

        @Override
        public int hashCode() {
            int result = queueDepth;
            result = 31 * result + (int) (spinTime ^ spinTime >>> 32);
            result = 31 * result + (int) (parkTime ^ parkTime >>> 32);
            result = 31 * result + (unit != null ? unit.hashCode() : 0);
            return result;
        }

    }

    public static class ActorSystemConfig {

        private String akkaConfigPath = "singlenode/akka-default.conf";
        private String factoryAkkaConfigPath = "singlenode/factory-akka-default.conf";

        @JsonIgnore
        private Config config;
        @JsonIgnore
        private ClassLoader classLoader;

        public String getAkkaConfigPath() {
            return akkaConfigPath;
        }

        public void setAkkaConfigPath(final String akkaConfigPath) {
            this.akkaConfigPath = akkaConfigPath;
        }

        public String getFactoryAkkaConfigPath() {
            return factoryAkkaConfigPath;
        }

        public void setFactoryAkkaConfigPath(final String factoryAkkaConfigPath) {
            this.factoryAkkaConfigPath = factoryAkkaConfigPath;
        }

        public Config getConfig() {
            return config;
        }

        public void setConfig(final Config config) {
            this.config = config;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public void setClassLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            ActorSystemConfig that = (ActorSystemConfig) obj;

            if (akkaConfigPath != null ? !akkaConfigPath.equals(that.akkaConfigPath) : that.akkaConfigPath != null) {
                return false;
            }
            if (factoryAkkaConfigPath != null ? !factoryAkkaConfigPath.equals(that.factoryAkkaConfigPath)
                    : that.factoryAkkaConfigPath != null) {
                return false;
            }
            if (config != null ? !config.equals(that.config) : that.config != null) {
                return false;
            }
            return classLoader != null ? classLoader.equals(that.classLoader) : that.classLoader == null;
        }

        @Override
        public int hashCode() {
            int result = akkaConfigPath != null ? akkaConfigPath.hashCode() : 0;
            result = 31 * result + (factoryAkkaConfigPath != null ? factoryAkkaConfigPath.hashCode() : 0);
            result = 31 * result + (config != null ? config.hashCode() : 0);
            result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
            return result;
        }
    }

    /**
     * Contains list of paths to artifacts containing yang models for Lighty SchemaContext.
     */
    public static class SchemaServiceConfig {
        private Set<YangModuleInfo> models;

        public SchemaServiceConfig() {
            models = new HashSet<>();
        }

        public Set<YangModuleInfo> getModels() {
            return models;
        }

        public void setModels(final Set<YangModuleInfo> models) {
            this.models = models;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SchemaServiceConfig that = (SchemaServiceConfig) obj;
            return Objects.equal(models, that.models);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(models);
        }
    }

    public String getRestoreDirectoryPath() {
        return restoreDirectoryPath;
    }

    public void setRestoreDirectoryPath(final String restoreDirectoryPath) {
        this.restoreDirectoryPath = restoreDirectoryPath;
    }

    public DOMNotificationRouterConfig getDomNotificationRouterConfig() {
        return domNotificationRouterConfig;
    }

    public void setDomNotificationRouterConfig(final DOMNotificationRouterConfig domNotificationRouterConfig) {
        this.domNotificationRouterConfig = domNotificationRouterConfig;
    }

    public ActorSystemConfig getActorSystemConfig() {
        return actorSystemConfig;
    }

    public void setActorSystemConfig(final ActorSystemConfig actorSystemConfig) {
        this.actorSystemConfig = actorSystemConfig;
    }

    public SchemaServiceConfig getSchemaServiceConfig() {
        return schemaServiceConfig;
    }

    public void setSchemaServiceConfig(final SchemaServiceConfig schemaServiceConfig) {
        this.schemaServiceConfig = schemaServiceConfig;
    }

    public int getMaxDataBrokerFutureCallbackQueueSize() {
        return maxDataBrokerFutureCallbackQueueSize;
    }

    public void setMaxDataBrokerFutureCallbackQueueSize(final int maxDataBrokerFutureCallbackQueueSize) {
        this.maxDataBrokerFutureCallbackQueueSize = maxDataBrokerFutureCallbackQueueSize;
    }

    public int getMaxDataBrokerFutureCallbackPoolSize() {
        return maxDataBrokerFutureCallbackPoolSize;
    }

    public void setMaxDataBrokerFutureCallbackPoolSize(final int maxDataBrokerFutureCallbackPoolSize) {
        this.maxDataBrokerFutureCallbackPoolSize = maxDataBrokerFutureCallbackPoolSize;
    }

    public boolean isMetricCaptureEnabled() {
        return metricCaptureEnabled;
    }

    public void setMetricCaptureEnabled(final boolean metricCaptureEnabled) {
        this.metricCaptureEnabled = metricCaptureEnabled;
    }

    public int getMailboxCapacity() {
        return mailboxCapacity;
    }

    public void setMailboxCapacity(final int mailboxCapacity) {
        this.mailboxCapacity = mailboxCapacity;
    }

    public Properties getDistributedEosProperties() {
        return distributedEosProperties;
    }

    public void setDistributedEosProperties(final Properties distributedEosProperties) {
        this.distributedEosProperties = distributedEosProperties;
    }

    public void addDistributedEosProperty(final String key, final String value) {
        this.distributedEosProperties.put(key, value);
    }

    public String getModuleShardsConfig() {
        return moduleShardsConfig;
    }

    public void setModuleShardsConfig(final String moduleShardsConfig) {
        this.moduleShardsConfig = moduleShardsConfig;
    }

    public String getModulesConfig() {
        return modulesConfig;
    }

    public void setModulesConfig(final String modulesConfig) {
        this.modulesConfig = modulesConfig;
    }

    public DatastoreContext getConfigDatastoreContext() {
        return configDatastoreContext;
    }

    public void setConfigDatastoreContext(final DatastoreContext configDatastoreContext) {
        this.configDatastoreContext = configDatastoreContext;
    }

    public DatastoreContext getOperDatastoreContext() {
        return operDatastoreContext;
    }

    public void setOperDatastoreContext(final DatastoreContext operDatastoreContext) {
        this.operDatastoreContext = operDatastoreContext;
    }

    public Map<String, Object> getDatastoreProperties() {
        return datastoreProperties;
    }

    public void setDatastoreProperties(final Map<String, Object> datastoreProperties) {
        this.datastoreProperties = datastoreProperties;
    }

    public void setInitialConfigData(InitialConfigData initialConfigData) {
        this.initialConfigData = initialConfigData;
    }

    public InitialConfigData getInitialConfigData() {
        return initialConfigData;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ControllerConfiguration that = (ControllerConfiguration) obj;

        if (maxDataBrokerFutureCallbackQueueSize != that.maxDataBrokerFutureCallbackQueueSize) {
            return false;
        }
        if (maxDataBrokerFutureCallbackPoolSize != that.maxDataBrokerFutureCallbackPoolSize) {
            return false;
        }
        if (metricCaptureEnabled != that.metricCaptureEnabled) {
            return false;
        }
        if (mailboxCapacity != that.mailboxCapacity) {
            return false;
        }
        if (!restoreDirectoryPath.equals(that.restoreDirectoryPath)) {
            return false;
        }
        if (!moduleShardsConfig.equals(that.moduleShardsConfig)) {
            return false;
        }
        if (!modulesConfig.equals(that.modulesConfig)) {
            return false;
        }
        if (!domNotificationRouterConfig.equals(that.domNotificationRouterConfig)) {
            return false;
        }
        if (!actorSystemConfig.equals(that.actorSystemConfig)) {
            return false;
        }
        if (!schemaServiceConfig.equals(that.schemaServiceConfig)) {
            return false;
        }
        if (!configDatastoreContext.equals(that.configDatastoreContext)) {
            return false;
        }
        if (!operDatastoreContext.equals(that.operDatastoreContext)) {
            return false;
        }
        if (initialConfigData != null && that.initialConfigData != null) {
            if (!initialConfigData.equals(that.initialConfigData)) {
                return false;
            }
        }
        return distributedEosProperties.equals(that.distributedEosProperties);
    }

    @Override
    public int hashCode() {
        int result = restoreDirectoryPath.hashCode();
        result = 31 * result + maxDataBrokerFutureCallbackQueueSize;
        result = 31 * result + maxDataBrokerFutureCallbackPoolSize;
        result = 31 * result + (metricCaptureEnabled ? 1 : 0);
        result = 31 * result + mailboxCapacity;
        result = 31 * result + moduleShardsConfig.hashCode();
        result = 31 * result + modulesConfig.hashCode();
        result = 31 * result + domNotificationRouterConfig.hashCode();
        result = 31 * result + actorSystemConfig.hashCode();
        result = 31 * result + schemaServiceConfig.hashCode();
        result = 31 * result + distributedEosProperties.hashCode();
        result = 31 * result + configDatastoreContext.hashCode();
        result = 31 * result + operDatastoreContext.hashCode();
        if (initialConfigData != null) {
            result = 31 * result + initialConfigData.hashCode();
        }
        return result;
    }
}
