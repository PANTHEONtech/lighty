/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.server.LightyServerBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.internal.guava.Preconditions;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.aaa.api.AuthenticationService;
import org.opendaylight.aaa.api.ClaimCache;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.IDMStoreException;
import org.opendaylight.aaa.api.IIDMStore;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.opendaylight.aaa.api.StoreBuilder;
import org.opendaylight.aaa.api.password.service.PasswordHashService;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.aaa.datastore.h2.H2Store;
import org.opendaylight.aaa.datastore.h2.IdmLightConfig;
import org.opendaylight.aaa.datastore.h2.IdmLightConfigBuilder;
import org.opendaylight.aaa.datastore.h2.IdmLightSimpleConnectionProvider;
import org.opendaylight.aaa.filterchain.configuration.impl.CustomFilterAdapterConfigurationImpl;
import org.opendaylight.aaa.filterchain.filters.CustomFilterAdapter;
import org.opendaylight.aaa.impl.password.service.DefaultPasswordHashService;
import org.opendaylight.aaa.shiro.filters.AAAShiroFilter;
import org.opendaylight.aaa.shiro.idm.IdmLightApplication;
import org.opendaylight.aaa.shiro.idm.IdmLightProxy;
import org.opendaylight.aaa.shiro.moon.MoonTokenEndpoint;
import org.opendaylight.aaa.shiro.web.env.AAAWebEnvironment;
import org.opendaylight.aaa.tokenauthrealm.auth.AuthenticationManager;
import org.opendaylight.aaa.tokenauthrealm.auth.HttpBasicAuth;
import org.opendaylight.aaa.tokenauthrealm.auth.TokenAuthenticators;
import org.opendaylight.aaa.web.servlet.jersey2.JerseyServletSupport;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.password.service.config.rev170619.PasswordServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.password.service.config.rev170619.PasswordServiceConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AAALightyShiroProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AAALightyShiroProvider.class);

    private static AAALightyShiroProvider INSTANCE;

    private final List<Handler> handlers;
    private final DataBroker dataBroker;
    private final ICertificateManager certificateManager;
    private final ShiroConfiguration shiroConfiguration;
    private final AuthenticationService authenticationService;
    private final DefaultPasswordHashService defaultPasswordHashService;
    private TokenAuthenticators tokenAuthenticators;
    private CredentialAuth<PasswordCredentials> credentialAuth;
    private ClaimCache claimCache;
    private PasswordHashService passwordHashService;
    private IIDMStore iidmStore;

    private AAAWebEnvironment aaaWebEnvironment;

    private AAALightyShiroProvider(final DataBroker dataBroker,
                                   final AAAConfiguration aaaConfiguration,
                                   final CredentialAuth<PasswordCredentials> credentialAuth,
                                   final LightyServerBuilder server) {
        this.dataBroker = dataBroker;
        this.certificateManager = aaaConfiguration.getCertificateManager();
        this.credentialAuth = credentialAuth;
        this.shiroConfiguration = aaaConfiguration.getShiroConf();
        this.handlers = new ArrayList<>();
        this.authenticationService = new AuthenticationManager();
        final DatastoreConfig datastoreConfig = aaaConfiguration.getDatastoreConf();

        if (datastoreConfig != null && datastoreConfig.getStore().equals(DatastoreConfig.Store.H2DataStore)) {
            final IdmLightConfig config = new IdmLightConfigBuilder()
                    .dbDirectory(aaaConfiguration.getDbPath())
                    .dbUser(aaaConfiguration.getUsername())
                    .dbPwd(aaaConfiguration.getDbPassword()).build();
            final PasswordServiceConfig passwordServiceConfig = new PasswordServiceConfigBuilder().setAlgorithm(
                    "SHA-512").setIterations(20000).build();
            this.defaultPasswordHashService = new DefaultPasswordHashService(passwordServiceConfig);
            iidmStore = new H2Store(new IdmLightSimpleConnectionProvider(config), defaultPasswordHashService);
        } else {
            this.defaultPasswordHashService = null;
            iidmStore = null;
            LOG.info("AAA Datastore has not been initialized");
            return;
        }
        this.passwordHashService = defaultPasswordHashService;
        if (credentialAuth == null) {
            IdmLightProxy idmLightProxy = new IdmLightProxy(iidmStore, defaultPasswordHashService);
            this.credentialAuth = idmLightProxy;
            this.claimCache = idmLightProxy;
        }
        this.tokenAuthenticators = buildTokenAuthenticators(this.credentialAuth);
        try {
            final StoreBuilder storeBuilder = new StoreBuilder(iidmStore);
            final String domain = storeBuilder.initDomainAndRolesWithoutUsers(IIDMStore.DEFAULT_DOMAIN);
            if (domain != null) {
                // If is not exist. If domain already exist on path, will be used instead
                storeBuilder.createUser(domain, aaaConfiguration.getUsername(), aaaConfiguration.getPassword(), true);
            }

        } catch (final IDMStoreException e) {
            LOG.error("Failed to initialize data in store", e);
        }
        final LocalHttpServer httpService = new LocalHttpServer(server);
        registerServletContexts(httpService, aaaConfiguration.getMoonEndpointPath());

        initAAAonServer(server);
    }

    private void initAAAonServer(final LightyServerBuilder server) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final ServletContextHandler mainHandler = new ServletContextHandler(contexts, "/auth", true, false);
        final IdmLightApplication idmLightApplication = new IdmLightApplication(iidmStore, claimCache);
        final ServletHolder idmLightServlet = new ServletHolder(new ServletContainer(ResourceConfig.forApplication(
                idmLightApplication)));
        idmLightServlet.setInitParameter("jersey.config.server.provider.packages",
                "org.opendaylight.aaa.impl.provider");
        mainHandler.addServlet(idmLightServlet, "/*");
        server.addContextHandler(contexts);
        this.handlers.add(contexts);
        this.handlers.add(mainHandler);
        this.aaaWebEnvironment = new AAAWebEnvironment(shiroConfiguration,
                dataBroker,
                certificateManager,
                authenticationService,
                tokenAuthenticators,
                passwordHashService,
                new JerseyServletSupport());

        final Map<String, String> properties = new HashMap<>();
        final CustomFilterAdapterConfigurationImpl customFilterAdapterConfig =
                new CustomFilterAdapterConfigurationImpl();
        customFilterAdapterConfig.update(properties);
        final FilterHolder customFilterAdapter = new FilterHolder(new CustomFilterAdapter(customFilterAdapterConfig));
        server.addCommonFilter(customFilterAdapter, "/*");

        final FilterHolder shiroFilter = new FilterHolder(new AAAShiroFilter(aaaWebEnvironment));
        server.addCommonFilter(shiroFilter, "/*");

        final FilterHolder crossOriginFilter = new FilterHolder(new CrossOriginFilter());
        crossOriginFilter.setInitParameter("allowedMethods", "GET,POST,OPTIONS,DELETE,PUT,HEAD");
        crossOriginFilter.setInitParameter("allowedHeaders",
                "origin, content-type, accept, authorization, Authorization");
        server.addCommonFilter(crossOriginFilter, "/*");
    }

    public static CompletableFuture<AAALightyShiroProvider> newInstance(final DataBroker dataBroker,
            final AAAConfiguration aaaConfig, final CredentialAuth<PasswordCredentials> credentialAuth,
            final LightyServerBuilder server) {
        final CompletableFuture<AAALightyShiroProvider> completableFuture = new CompletableFuture<>();
        INSTANCE = new AAALightyShiroProvider(dataBroker, aaaConfig, credentialAuth, server);
        completableFuture.complete(INSTANCE);
        return completableFuture;
    }

    public static AAALightyShiroProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Extract the data broker.
     *
     * @return the data broker
     */
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    /**
     * Extract the certificate manager.
     *
     * @return the certificate manager.
     */
    public ICertificateManager getCertificateManager() {
        return this.certificateManager;
    }

    /**
     * Extract Shiro related configuration.
     *
     * @return Shiro related configuration.
     */
    public ShiroConfiguration getShiroConfiguration() {
        return this.shiroConfiguration;
    }

    public AAAWebEnvironment getAaaWebEnvironment() {
        return aaaWebEnvironment;
    }

    public TokenAuthenticators getTokenAuthenticators() {
        return this.tokenAuthenticators;
    }

    public DefaultPasswordHashService getDefaultPasswordHashService() {
        return defaultPasswordHashService;
    }

    /**
     * Get IDM data store.
     *
     * @return IIDMStore data store
     */
    public static IIDMStore getIdmStore() {
        return INSTANCE.iidmStore;
    }

    /**
     * Set IDM data store, only used for test.
     *
     * @param store data store
     */
    public static void setIdmStore(final IIDMStore store) {
        INSTANCE.iidmStore = store;
    }

    @SuppressWarnings("IllegalCatch")
    public void close() {
        this.handlers.forEach((handler) -> {
            try {
                handler.stop();
            } catch (Exception e) {
                LOG.error("Failed to close AAA handler [{}]", handler, e);
            } finally {
                handler.destroy();
            }
        });
    }

    private static TokenAuthenticators buildTokenAuthenticators(
            final CredentialAuth<PasswordCredentials> credentialAuth) {
        return new TokenAuthenticators(new HttpBasicAuth(credentialAuth));
    }

    private void registerServletContexts(final LocalHttpServer httpService, final String moonEndpointPath) {
        LOG.info("attempting registration of AAA moon and auth servlets");

        Preconditions.checkNotNull(httpService, "httpService cannot be null");
        httpService.registerServlet(moonEndpointPath, new MoonTokenEndpoint(), null);
    }
}
