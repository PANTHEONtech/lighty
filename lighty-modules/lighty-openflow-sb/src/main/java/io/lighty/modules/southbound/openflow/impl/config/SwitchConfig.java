/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.openflowjava.protocol.api.connection.OpenflowDiagStatusProvider;
import org.opendaylight.openflowjava.protocol.impl.core.DefaultOpenflowDiagStatusProvider;
import org.opendaylight.openflowjava.protocol.impl.core.SwitchConnectionProviderFactoryImpl;
import org.opendaylight.openflowjava.protocol.spi.connection.SwitchConnectionProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.KeystoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.TransportProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow._switch.connection.config.rev160506.SwitchConnectionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow._switch.connection.config.rev160506.SwitchConnectionConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow._switch.connection.config.rev160506._switch.connection.config.TlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.OpenflowProviderConfig;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class SwitchConfig {

    private static final String LEGACY_INSTANCE_NAME = "openflow-switch-connection-provider-legacy-impl";
    private static final int LEGACY_PORT = 6633;

    private final SwitchConnectionProviderFactoryImpl factory = new SwitchConnectionProviderFactoryImpl();

    @JsonIgnore
    private final SwitchConnectionConfig defaultSwitch;
    @JsonIgnore
    private final SwitchConnectionConfig legacySwitch;

    private String instanceName = "openflow-switch-connection-provider-default-impl";
    private int port = 0;
    private int transportProtocol = 0;
    private String address;
    private boolean useBarrier = false;
    private Long switchIdleTimeout = 0L;
    private String keystore;
    private int keystoreType = 0;
    private int keystorePathType = 0;
    private String keystorePassword;
    private String truststore;
    private int truststoreType = 0;
    private int truststorePathType = 0;
    private String truststorePassword;
    private String certificatePassword;
    private int channelOutboundQueueSize = 0;

    protected SwitchConfig() {
        this.defaultSwitch =
                new SwitchConnectionConfigBuilder()
                        .setInstanceName(this.instanceName)
                        .setPort(Uint16.valueOf(this.port))
                        .setTransportProtocol(TransportProtocol.forValue(this.transportProtocol))
                        .setSwitchIdleTimeout(Uint32.valueOf(this.switchIdleTimeout))
                        .setUseBarrier(this.useBarrier)
                        .setChannelOutboundQueueSize(Uint16.valueOf(this.channelOutboundQueueSize))
                        .setTls(
                                new TlsBuilder()
                                        .setKeystoreType(KeystoreType.forValue(this.keystoreType))
                                        .setKeystorePathType(PathType.forValue(this.keystorePathType))
                                        .setTruststoreType(KeystoreType.forValue(this.truststoreType))
                                        .setTruststorePathType(PathType.forValue(this.truststorePathType))
                                        .build()
                        ).build();
        this.legacySwitch =
                new SwitchConnectionConfigBuilder()
                        .setInstanceName(LEGACY_INSTANCE_NAME)
                        .setPort(Uint16.valueOf(LEGACY_PORT))
                        .setTransportProtocol(TransportProtocol.forValue(this.transportProtocol))
                        .setSwitchIdleTimeout(Uint32.valueOf(this.switchIdleTimeout))
                        .setUseBarrier(this.useBarrier)
                        .setChannelOutboundQueueSize(Uint16.valueOf(this.channelOutboundQueueSize))
                        .setTls(
                                new TlsBuilder()
                                        .setKeystoreType(KeystoreType.forValue(this.keystoreType))
                                        .setKeystorePathType(PathType.forValue(this.keystorePathType))
                                        .setTruststoreType(KeystoreType.forValue(this.truststoreType))
                                        .setTruststorePathType(PathType.forValue(this.truststorePathType))
                                        .build()
                        ).build();
    }

    public List<SwitchConnectionProvider> getDefaultProviders(DiagStatusService diagStatusService) {
        final List<SwitchConnectionProvider> switchConnectionProviderList = new ArrayList<>();
        final OpenflowDiagStatusProvider openflowDiagStatusProvider
                = new DefaultOpenflowDiagStatusProvider(diagStatusService);

        switchConnectionProviderList.add(new SwitchConnectionProviderFactoryImpl()
                .newInstance(this.defaultSwitch, openflowDiagStatusProvider));
        switchConnectionProviderList.add(new SwitchConnectionProviderFactoryImpl()
                .newInstance(this.legacySwitch, openflowDiagStatusProvider));
        return switchConnectionProviderList;
    }

    public List<SwitchConnectionProvider> getProviders(DiagStatusService diagStatusService,
                                                       OpenflowProviderConfig config, ExecutorService executorService) {

        final SwitchConnectionConfig tmpDefaultSwitch =
                new SwitchConnectionConfigBuilder()
                        .setInstanceName(this.instanceName)
                        .setPort(Uint16.valueOf(this.port))
                        .setTransportProtocol(TransportProtocol.forValue(this.transportProtocol))
                        .setSwitchIdleTimeout(Uint32.valueOf(this.switchIdleTimeout))
                        .setUseBarrier(this.useBarrier)
                        .setChannelOutboundQueueSize(Uint16.valueOf(this.channelOutboundQueueSize))
                        .setTls(
                                new TlsBuilder()
                                        .setKeystore(this.keystore)
                                        .setKeystoreType(KeystoreType.forValue(this.keystoreType))
                                        .setKeystorePathType(PathType.forValue(this.keystorePathType))
                                        .setKeystorePassword(this.keystorePassword)
                                        .setTruststore(this.truststore)
                                        .setTruststoreType(KeystoreType.forValue(this.truststoreType))
                                        .setTruststorePathType(PathType.forValue(this.truststorePathType))
                                        .setTruststorePassword(this.truststorePassword)
                                        .setCertificatePassword(this.certificatePassword)
                                        .build()
                        ).setGroupAddModEnabled(true).build();

        final SwitchConnectionConfig tmpLegacySwitch =
                new SwitchConnectionConfigBuilder()
                        .setInstanceName(LEGACY_INSTANCE_NAME)
                        .setPort(Uint16.valueOf(LEGACY_PORT))
                        .setTransportProtocol(TransportProtocol.forValue(this.transportProtocol))
                        .setSwitchIdleTimeout(Uint32.valueOf(this.switchIdleTimeout))
                        .setUseBarrier(this.useBarrier)
                        .setChannelOutboundQueueSize(Uint16.valueOf(this.channelOutboundQueueSize))
                        .setTls(
                                new TlsBuilder()
                                        .setKeystore(this.keystore)
                                        .setKeystoreType(KeystoreType.forValue(this.keystoreType))
                                        .setKeystorePathType(PathType.forValue(this.keystorePathType))
                                        .setKeystorePassword(this.keystorePassword)
                                        .setTruststore(this.truststore)
                                        .setTruststoreType(KeystoreType.forValue(this.truststoreType))
                                        .setTruststorePathType(PathType.forValue(this.truststorePathType))
                                        .setTruststorePassword(this.truststorePassword)
                                        .setCertificatePassword(this.certificatePassword)
                                        .build()
                        ).setGroupAddModEnabled(true).build();

        final List<SwitchConnectionProvider> switchConnectionProviderList = new ArrayList<>();
        final OpenflowDiagStatusProvider openflowDiagStatusProvider
                = new DefaultOpenflowDiagStatusProvider(diagStatusService);

        //add default switch connection provider
        SwitchConnectionProvider defaultSwitchConnectionProvider = factory
                .newInstance(tmpDefaultSwitch, openflowDiagStatusProvider);
        switchConnectionProviderList.add(defaultSwitchConnectionProvider);

        //add legacy switch connection provider
        SwitchConnectionProvider legacySwitchConnectionProvider = factory
                .newInstance(tmpLegacySwitch, openflowDiagStatusProvider);
        switchConnectionProviderList.add(legacySwitchConnectionProvider);
        return switchConnectionProviderList;
    }

    public int getChannelOutboundQueueSize() {
        return this.channelOutboundQueueSize;
    }

    public void setChannelOutboundQueueSize(final int channelOutboundQueueSize) {
        this.channelOutboundQueueSize = channelOutboundQueueSize;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public void setInstanceName(final String instanceName) {
        this.instanceName = instanceName;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public int getTransportProtocol() {
        return this.transportProtocol;
    }

    public void setTransportProtocol(final int transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public boolean isUseBarrier() {
        return this.useBarrier;
    }

    public void setUseBarrier(final boolean useBarrier) {
        this.useBarrier = useBarrier;
    }

    public Long getSwitchIdleTimeout() {
        return this.switchIdleTimeout;
    }

    public void setSwitchIdleTimeout(final Long switchIdleTimeout) {
        this.switchIdleTimeout = switchIdleTimeout;
    }

    public String getKeystore() {
        return this.keystore;
    }

    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }

    public int getKeystoreType() {
        return this.keystoreType;
    }

    public void setKeystoreType(final int keystoreType) {
        this.keystoreType = keystoreType;
    }

    public int getKeystorePathType() {
        return this.keystorePathType;
    }

    public void setKeystorePathType(final int keystorePathType) {
        this.keystorePathType = keystorePathType;
    }

    public String getKeystorePassword() {
        return this.keystorePassword;
    }

    public void setKeystorePassword(final String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getTruststore() {
        return this.truststore;
    }

    public void setTruststore(final String truststore) {
        this.truststore = truststore;
    }

    public int getTruststoreType() {
        return this.truststoreType;
    }

    public void setTruststoreType(final int truststoreType) {
        this.truststoreType = truststoreType;
    }

    public int getTruststorePathType() {
        return this.truststorePathType;
    }

    public void setTruststorePathType(final int truststorePathType) {
        this.truststorePathType = truststorePathType;
    }

    public String getTruststorePassword() {
        return this.truststorePassword;
    }

    public void setTruststorePassword(final String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getCertificatePassword() {
        return this.certificatePassword;
    }

    public void setCertificatePassword(final String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }
}
