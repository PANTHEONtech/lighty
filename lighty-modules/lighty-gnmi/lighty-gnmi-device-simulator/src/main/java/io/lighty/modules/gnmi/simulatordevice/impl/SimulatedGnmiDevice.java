/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.impl;

import com.google.gson.Gson;
import gnmi.Gnmi;
import io.grpc.Server;
import io.grpc.netty.InternalProtocolNegotiators;
import io.grpc.netty.NettyServerBuilder;
import io.lighty.modules.gnmi.simulatordevice.gnmi.AuthenticationInterceptor;
import io.lighty.modules.gnmi.simulatordevice.gnmi.GnmiService;
import io.lighty.modules.gnmi.simulatordevice.gnoi.GnoiCertService;
import io.lighty.modules.gnmi.simulatordevice.gnoi.GnoiFileService;
import io.lighty.modules.gnmi.simulatordevice.gnoi.GnoiOSService;
import io.lighty.modules.gnmi.simulatordevice.gnoi.GnoiSonicService;
import io.lighty.modules.gnmi.simulatordevice.gnoi.GnoiSystemService;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.utils.FileUtils;
import io.lighty.modules.gnmi.simulatordevice.utils.UsernamePasswordAuth;
import io.lighty.modules.gnmi.simulatordevice.yang.YangDataService;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.StringUtil;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SimulatedGnmiDevice {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedGnmiDevice.class);

    private static final String DEFAULT_SERVER_CRT_FILE_PATH = "certs/server.crt";
    private static final String DEFAULT_SERVER_KEY_FILE_PATH = "certs/server.key";

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final String host;
    private final String certificatePath;
    private final String keyPath;
    private final String yangsPath;
    private final Set<YangModuleInfo> modulesInfoSet;
    private final String initialConfigDataPath;
    private final String initialStateDataPath;
    private final UsernamePasswordAuth usernamePasswordAuth;
    private final int port;
    private final int maxConnections;
    private final boolean plaintext;
    private final Gson gson;
    private final EnumSet<Gnmi.Encoding> supportedEncodings;
    private Server server;

    private GnoiSystemService gnoiSystemService;
    private GnoiCertService gnoiCertService;
    private GnoiFileService gnoiFileService;
    private GnoiOSService gnoiOSService;
    private GnoiSonicService gnoiSonicService;

    private GnmiService gnmiService;

    private EffectiveModelContext schemaContext;
    private YangDataService dataService;


    public SimulatedGnmiDevice(final SimulatedGnmiDeviceGroupHolder groups, final SimulatedGnmiDevicePathsHolder paths,
                               final SimulatedGnmiDeviceConnectionInfoHolder connectionInfo,
                               final UsernamePasswordAuth usernamePasswordAuth, final boolean plaintext,
                               final Gson gson, final EnumSet<Gnmi.Encoding> supportedEncodings,
                               final Set<YangModuleInfo> modulesInfoSet) {
        this.bossGroup = Objects.requireNonNullElseGet(groups.bossGroup, () -> new NioEventLoopGroup(1));
        this.workerGroup = Objects.requireNonNullElseGet(groups.workerGroup, NioEventLoopGroup::new);
        this.yangsPath = Objects.requireNonNull(paths.yangsPath, "Path to directory of yang files form which schema"
                + " will be created is needed!");
        this.modulesInfoSet = modulesInfoSet;
        this.host = connectionInfo.host;
        this.port = connectionInfo.port;
        this.maxConnections = connectionInfo.maxConnections;
        this.certificatePath = paths.certificatePath;
        this.keyPath = paths.keyPath;
        this.initialConfigDataPath = paths.initialConfigDataPath;
        this.initialStateDataPath = paths.initialStateDataPath;
        this.usernamePasswordAuth = usernamePasswordAuth;
        this.plaintext = plaintext;
        this.gson = gson;
        this.supportedEncodings = supportedEncodings;
    }

    public void start() throws IOException, EffectiveModelContextBuilderException {
        final NettyServerBuilder serverBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
                .bossEventLoopGroup(bossGroup)
                .workerEventLoopGroup(workerGroup)
                .channelType(NioServerSocketChannel.class)
                .withChildOption(ChannelOption.SO_KEEPALIVE, true)
                .withChildOption(ChannelOption.SO_BACKLOG, maxConnections);

        if (usernamePasswordAuth != null && usernamePasswordAuth.isNotEmpty()) {
            serverBuilder.intercept(new AuthenticationInterceptor(this.usernamePasswordAuth));
        }

        if (plaintext) {
            serverBuilder.protocolNegotiator(InternalProtocolNegotiators.plaintext());
        }

        if (StringUtil.isNullOrEmpty(certificatePath) || StringUtil.isNullOrEmpty(keyPath)) {
            // use default certificates
            serverBuilder.useTransportSecurity(
                    FileUtils.getResourceAsStream(DEFAULT_SERVER_CRT_FILE_PATH),
                    FileUtils.getResourceAsStream(DEFAULT_SERVER_KEY_FILE_PATH)
            );
            LOG.info("Combination of server certificate and key not provided, using default ones.");
        } else {
            serverBuilder.useTransportSecurity(new File(certificatePath), new File(keyPath));
        }

        // Initialize schema context from yang models
        schemaContext = new EffectiveModelContextBuilder()
                .addYangModulesPath(yangsPath)
                .addYangModulesInfo(modulesInfoSet)
                .build();

        // Initialize data service
        dataService = new YangDataService(schemaContext, initialConfigDataPath, initialStateDataPath);

        // Route gNMI calls towards gNMI service facade
        gnmiService = new GnmiService(schemaContext, dataService, gson, supportedEncodings);
        serverBuilder.addService(gnmiService);

        gnoiSystemService = new GnoiSystemService();
        serverBuilder.addService(gnoiSystemService);

        gnoiCertService = new GnoiCertService();
        serverBuilder.addService(gnoiCertService);

        gnoiFileService = new GnoiFileService();
        serverBuilder.addService(gnoiFileService);

        gnoiOSService = new GnoiOSService();
        serverBuilder.addService(gnoiOSService);

        gnoiSonicService = new GnoiSonicService();
        serverBuilder.addService(gnoiSonicService);

        // build & start
        LOG.info("Starting gNMI device simulator on {}:{} ...", host, port);
        this.server = serverBuilder.build();
        this.server.start();
        LOG.info("gNMI device simulator is up and running");
    }

    public void stop() {
        LOG.debug("Shutting down simulator...");
        if (!server.isShutdown()) {
            try {
                server.shutdown();
                server.awaitTermination();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SimulatedGnmiDeviceException("Shutdown interrupted", e);
            }
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public GnoiSystemService getGnoiSystemService() {
        return gnoiSystemService;
    }

    public GnoiCertService getGnoiCertService() {
        return gnoiCertService;
    }

    public GnoiFileService getGnoiFileService() {
        return gnoiFileService;
    }

    public GnoiOSService getGnoiOSService() {
        return gnoiOSService;
    }

    public GnoiSonicService getGnoiSonicService() {
        return gnoiSonicService;
    }

    public GnmiService getGnmiService() {
        return gnmiService;
    }

    public YangDataService getDataService() {
        return dataService;
    }

    public EffectiveModelContext getSchemaContext() {
        return schemaContext;
    }

    protected static final class SimulatedGnmiDeviceGroupHolder {

        private final EventLoopGroup bossGroup;
        private final EventLoopGroup workerGroup;

        public SimulatedGnmiDeviceGroupHolder(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
            this.bossGroup = bossGroup;
            this.workerGroup = workerGroup;
        }

    }

    protected static final class SimulatedGnmiDeviceConnectionInfoHolder {

        private final String host;
        private final int port;
        private final int maxConnections;

        public SimulatedGnmiDeviceConnectionInfoHolder(final String host, final int port, final int maxConnections) {
            this.host = host;
            this.port = port;
            this.maxConnections = maxConnections;
        }

    }

    protected static final class SimulatedGnmiDevicePathsHolder {

        private final String certificatePath;
        private final String keyPath;
        private final String yangsPath;
        private final String initialConfigDataPath;
        private final String initialStateDataPath;

        public SimulatedGnmiDevicePathsHolder(final String certificatePath, final String keyPath,
                                              final String yangsPath, final String initialConfigDataPath,
                                              final String initialStateDataPath) {
            this.yangsPath = yangsPath;
            this.certificatePath = certificatePath;
            this.keyPath = keyPath;
            this.initialConfigDataPath = initialConfigDataPath;
            this.initialStateDataPath = initialStateDataPath;
        }
    }

    static final class SimulatedGnmiDeviceException extends RuntimeException {
        SimulatedGnmiDeviceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
