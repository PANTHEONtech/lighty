package io.lighty.modules.northbound.netty.restconf.community.impl.util;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.aaa.config.CertificateManagerConfig;
import io.lighty.aaa.util.AAAConfigUtils;
import org.eclipse.jdt.annotation.Nullable;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.tcp.Tcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.tcp.tcp.TcpServerParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.http.server.rev240208.http.server.stack.grouping.transport.tcp.TcpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.tcp.server.rev241010.tcp.server.grouping.LocalBindBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.password.service.config.rev170619.PasswordServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.password.service.config.rev170619.PasswordServiceConfigBuilder;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;

public class NettyRestConfUtils {

    public static AAAWebEnvironment getAaaWebEnvironment(final DataBroker dataBroker, final RpcProviderService rpcProviderService, @Nullable AAAConfiguration aaaConfiguration) {
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
}
