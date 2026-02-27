/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.modules.northbound.netty.restconf.community.impl.util.NettyRestConfUtils;
import java.net.InetAddress;
import org.apache.shiro.web.env.WebEnvironment;
import org.opendaylight.aaa.shiro.web.env.AAAShiroWebEnvironment;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.netconf.transport.http.HttpServerStackConfiguration;
import org.opendaylight.netconf.transport.tcp.BootstrapFactory;
import org.opendaylight.restconf.api.query.PrettyPrintParam;
import org.opendaylight.restconf.server.AAAShiroPrincipalService;
import org.opendaylight.restconf.server.MessageEncoding;
import org.opendaylight.restconf.server.NettyEndpointConfiguration;
import org.opendaylight.restconf.server.PrincipalService;
import org.opendaylight.restconf.server.SimpleNettyEndpoint;
import org.opendaylight.restconf.server.jaxrs.JaxRsLocationProvider;
import org.opendaylight.restconf.server.mdsal.MdsalDatabindProvider;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfServer;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfStreamRegistry;
import org.opendaylight.restconf.server.spi.ErrorTagMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyRestConf extends AbstractLightyModule {
    private static final Logger LOG = LoggerFactory.getLogger(NettyRestConf.class);

    private final DOMDataBroker domDataBroker;
    private final DOMRpcService domRpcService;
    private final DOMNotificationService domNotificationService;
    private final DOMMountPointService domMountPointService;
    private final DOMActionService domActionService;
    private final DOMSchemaService domSchemaService;
    private final WebEnvironment webEnvironment;
    private final InetAddress inetAddress;
    private final int httpPort;
    private final String restconfServletContextPath;
    private final String groupName;
    private final int workThreads;

    private MdsalRestconfStreamRegistry mdsalRestconfStreamRegistry;
    private SimpleNettyEndpoint nettyEndpoint;
    private MdsalDatabindProvider databindProvider;
    private MdsalRestconfServer server;

    public NettyRestConf(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService,
            final DOMNotificationService domNotificationService, final DOMActionService domActionService,
            final DOMMountPointService domMountPointService, final DOMSchemaService domSchemaService,
            final InetAddress inetAddress, final int httpPort, final String restconfServletContextPath,
            final String groupName, final int workThreads, final WebEnvironment webEnvironment) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
        this.domNotificationService = domNotificationService;
        this.domActionService = domActionService;
        this.domMountPointService = domMountPointService;
        this.domSchemaService = domSchemaService;
        this.inetAddress = inetAddress;
        this.httpPort = httpPort;
        this.restconfServletContextPath = restconfServletContextPath;
        this.groupName = groupName;
        this.workThreads = workThreads;
        this.webEnvironment = webEnvironment;
    }

    @Override
    protected boolean initProcedure() {
        databindProvider = new MdsalDatabindProvider(domSchemaService);
        server = new MdsalRestconfServer(databindProvider, domDataBroker, domRpcService, domActionService,
            domMountPointService);

        final var tcpConfig = NettyRestConfUtils.getTcpConfig(
            IetfInetUtil.ipAddressFor(inetAddress), Uint16.valueOf(httpPort));
        final PrincipalService service = new AAAShiroPrincipalService((AAAShiroWebEnvironment) webEnvironment);
        final var serverStackGrouping = new HttpServerStackConfiguration(new TcpBuilder().setTcp(tcpConfig).build());
        final NettyEndpointConfiguration configuration = new NettyEndpointConfiguration(ErrorTagMapping.RFC8040,
            PrettyPrintParam.FALSE, Uint16.valueOf(0), Uint32.valueOf(10000), restconfServletContextPath,
            MessageEncoding.JSON, serverStackGrouping);
        final ClusterSingletonServiceProvider cssProvider = SingletonService -> {
            SingletonService.instantiateServiceInstance();
            return SingletonService::closeServiceInstance;
        };
        this.mdsalRestconfStreamRegistry = new MdsalRestconfStreamRegistry(domDataBroker, domNotificationService,
            domSchemaService, new JaxRsLocationProvider(), databindProvider, cssProvider);
        final var bootstrapFactory = new BootstrapFactory(groupName ,workThreads);
        nettyEndpoint = new SimpleNettyEndpoint(server, service, mdsalRestconfStreamRegistry,
            bootstrapFactory, configuration);

        return true;
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    protected boolean stopProcedure() {
        boolean stopSuccessful = true;
        if (nettyEndpoint != null) {
            try {
                nettyEndpoint.close();
            } catch (Exception e) {
                LOG.error("Failed to stop Netty endpoint!", e);
                stopSuccessful = false;
            }
        }
        if (mdsalRestconfStreamRegistry != null) {
            try {
                mdsalRestconfStreamRegistry.close();
            } catch (Exception e) {
                LOG.error("Failed to stop MdsalRestconfStreamRegistry!", e);
                stopSuccessful = false;
            }
        }
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                LOG.error("Failed to stop MdsalRestconfServer!", e);
                stopSuccessful = false;
            }
        }
        if (databindProvider != null) {
            try {
                databindProvider.close();
            } catch (Exception e) {
                LOG.error("Failed to stop MdsalDatabindProvider!", e);
                stopSuccessful = false;
            }
        }
        LOG.info("Netty endpoint stopped successfully.");
        return stopSuccessful;
    }
}
