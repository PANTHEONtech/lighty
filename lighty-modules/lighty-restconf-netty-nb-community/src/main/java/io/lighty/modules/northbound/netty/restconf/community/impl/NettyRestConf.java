package io.lighty.modules.northbound.netty.restconf.community.impl;

import io.lighty.core.controller.api.AbstractLightyModule;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import org.apache.shiro.web.env.WebEnvironment;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.shiro.web.env.AAAShiroWebEnvironment;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.netconf.transport.http.HttpServerStackConfiguration;
import org.opendaylight.netconf.transport.tcp.BootstrapFactory;
import org.opendaylight.restconf.api.query.PrettyPrintParam;
import org.opendaylight.restconf.server.AAAShiroPrincipalService;
import org.opendaylight.restconf.server.MessageEncoding;
import org.opendaylight.restconf.server.NettyEndpointConfiguration;
import org.opendaylight.restconf.server.OSGiNettyEndpoint;
import org.opendaylight.restconf.server.PrincipalService;
import org.opendaylight.restconf.server.jaxrs.JaxRsLocationProvider;
import org.opendaylight.restconf.server.mdsal.MdsalDatabindProvider;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfServer;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfStreamRegistry;
import org.opendaylight.restconf.server.spi.ErrorTagMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana.crypt.hash.rev140806.CryptHash;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.grouping.ClientAuthentication;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.grouping.ClientAuthenticationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.grouping.client.authentication.users.User;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.grouping.client.authentication.users.user.auth.type.basic.basic.PasswordBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.tcp.server.rev241010.tcp.server.grouping.LocalBindBuilder;
import org.opendaylight.yangtools.binding.util.BindingMap;
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
    private MdsalRestconfStreamRegistry mdsalRestconfStreamRegistry;

    public NettyRestConf(final DOMDataBroker domDataBroker, final DOMRpcService domRpcService,
        final DOMNotificationService domNotificationService,
        final DOMActionService domActionService,
        final DOMMountPointService domMountPointService,
        final DOMSchemaService domSchemaService,
        final WebEnvironment webEnvironment) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
        this.domNotificationService = domNotificationService;
        this.domActionService = domActionService;
        this.domMountPointService = domMountPointService;
        this.domSchemaService = domSchemaService;
        this.webEnvironment = webEnvironment;}

    @Override
    protected boolean initProcedure() throws InterruptedException, ServletException {
        final MdsalDatabindProvider databindProvider = new MdsalDatabindProvider(domSchemaService);
        final MdsalRestconfServer server = new MdsalRestconfServer(databindProvider, domDataBroker, domRpcService,
            domActionService, domMountPointService);

        final var tcpParams = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
            .http.server.stack.grouping.transport.tcp.tcp.TcpServerParametersBuilder()
            .setLocalBind(BindingMap.of(new LocalBindBuilder()
                .setLocalAddress(IetfInetUtil.ipAddressFor("127.0.0.1"))
                .setLocalPort(new PortNumber(Uint16.valueOf(8888)))
                .build()))
            .build();

        final var httpParams = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
            .http.server.stack.grouping.transport.tcp.tcp.HttpServerParametersBuilder()
            .setClientAuthentication(clientAuthentication(null)).build();

        final var tcp = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
            .http.server.stack.grouping.transport.tcp.TcpBuilder()
            .setTcpServerParameters(tcpParams).setHttpServerParameters(httpParams).build();

        final PrincipalService service = new AAAShiroPrincipalService((AAAShiroWebEnvironment) webEnvironment);
        final var serverStackGrouping = new HttpServerStackConfiguration(new TcpBuilder().setTcp(tcp).build());
        final BootstrapFactory factory = new BootstrapFactory("lighty-restconf-nb-worker", 1);
        final NettyEndpointConfiguration configuration = new NettyEndpointConfiguration(ErrorTagMapping.RFC8040,
            PrettyPrintParam.FALSE, Uint16.valueOf(0), Uint32.valueOf(10000), "restconf",
            MessageEncoding.JSON, serverStackGrouping);
        this.mdsalRestconfStreamRegistry = new MdsalRestconfStreamRegistry(domDataBroker, domNotificationService,
            domSchemaService, new JaxRsLocationProvider(), databindProvider);

        final OSGiNettyEndpoint endpoint = new OSGiNettyEndpoint(server, service, mdsalRestconfStreamRegistry,
            OSGiNettyEndpoint.props(factory, configuration));
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        return false;
    }

    private static @Nullable ClientAuthentication clientAuthentication(
        final @Nullable Map<String, String> userCryptHashMap) {
        if (userCryptHashMap == null || userCryptHashMap.isEmpty()) {
            return null;
        }
        final var userMap = userCryptHashMap.entrySet().stream()
            .map(entry -> new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
                .http.server.grouping.client.authentication.users.UserBuilder()
                .setUserId(entry.getKey())
                .setAuthType(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
                        .http.server.grouping.client.authentication.users.user.auth.type.BasicBuilder().setBasic(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
                            .http.server.grouping.client.authentication.users.user.auth.type.basic.BasicBuilder()
                            .setUsername(entry.getKey())
                            .setPassword(new PasswordBuilder()
                                .setHashedPassword(new CryptHash(entry.getValue())).build()).build()
                    ).build()).build())
            .collect(Collectors.toMap(User::key, Function.identity()));
        return new ClientAuthenticationBuilder()
            .setUsers(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208
                .http.server.grouping.client.authentication.UsersBuilder().setUser(userMap).build()).build();
    }
}
