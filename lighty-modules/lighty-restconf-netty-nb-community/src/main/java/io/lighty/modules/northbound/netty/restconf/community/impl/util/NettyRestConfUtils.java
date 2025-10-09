/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.northbound.netty.restconf.community.impl.config.NettyRestConfConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.api.IDMStoreException;
import org.opendaylight.aaa.api.IIDMStore;
import org.opendaylight.aaa.api.StoreBuilder;
import org.opendaylight.aaa.datastore.h2.H2Store;
import org.opendaylight.aaa.datastore.h2.IdmLightConfig;
import org.opendaylight.aaa.datastore.h2.IdmLightConfigBuilder;
import org.opendaylight.aaa.datastore.h2.IdmLightSimpleConnectionProvider;
import org.opendaylight.aaa.impl.password.service.DefaultPasswordHashService;
import org.opendaylight.aaa.shiro.idm.IdmLightProxy;
import org.opendaylight.aaa.shiro.realm.BasicRealmAuthProvider;
import org.opendaylight.aaa.shiro.web.env.AAAWebEnvironment;
import org.opendaylight.aaa.tokenauthrealm.auth.AuthenticationManager;
import org.opendaylight.aaa.web.servlet.jersey2.JerseyServletSupport;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.restconf.api.query.PrettyPrintParam;
import org.opendaylight.restconf.server.jaxrs.JaxRsEndpointConfiguration;
import org.opendaylight.restconf.server.spi.ErrorTagMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.tcp.Tcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.tcp.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.tcp.tcp.TcpServerParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.tcp.server.rev241010.tcp.server.grouping.LocalBindBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.password.service.config.rev170619.PasswordServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.password.service.config.rev170619.PasswordServiceConfigBuilder;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NettyRestConfUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NettyRestConfUtils.class);

    public static final String RESTCONF_CONFIG_ROOT_ELEMENT_NAME = "restconf";
    public static final Set<YangModuleInfo> YANG_MODELS = Set.of(
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev190104
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.restconf.rev170126
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.restconf.monitoring.rev170126
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.sal.restconf.event.subscription.rev231103
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.yang.patch.rev170222
            .YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.urn.opendaylight.device.notification.rev240218
            .YangModuleInfoImpl.getInstance());
    public static final int MAXIMUM_FRAGMENT_LENGTH = 0;
    public static final int IDLE_TIMEOUT = 30000;
    public static final int HEARTBEAT_INTERVAL = 10000;

    private NettyRestConfUtils() {
        throw new UnsupportedOperationException();
    }

    public static AAAWebEnvironment getAaaWebEnvironment(final DataBroker dataBroker,
        final RpcProviderService rpcProviderService, @Nullable AAAConfiguration aaaConfiguration) {
        if (aaaConfiguration == null) {
            aaaConfiguration = AAAConfigUtils.createDefaultAAAConfiguration();
            aaaConfiguration.setCertificateManager(
                CertificateManagerConfig.getDefault(dataBroker,
                    rpcProviderService));
        }

        final IdmLightConfig config = new IdmLightConfigBuilder()
            .dbDirectory(aaaConfiguration.getDbPath())
            .dbUser(aaaConfiguration.getUsername())
            .dbPwd(aaaConfiguration.getDbPassword()).build();
        final PasswordServiceConfig passwordServiceConfig = new PasswordServiceConfigBuilder().setAlgorithm(
            "SHA-512").setIterations(20000).build();
        final var defaultPasswordHashService = new DefaultPasswordHashService(passwordServiceConfig);
        final var iidmStore = new H2Store(new IdmLightSimpleConnectionProvider(config), defaultPasswordHashService);
        final IdmLightProxy idmLightProxy = new IdmLightProxy(iidmStore, defaultPasswordHashService);

        try {
            final StoreBuilder storeBuilder = new StoreBuilder(iidmStore);
            final String domain = storeBuilder.initDomainAndRolesWithoutUsers(IIDMStore.DEFAULT_DOMAIN);
            if (domain != null) {
                storeBuilder.createUser(domain, aaaConfiguration.getUsername(), aaaConfiguration.getPassword(), true);
            }

        } catch (IDMStoreException e) {
            throw new RuntimeException("Failed to create user: ", e);
        }

        return new AAAWebEnvironment(
            aaaConfiguration.getShiroConf(),
            dataBroker,
            aaaConfiguration.getCertificateManager(),
            new AuthenticationManager(),
            new BasicRealmAuthProvider(idmLightProxy, iidmStore),
            new DefaultPasswordHashService(passwordServiceConfig),
            new JerseyServletSupport());
    }

    public static Tcp getTcpConfig(final IpAddress address, final Uint16 httpPort) {
        return new TcpBuilder().setTcpServerParameters(
            new TcpServerParametersBuilder()
                .setLocalBind(BindingMap.of(new LocalBindBuilder()
                    .setLocalAddress(address)
                    .setLocalPort(new PortNumber(httpPort))
                    .build()))
                .build()).build();

    }

    /**
     * Load restconf configuration from InputStream containing JSON data.
     *
     * @param jsonConfigInputStream InputStream containing RestConf configuration data in JSON format.
     * @return Object representation of configuration data.
     * @throws ConfigurationException In case InputStream does not contain valid JSON data or cannot bind Json tree
     *                                to type.
     */
    public static NettyRestConfConfiguration getNettyRestConfConfiguration(
        final InputStream jsonConfigInputStream) throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(RESTCONF_CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", RESTCONF_CONFIG_ROOT_ELEMENT_NAME);
            return new NettyRestConfConfiguration();
        }
        final JsonNode restconfNode = configNode.path(RESTCONF_CONFIG_ROOT_ELEMENT_NAME);
        NettyRestConfConfiguration restconfConfiguration = null;
        try {
            restconfConfiguration = mapper.treeToValue(restconfNode, NettyRestConfConfiguration.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                NettyRestConfConfiguration.class), e);
        }

        return restconfConfiguration;
    }

    /**
     * Load restconf configuration from InputStream containing JSON data and use lightyServices to
     * get references to necessary Lighty services.
     *
     * @param jsonConfigInputStream InputStream containing RestConf configuration data in JSON format.
     * @param lightyServices        This object instace contains references to initialized Lighty services required for
     *                              RestConf.
     * @return Object representation of configuration data.
     * @throws ConfigurationException In case InputStream does not contain valid JSON data or cannot bind Json tree
     *                                to type.
     */
    public static NettyRestConfConfiguration getNettyRestConfConfiguration(final InputStream jsonConfigInputStream,
        final LightyServices lightyServices) throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(RESTCONF_CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", RESTCONF_CONFIG_ROOT_ELEMENT_NAME);
            return getDefaultNettyRestConfConfiguration(lightyServices);
        }
        final JsonNode restconfNode = configNode.path(RESTCONF_CONFIG_ROOT_ELEMENT_NAME);

        NettyRestConfConfiguration restconfConfiguration = null;
        try {
            restconfConfiguration = mapper.treeToValue(restconfNode, NettyRestConfConfiguration.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                NettyRestConfConfiguration.class), e);
        }
        restconfConfiguration.setDomDataBroker(lightyServices.getClusteredDOMDataBroker());
        restconfConfiguration.setSchemaService(lightyServices.getDOMSchemaService());
        restconfConfiguration.setDomRpcService(lightyServices.getDOMRpcService());
        restconfConfiguration.setDomNotificationService(lightyServices.getDOMNotificationService());
        restconfConfiguration.setDomMountPointService(lightyServices.getDOMMountPointService());
        restconfConfiguration.setDomSchemaService(lightyServices.getDOMSchemaService());

        return restconfConfiguration;
    }

    /**
     * Copy existing RestConf configuration and use provided lightyServices
     * to populate references to necessary Lighty services.
     *
     * @param restConfConfiguration Object representation of configuration data.
     * @param lightyServices        This object instace contains references to initialized Lighty services required for
     *                              RestConf.
     * @return Object representation of configuration data.
     */
    public static NettyRestConfConfiguration getNettyRestConfConfiguration(
        final NettyRestConfConfiguration restConfConfiguration, final LightyServices lightyServices) {
        final NettyRestConfConfiguration config = new NettyRestConfConfiguration(restConfConfiguration);
        config.setDomDataBroker(lightyServices.getClusteredDOMDataBroker());
        config.setSchemaService(lightyServices.getDOMSchemaService());
        config.setDomRpcService(lightyServices.getDOMRpcService());
        config.setDomActionService(lightyServices.getDOMActionService());
        config.setDomNotificationService(lightyServices.getDOMNotificationService());
        config.setDomMountPointService(lightyServices.getDOMMountPointService());
        config.setDomSchemaService(lightyServices.getDOMSchemaService());
        return config;
    }

    /**
     * Get default RestConf configuration using provided Lighty services.
     *
     * @param lightyServices This object instace contains references to initialized Lighty services required for
     *                       RestConf.
     * @return Object representation of configuration data.
     */
    public static NettyRestConfConfiguration getDefaultNettyRestConfConfiguration(final LightyServices lightyServices) {
        return new NettyRestConfConfiguration(
            lightyServices.getClusteredDOMDataBroker(), lightyServices.getDOMSchemaService(),
            lightyServices.getDOMRpcService(), lightyServices.getDOMActionService(),
            lightyServices.getDOMNotificationService(), lightyServices.getDOMMountPointService(),
            lightyServices.getDOMSchemaService());
    }

    /**
     * Get default RestConf configuration, Lighty services are not populated in this configuration.
     *
     * @return Object representation of configuration data.
     */
    public static NettyRestConfConfiguration getDefaultNettyRestConfConfiguration() {
        return new NettyRestConfConfiguration();
    }

    public static JaxRsEndpointConfiguration getStreamsConfiguration(final String restconfPath) {
        return new JaxRsEndpointConfiguration(ErrorTagMapping.RFC8040, PrettyPrintParam.FALSE,
            Uint16.valueOf(MAXIMUM_FRAGMENT_LENGTH), Uint32.valueOf(HEARTBEAT_INTERVAL), restconfPath);
    }
}
