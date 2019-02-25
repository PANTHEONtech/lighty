/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the LIGHTY.IO LICENSE,
 * version 1.1. If a copy of the license was not distributed with this file,
 * You can obtain one at https://lighty.io/license/1.1/
 */
package io.lighty.modules.southbound.openflow.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.NonZeroUint16Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.NonZeroUint32Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.forwardingrules.manager.config.rev160511.ForwardingRulesManagerConfigBuilder;

public class OpenflowpluginConfiguration {

    @JsonIgnore
    private OpenflowProviderConfig openflowProviderConfig;
    @JsonIgnore
    private OpenflowProviderConfig defaultOpenflowProviderConfig = null;

    private SwitchConfig switchConfig;
    private boolean isStatisticsPollingOn = true;
    private int barrierCountLimit = 25600;
    private long barrierIntervalTimeoutLimit = 500;
    private long echoReplyTimeout = 2000;
    private boolean enableFlowRemovedNotification = true;
    private boolean skipTableFeatures = true;
    private long basicTimerDelay = 3000;
    private long maximumTimerDelay = 3679;
    private boolean switchFeaturesMandatory = false;
    private boolean isStatisticsRpcEnabled = false;
    private boolean useSingleLayerSerialization = true;
    private int rpcRequestsQuota = 20000;
    private long globalNotificationQuota = 64000;
    private int threadPoolMinThreads = 1;
    private int threadPoolMaxThreads = 32000;
    private long threadPoolTimeout = 60;
    private boolean isFrmDisableReconciliation = false;
    private boolean isFrmStaleMarkingEnabled = false;
    private int frmReconciliationRetryCount = 5;
    private boolean isFrmBundleBasedReconciliationEnabled = false;
    private final NonZeroUint32Type nonZeroUint32Type = NonZeroUint32Type.getDefaultInstance("900000");
    private boolean isStateful = true;

    public OpenflowpluginConfiguration() {
        this.switchConfig = new SwitchConfig();
    }

    public OpenflowProviderConfig getDefaultProviderConfig() {
        if (this.defaultOpenflowProviderConfig == null) {
            this.defaultOpenflowProviderConfig = new OpenflowProviderConfigBuilder()
                    .setIsStatisticsPollingOn(this.isStatisticsPollingOn)
                    .setBarrierCountLimit(new NonZeroUint16Type(this.barrierCountLimit))
                    .setBarrierIntervalTimeoutLimit(new NonZeroUint32Type(this.barrierIntervalTimeoutLimit))
                    .setEchoReplyTimeout(new NonZeroUint32Type(this.echoReplyTimeout))
                    .setEnableFlowRemovedNotification(this.enableFlowRemovedNotification)
                    .setSkipTableFeatures(this.skipTableFeatures)
                    .setBasicTimerDelay(new NonZeroUint32Type(this.basicTimerDelay))
                    .setMaximumTimerDelay(new NonZeroUint32Type(this.maximumTimerDelay))
                    .setSwitchFeaturesMandatory(this.switchFeaturesMandatory)
                    .setIsStatisticsRpcEnabled(this.isStatisticsRpcEnabled)
                    .setUseSingleLayerSerialization(this.useSingleLayerSerialization)
                    .setRpcRequestsQuota(new NonZeroUint16Type(this.rpcRequestsQuota))
                    .setGlobalNotificationQuota(this.globalNotificationQuota)
                    .setThreadPoolMinThreads(this.threadPoolMinThreads)
                    .setThreadPoolMaxThreads(new NonZeroUint16Type(this.threadPoolMaxThreads))
                    .setThreadPoolTimeout(this.threadPoolTimeout)
                    .setMaximumTimerDelay(this.nonZeroUint32Type)
                    .setIsQueueStatisticsPollingOn(true)
                    .setIsFlowStatisticsPollingOn(true)
                    .setIsTableStatisticsPollingOn(true)
                    .setDeviceConnectionRateLimitPerMin(0)
                    .setIsGroupStatisticsPollingOn(true)
                    .setIsPortStatisticsPollingOn(true)
                    .setIsMeterStatisticsPollingOn(true)
                    .build();
        }
        return this.defaultOpenflowProviderConfig;
    }

    public OpenflowProviderConfig getOpenflowProviderConfig() {
        return new OpenflowProviderConfigBuilder()
                .setIsStatisticsPollingOn(this.isStatisticsPollingOn)
                .setBarrierCountLimit(new NonZeroUint16Type(this.barrierCountLimit))
                .setBarrierIntervalTimeoutLimit(new NonZeroUint32Type(this.barrierIntervalTimeoutLimit))
                .setEchoReplyTimeout(new NonZeroUint32Type(this.echoReplyTimeout))
                .setEnableFlowRemovedNotification(this.enableFlowRemovedNotification)
                .setSkipTableFeatures(this.skipTableFeatures)
                .setBasicTimerDelay(new NonZeroUint32Type(this.basicTimerDelay))
                .setMaximumTimerDelay(new NonZeroUint32Type(this.maximumTimerDelay))
                .setSwitchFeaturesMandatory(this.switchFeaturesMandatory)
                .setIsStatisticsRpcEnabled(this.isStatisticsRpcEnabled)
                .setUseSingleLayerSerialization(this.useSingleLayerSerialization)
                .setRpcRequestsQuota(new NonZeroUint16Type(this.rpcRequestsQuota))
                .setGlobalNotificationQuota(this.globalNotificationQuota)
                .setThreadPoolMinThreads(this.threadPoolMinThreads)
                .setThreadPoolMaxThreads(new NonZeroUint16Type(this.threadPoolMaxThreads))
                .setThreadPoolTimeout(this.threadPoolTimeout)
                .setMaximumTimerDelay(this.nonZeroUint32Type)
                .setIsQueueStatisticsPollingOn(true)
                .setIsFlowStatisticsPollingOn(true)
                .setIsTableStatisticsPollingOn(true)
                .setDeviceConnectionRateLimitPerMin(0)
                .setIsGroupStatisticsPollingOn(true)
                .setIsPortStatisticsPollingOn(true)
                .setIsMeterStatisticsPollingOn(true)
                .build();
    }

    /**
     * Create configuration settings need for initialize ForwardingRulesManager
     * @return instance of {@link ForwardingRulesManagerConfigBuilder}
     */
    public ForwardingRulesManagerConfigBuilder getFrmConfigBuilder(){
        final ForwardingRulesManagerConfigBuilder frmConfigBuilder = new ForwardingRulesManagerConfigBuilder();
        frmConfigBuilder.setDisableReconciliation(this.isFrmDisableReconciliation);
        frmConfigBuilder.setStaleMarkingEnabled(this.isFrmStaleMarkingEnabled);
        frmConfigBuilder.setReconciliationRetryCount(this.frmReconciliationRetryCount);
        frmConfigBuilder.setBundleBasedReconciliationEnabled(this.isFrmBundleBasedReconciliationEnabled);
        return frmConfigBuilder;
    }

    public boolean isStateful() {
        return isStateful;
    }

    public void setStateful(boolean ofpStartAsStateful) {
        isStateful = ofpStartAsStateful;
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

    public void setIsSkipTableFeatures(final boolean skipTableFeatures) {
        this.skipTableFeatures = skipTableFeatures;
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
}