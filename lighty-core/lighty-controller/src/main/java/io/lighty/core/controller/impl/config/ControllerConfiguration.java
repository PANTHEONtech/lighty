/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;
import com.typesafe.config.Config;
import io.lighty.core.controller.impl.util.DatastoreConfigurationUtils;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Complete configuration for Lighty controller
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

    @JsonIgnore
    private SchemaServiceConfig schemaServiceConfig;
    private Properties distributedEosProperties;

    @JsonIgnore
    private DatastoreContext configDatastoreContext;
    @JsonIgnore
    private DatastoreContext operDatastoreContext;

    public ControllerConfiguration() {
        this.domNotificationRouterConfig = new DOMNotificationRouterConfig();
        this.actorSystemConfig = new ActorSystemConfig();
        this.schemaServiceConfig = new SchemaServiceConfig();
        this.distributedEosProperties = new Properties();
        this.configDatastoreContext = DatastoreConfigurationUtils.createDefaultConfigDatastoreContext();
        this.operDatastoreContext = DatastoreConfigurationUtils.createDefaultOperationalDatastoreContext();
    }

    public static class  DOMNotificationRouterConfig {

        private int queueDepth = 65536;
        private long spinTime = 0;
        private long parkTime = 0;
        private TimeUnit unit = TimeUnit.MILLISECONDS;

        public int getQueueDepth() {
            return queueDepth;
        }

        public void setQueueDepth(int queueDepth) {
            this.queueDepth = queueDepth;
        }

        public long getSpinTime() {
            return spinTime;
        }

        public void setSpinTime(long spinTime) {
            this.spinTime = spinTime;
        }

        public long getParkTime() {
            return parkTime;
        }

        public void setParkTime(long parkTime) {
            this.parkTime = parkTime;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(TimeUnit unit) {
            this.unit = unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DOMNotificationRouterConfig that = (DOMNotificationRouterConfig) o;

            if (queueDepth != that.queueDepth) return false;
            if (spinTime != that.spinTime) return false;
            if (parkTime != that.parkTime) return false;
            return unit == that.unit;
        }

        @Override
        public int hashCode() {
            int result = queueDepth;
            result = 31 * result + (int) (spinTime ^ (spinTime >>> 32));
            result = 31 * result + (int) (parkTime ^ (parkTime >>> 32));
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

        public void setAkkaConfigPath(String akkaConfigPath) {
            this.akkaConfigPath = akkaConfigPath;
        }

        public String getFactoryAkkaConfigPath() {
            return factoryAkkaConfigPath;
        }

        public void setFactoryAkkaConfigPath(String factoryAkkaConfigPath) {
            this.factoryAkkaConfigPath = factoryAkkaConfigPath;
        }

        public Config getConfig() {
            return config;
        }

        public void setConfig(Config config) {
            this.config = config;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public void setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ActorSystemConfig that = (ActorSystemConfig) o;

            if (akkaConfigPath != null ? !akkaConfigPath.equals(that.akkaConfigPath) : that.akkaConfigPath != null)
                return false;
            if (factoryAkkaConfigPath != null ?
                    !factoryAkkaConfigPath.equals(that.factoryAkkaConfigPath) :
                    that.factoryAkkaConfigPath != null)
                return false;
            if (config != null ? !config.equals(that.config) : that.config != null)
                return false;
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
    public class SchemaServiceConfig {

        private Set<YangModuleInfo> models;

        public SchemaServiceConfig() {
            models = new HashSet<>();
        }

        public Set<YangModuleInfo> getModels() {
            return models;
        }

        public void setModels(Set<YangModuleInfo> models) {
            this.models = models;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SchemaServiceConfig that = (SchemaServiceConfig) o;
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

    public void setRestoreDirectoryPath(String restoreDirectoryPath) {
        this.restoreDirectoryPath = restoreDirectoryPath;
    }

    public DOMNotificationRouterConfig getDomNotificationRouterConfig() {
        return domNotificationRouterConfig;
    }

    public void setDomNotificationRouterConfig(DOMNotificationRouterConfig domNotificationRouterConfig) {
        this.domNotificationRouterConfig = domNotificationRouterConfig;
    }

    public ActorSystemConfig getActorSystemConfig() {
        return actorSystemConfig;
    }

    public void setActorSystemConfig(ActorSystemConfig actorSystemConfig) {
        this.actorSystemConfig = actorSystemConfig;
    }

    public SchemaServiceConfig getSchemaServiceConfig() {
        return schemaServiceConfig;
    }

    public void setSchemaServiceConfig(SchemaServiceConfig schemaServiceConfig) {
        this.schemaServiceConfig = schemaServiceConfig;
    }

    public int getMaxDataBrokerFutureCallbackQueueSize() {
        return maxDataBrokerFutureCallbackQueueSize;
    }

    public void setMaxDataBrokerFutureCallbackQueueSize(int maxDataBrokerFutureCallbackQueueSize) {
        this.maxDataBrokerFutureCallbackQueueSize = maxDataBrokerFutureCallbackQueueSize;
    }

    public int getMaxDataBrokerFutureCallbackPoolSize() {
        return maxDataBrokerFutureCallbackPoolSize;
    }

    public void setMaxDataBrokerFutureCallbackPoolSize(int maxDataBrokerFutureCallbackPoolSize) {
        this.maxDataBrokerFutureCallbackPoolSize = maxDataBrokerFutureCallbackPoolSize;
    }

    public boolean isMetricCaptureEnabled() {
        return metricCaptureEnabled;
    }

    public void setMetricCaptureEnabled(boolean metricCaptureEnabled) {
        this.metricCaptureEnabled = metricCaptureEnabled;
    }

    public int getMailboxCapacity() {
        return mailboxCapacity;
    }

    public void setMailboxCapacity(int mailboxCapacity) {
        this.mailboxCapacity = mailboxCapacity;
    }

    public Properties getDistributedEosProperties() {
        return distributedEosProperties;
    }

    public void setDistributedEosProperties(Properties distributedEosProperties) {
        this.distributedEosProperties = distributedEosProperties;
    }

    public void addDistributedEosProperty(String key, String value) {
        this.distributedEosProperties.put(key, value);
    }

    public String getModuleShardsConfig() {
        return moduleShardsConfig;
    }

    public void setModuleShardsConfig(String moduleShardsConfig) {
        this.moduleShardsConfig = moduleShardsConfig;
    }

    public String getModulesConfig() {
        return modulesConfig;
    }

    public void setModulesConfig(String modulesConfig) {
        this.modulesConfig = modulesConfig;
    }

    public DatastoreContext getConfigDatastoreContext() {
        return configDatastoreContext;
    }

    public void setConfigDatastoreContext(DatastoreContext configDatastoreContext) {
        this.configDatastoreContext = configDatastoreContext;
    }

    public DatastoreContext getOperDatastoreContext() {
        return operDatastoreContext;
    }

    public void setOperDatastoreContext(DatastoreContext operDatastoreContext) {
        this.operDatastoreContext = operDatastoreContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ControllerConfiguration that = (ControllerConfiguration) o;

        if (maxDataBrokerFutureCallbackQueueSize != that.maxDataBrokerFutureCallbackQueueSize) return false;
        if (maxDataBrokerFutureCallbackPoolSize != that.maxDataBrokerFutureCallbackPoolSize) return false;
        if (metricCaptureEnabled != that.metricCaptureEnabled) return false;
        if (mailboxCapacity != that.mailboxCapacity) return false;
        if (!restoreDirectoryPath.equals(that.restoreDirectoryPath)) return false;
        if (!moduleShardsConfig.equals(that.moduleShardsConfig)) return false;
        if (!modulesConfig.equals(that.modulesConfig)) return false;
        if (!domNotificationRouterConfig.equals(that.domNotificationRouterConfig)) return false;
        if (!actorSystemConfig.equals(that.actorSystemConfig)) return false;
        if (!schemaServiceConfig.equals(that.schemaServiceConfig)) return false;
        if (!configDatastoreContext.equals(that.configDatastoreContext)) return false;
        if (!operDatastoreContext.equals(that.operDatastoreContext)) return false;
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
        return result;
    }
}
