/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.NonZeroUint16Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.NonZeroUint32Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.forwardingrules.manager.config.rev160511.ForwardingRulesManagerConfigBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class OpenflowpluginConfiguration {

    @JsonIgnore
    private OpenflowProviderConfig openflowProviderConfig;
    @JsonIgnore
    private OpenflowProviderConfig defaultOpenflowProviderConfig = null;

    private SwitchConfig switchConfig;
    private boolean isStatisticsPollingOn;
    private int barrierCountLimit;
    private long barrierIntervalTimeoutLimit;
    private long echoReplyTimeout;
    private boolean enableFlowRemovedNotification;
    private boolean skipTableFeatures;
    private long basicTimerDelay;
    private long maximumTimerDelay;
    private boolean switchFeaturesMandatory;
    private boolean isStatisticsRpcEnabled;
    private boolean useSingleLayerSerialization;
    private int rpcRequestsQuota;
    private long globalNotificationQuota;
    private int threadPoolMinThreads;
    private int threadPoolMaxThreads;
    private long threadPoolTimeout;
    private boolean isFrmDisableReconciliation = false;
    private boolean isFrmStaleMarkingEnabled = false;
    private int frmReconciliationRetryCount;
    private boolean isFrmBundleBasedReconciliationEnabled = false;
    private boolean enableForwardingRulesManager;
    private Uint16 deviceConnectionHoldTimeInSeconds;

    protected OpenflowpluginConfiguration() {
        this.switchConfig = new SwitchConfig();
    }

    public OpenflowProviderConfig getDefaultProviderConfig() {
        if (this.defaultOpenflowProviderConfig == null) {
            this.defaultOpenflowProviderConfig = new OpenflowProviderConfigBuilder()
                    .setIsStatisticsPollingOn(this.isStatisticsPollingOn)
                    .setBarrierCountLimit(new NonZeroUint16Type(Uint16.valueOf(this.barrierCountLimit)))
                    .setBarrierIntervalTimeoutLimit(
                            new NonZeroUint32Type(Uint32.valueOf(this.barrierIntervalTimeoutLimit)))
                    .setEchoReplyTimeout(new NonZeroUint32Type(Uint32.valueOf(this.echoReplyTimeout)))
                    .setEnableFlowRemovedNotification(this.enableFlowRemovedNotification)
                    .setSkipTableFeatures(this.skipTableFeatures)
                    .setBasicTimerDelay(new NonZeroUint32Type(Uint32.valueOf(this.basicTimerDelay)))
                    .setMaximumTimerDelay(new NonZeroUint32Type(Uint32.valueOf(this.maximumTimerDelay)))
                    .setSwitchFeaturesMandatory(this.switchFeaturesMandatory)
                    .setIsStatisticsRpcEnabled(this.isStatisticsRpcEnabled)
                    .setUseSingleLayerSerialization(this.useSingleLayerSerialization)
                    .setRpcRequestsQuota(new NonZeroUint16Type(Uint16.valueOf(this.rpcRequestsQuota)))
                    .setGlobalNotificationQuota(Uint32.valueOf(this.globalNotificationQuota))
                    .setThreadPoolMinThreads(Uint16.valueOf(this.threadPoolMinThreads))
                    .setThreadPoolMaxThreads(new NonZeroUint16Type(Uint16.valueOf(this.threadPoolMaxThreads)))
                    .setThreadPoolTimeout(Uint32.valueOf(this.threadPoolTimeout))
                    .setIsQueueStatisticsPollingOn(true)
                    .setIsFlowStatisticsPollingOn(true)
                    .setIsTableStatisticsPollingOn(true)
                    .setDeviceConnectionRateLimitPerMin(Uint16.valueOf(0))
                    .setIsGroupStatisticsPollingOn(true)
                    .setIsPortStatisticsPollingOn(true)
                    .setIsMeterStatisticsPollingOn(true)
                    .setDeviceConnectionHoldTimeInSeconds(this.deviceConnectionHoldTimeInSeconds)
                    .build();
        }
        return this.defaultOpenflowProviderConfig;
    }

    public OpenflowProviderConfig getOpenflowProviderConfig() {
        return new OpenflowProviderConfigBuilder()
                .setIsStatisticsPollingOn(this.isStatisticsPollingOn)
                .setBarrierCountLimit(new NonZeroUint16Type(Uint16.valueOf(this.barrierCountLimit)))
                .setBarrierIntervalTimeoutLimit(new NonZeroUint32Type(Uint32.valueOf(this.barrierIntervalTimeoutLimit)))
                .setEchoReplyTimeout(new NonZeroUint32Type(Uint32.valueOf(this.echoReplyTimeout)))
                .setEnableFlowRemovedNotification(this.enableFlowRemovedNotification)
                .setSkipTableFeatures(this.skipTableFeatures)
                .setBasicTimerDelay(new NonZeroUint32Type(Uint32.valueOf(this.basicTimerDelay)))
                .setMaximumTimerDelay(new NonZeroUint32Type(Uint32.valueOf(this.maximumTimerDelay)))
                .setSwitchFeaturesMandatory(this.switchFeaturesMandatory)
                .setIsStatisticsRpcEnabled(this.isStatisticsRpcEnabled)
                .setUseSingleLayerSerialization(this.useSingleLayerSerialization)
                .setRpcRequestsQuota(new NonZeroUint16Type(Uint16.valueOf(this.rpcRequestsQuota)))
                .setGlobalNotificationQuota(Uint32.valueOf(this.globalNotificationQuota))
                .setThreadPoolMinThreads(Uint16.valueOf(this.threadPoolMinThreads))
                .setThreadPoolMaxThreads(new NonZeroUint16Type(Uint16.valueOf(this.threadPoolMaxThreads)))
                .setThreadPoolTimeout(Uint32.valueOf(this.threadPoolTimeout))
                .setIsQueueStatisticsPollingOn(true)
                .setIsFlowStatisticsPollingOn(true)
                .setIsTableStatisticsPollingOn(true)
                .setDeviceConnectionRateLimitPerMin(Uint16.valueOf(0))
                .setIsGroupStatisticsPollingOn(true)
                .setIsPortStatisticsPollingOn(true)
                .setIsMeterStatisticsPollingOn(true)
                .setDeviceConnectionHoldTimeInSeconds(this.deviceConnectionHoldTimeInSeconds)
                .build();
    }

    /**
     * Create configuration settings need for initialize ForwardingRulesManager.
     * @return instance of {@link ForwardingRulesManagerConfigBuilder}.
     */
    public ForwardingRulesManagerConfigBuilder getFrmConfigBuilder() {
        final ForwardingRulesManagerConfigBuilder frmConfigBuilder = new ForwardingRulesManagerConfigBuilder();
        frmConfigBuilder.setDisableReconciliation(this.isFrmDisableReconciliation);
        frmConfigBuilder.setStaleMarkingEnabled(this.isFrmStaleMarkingEnabled);
        frmConfigBuilder.setReconciliationRetryCount(Uint16.valueOf(this.frmReconciliationRetryCount));
        frmConfigBuilder.setBundleBasedReconciliationEnabled(this.isFrmBundleBasedReconciliationEnabled);
        return frmConfigBuilder;
    }

    public boolean isEnableForwardingRulesManager() {
        return this.enableForwardingRulesManager;
    }

    public void setEnableForwardingRulesManager(boolean enableForwardingRulesManager) {
        this.enableForwardingRulesManager = enableForwardingRulesManager;
    }

    public boolean isFrmDisableReconciliation() {
        return isFrmDisableReconciliation;
    }

    public void setFrmDisableReconciliation(boolean frmDisableReconciliation) {
        this.isFrmDisableReconciliation = frmDisableReconciliation;
    }

    public boolean isFrmStaleMarkingEnabled() {
        return this.isFrmStaleMarkingEnabled;
    }

    public void setFrmStaleMarkingEnabled(boolean frmStaleMarkingEnabled) {
        this.isFrmStaleMarkingEnabled = frmStaleMarkingEnabled;
    }

    public int getFrmReconciliationRetryCount() {
        return this.frmReconciliationRetryCount;
    }

    public void setFrmReconciliationRetryCount(int frmReconciliationRetryCount) {
        this.frmReconciliationRetryCount = frmReconciliationRetryCount;
    }

    public boolean isFrmBundleBasedReconciliationEnabled() {
        return isFrmBundleBasedReconciliationEnabled;
    }

    public void setFrmBundleBasedReconciliationEnabled(boolean frmBundleBasedReconciliationEnabled) {
        this.isFrmBundleBasedReconciliationEnabled = frmBundleBasedReconciliationEnabled;
    }

    public boolean isStatisticsPollingOn() {
        return this.isStatisticsPollingOn;
    }

    public void setIsStatisticsPollingOn(final boolean statisticsPollingOn) {
        this.isStatisticsPollingOn = statisticsPollingOn;
    }

    public int getBarrierCountLimit() {
        return this.barrierCountLimit;
    }

    public void setBarrierCountLimit(final int barrierCountLimit) {
        this.barrierCountLimit = barrierCountLimit;
    }

    public long getBarrierIntervalTimeoutLimit() {
        return this.barrierIntervalTimeoutLimit;
    }

    public void setBarrierIntervalTimeoutLimit(final long barrierIntervalTimeoutLimit) {
        this.barrierIntervalTimeoutLimit = barrierIntervalTimeoutLimit;
    }

    public long getEchoReplyTimeout() {
        return this.echoReplyTimeout;
    }

    public void setEchoReplyTimeout(final long echoReplyTimeout) {
        this.echoReplyTimeout = echoReplyTimeout;
    }

    public boolean getEnableFlowRemovedNotification() {
        return this.enableFlowRemovedNotification;
    }

    public void setEnableFlowRemovedNotification(final boolean enableFlowRemovedNotification) {
        this.enableFlowRemovedNotification = enableFlowRemovedNotification;
    }

    public boolean getIsSkipTableFeatures() {
        return this.skipTableFeatures;
    }

    public void setIsSkipTableFeatures(final boolean skipFeatures) {
        this.skipTableFeatures = skipFeatures;
    }

    public long getBasicTimerDelay() {
        return this.basicTimerDelay;
    }

    public void setBasicTimerDelay(final long basicTimerDelay) {
        this.basicTimerDelay = basicTimerDelay;
    }

    public long getMaximumTimerDelay() {
        return this.maximumTimerDelay;
    }

    public void setMaximumTimerDelay(final long maximumTimerDelay) {
        this.maximumTimerDelay = maximumTimerDelay;
    }

    public boolean getSwitchFeaturesMandatory() {
        return this.switchFeaturesMandatory;
    }

    public void setSwitchFeaturesMandatory(final boolean switchFeaturesMandatory) {
        this.switchFeaturesMandatory = switchFeaturesMandatory;
    }

    public boolean getIsStatisticsRpcEnabled() {
        return this.isStatisticsRpcEnabled;
    }

    public void setIsStatisticsRpcEnabled(final boolean statisticsRpcEnabled) {
        this.isStatisticsRpcEnabled = statisticsRpcEnabled;
    }

    public boolean getUseSingleLayerSerialization() {
        return this.useSingleLayerSerialization;
    }

    public void setUseSingleLayerSerialization(final boolean useSingleLayerSerialization) {
        this.useSingleLayerSerialization = useSingleLayerSerialization;
    }

    public int getRpcRequestsQuota() {
        return this.rpcRequestsQuota;
    }

    public void setRpcRequestsQuota(final int rpcRequestsQuota) {
        this.rpcRequestsQuota = rpcRequestsQuota;
    }

    public long getGlobalNotificationQuota() {
        return this.globalNotificationQuota;
    }

    public void setGlobalNotificationQuota(final long globalNotificationQuota) {
        this.globalNotificationQuota = globalNotificationQuota;
    }

    public int getThreadPoolMinThreads() {
        return this.threadPoolMinThreads;
    }

    public void setThreadPoolMinThreads(final int threadPoolMinThreads) {
        this.threadPoolMinThreads = threadPoolMinThreads;
    }

    public int getThreadPoolMaxThreads() {
        return this.threadPoolMaxThreads;
    }

    public void setThreadPoolMaxThreads(final int threadPoolMaxThreads) {
        this.threadPoolMaxThreads = threadPoolMaxThreads;
    }

    public long getThreadPoolTimeout() {
        return this.threadPoolTimeout;
    }

    public void setThreadPoolTimeout(final long threadPoolTimeout) {
        this.threadPoolTimeout = threadPoolTimeout;
    }

    public boolean getSkipTableFeatures() {
        return this.skipTableFeatures;
    }

    public void setSkipTableFeatures(final boolean skipTableFeatures) {
        this.skipTableFeatures = skipTableFeatures;
    }

    public SwitchConfig getSwitchConfig() {
        return this.switchConfig;
    }

    public void setSwitchConfig(final SwitchConfig switchConfig) {
        this.switchConfig = switchConfig;
    }

    public Uint16 getDeviceConnectionHoldTimeInSeconds() {
        return deviceConnectionHoldTimeInSeconds;
    }

    public void setDeviceConnectionHoldTimeInSeconds(Uint16 deviceConnectionHoldTimeInSeconds) {
        this.deviceConnectionHoldTimeInSeconds = deviceConnectionHoldTimeInSeconds;
    }
}