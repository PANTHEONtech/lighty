/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.impl.config;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.openflowplugin.api.openflow.configuration.ConfigurationListener;
import org.opendaylight.openflowplugin.api.openflow.configuration.ConfigurationProperty;
import org.opendaylight.openflowplugin.api.openflow.configuration.ConfigurationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceFactory.class);

    public ConfigurationService newInstance(final OpenflowProviderConfig providerConfig) {
        return new ConfigurationServiceImpl(providerConfig);
    }

    private static final class ConfigurationServiceImpl implements ConfigurationService {
        private final Map<String, String> propertyMap = new HashMap<>();
        private final List<ConfigurationListener> listeners = new ArrayList<>();

        ConfigurationServiceImpl(final OpenflowProviderConfig providerConfig) {
            update(ImmutableMap
                    .<String, String>builder()
                    .put(ConfigurationProperty.RPC_REQUESTS_QUOTA.toString(),
                            providerConfig.getRpcRequestsQuota().getValue().toString())
                    .put(ConfigurationProperty.GLOBAL_NOTIFICATION_QUOTA.toString(),
                            providerConfig.getGlobalNotificationQuota().toString())
                    .put(ConfigurationProperty.SWITCH_FEATURES_MANDATORY.toString(),
                            providerConfig.getSwitchFeaturesMandatory().toString())
                    .put(ConfigurationProperty.ENABLE_FLOW_REMOVED_NOTIFICATION.toString(),
                            providerConfig.getEnableFlowRemovedNotification().toString())
                    .put(ConfigurationProperty.BARRIER_COUNT_LIMIT.toString(),
                            providerConfig.getBarrierCountLimit().getValue().toString())
                    .put(ConfigurationProperty.BARRIER_INTERVAL_TIMEOUT_LIMIT.toString(),
                            providerConfig.getBarrierIntervalTimeoutLimit().getValue().toString())
                    .put(ConfigurationProperty.ECHO_REPLY_TIMEOUT.toString(),
                            providerConfig.getEchoReplyTimeout().getValue().toString())
                    .put(ConfigurationProperty.IS_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsStatisticsPollingOn().toString())
                    .put(ConfigurationProperty.SKIP_TABLE_FEATURES.toString(),
                            providerConfig.getSkipTableFeatures().toString())
                    .put(ConfigurationProperty.BASIC_TIMER_DELAY.toString(),
                            providerConfig.getBasicTimerDelay().getValue().toString())
                    .put(ConfigurationProperty.MAXIMUM_TIMER_DELAY.toString(),
                            providerConfig.getMaximumTimerDelay().getValue().toString())
                    .put(ConfigurationProperty.USE_SINGLE_LAYER_SERIALIZATION.toString(),
                            providerConfig.getUseSingleLayerSerialization().toString())
                    .put(ConfigurationProperty.THREAD_POOL_MIN_THREADS.toString(),
                            providerConfig.getThreadPoolMinThreads().toString())
                    .put(ConfigurationProperty.THREAD_POOL_MAX_THREADS.toString(),
                            providerConfig.getThreadPoolMaxThreads().getValue().toString())
                    .put(ConfigurationProperty.THREAD_POOL_TIMEOUT.toString(),
                            providerConfig.getThreadPoolTimeout().toString())
                    .put(ConfigurationProperty.IS_FLOW_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsFlowStatisticsPollingOn().toString())
                    .put(ConfigurationProperty.IS_QUEUE_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsQueueStatisticsPollingOn().toString())
                    .put(ConfigurationProperty.IS_TABLE_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsTableStatisticsPollingOn().toString())
                    .put(ConfigurationProperty.IS_GROUP_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsGroupStatisticsPollingOn().toString())
                    .put(ConfigurationProperty.IS_PORT_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsPortStatisticsPollingOn().toString())
                    .put(ConfigurationProperty.IS_METER_STATISTICS_POLLING_ON.toString(),
                            providerConfig.getIsMeterStatisticsPollingOn().toString())
                    // FIXME add some normal value
                    .put(ConfigurationProperty.DEVICE_CONNECTION_RATE_LIMIT_PER_MIN.toString(),
                            providerConfig.getDeviceConnectionRateLimitPerMin().toString())
                    .put(ConfigurationProperty.DEVICE_CONNECTION_HOLD_TIME_IN_SECONDS.toString(),
                            providerConfig.getDeviceConnectionHoldTimeInSeconds().toString())
                    .build());
        }

        @Override
        public void update(@Nonnull final Map<String, String> properties) {
            properties.forEach((propertyName, newValue) -> {
                final String originalValue = this.propertyMap.get(propertyName);

                if (Objects.nonNull(originalValue)) {
                    if (originalValue.equals(newValue)) {
                        return;
                    }

                    LOG.info("{} configuration property was changed from '{}' to '{}'",
                            propertyName,
                            originalValue,
                            newValue);
                } else {
                    if (Objects.isNull(newValue)) {
                        return;
                    }

                    LOG.info("{} configuration property was changed to '{}'", propertyName, newValue);
                }

                this.propertyMap.put(propertyName, newValue);
                this.listeners.forEach(listener -> listener.onPropertyChanged(propertyName, newValue));
            });
        }

        @Nonnull
        @Override
        public <T> T getProperty(@Nonnull final String key, @Nonnull final Function<String, T> transformer) {
            return transformer.apply(this.propertyMap.get(key));
        }

        @Nonnull
        @Override
        public AutoCloseable registerListener(@Nonnull final ConfigurationListener listener) {
            Verify.verify(!this.listeners.contains(listener));
            LOG.info("{} was registered as configuration listener to OpenFlowPlugin configuration service", listener);
            this.listeners.add(listener);
            this.propertyMap.forEach(listener::onPropertyChanged);
            return () -> this.listeners.remove(listener);
        }

        @Override
        public void close() throws Exception {
            this.propertyMap.clear();
            this.listeners.clear();
        }
    }
}