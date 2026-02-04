/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.server.LightyJettyServerProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.servlet.ServletException;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.opendaylight.aaa.api.AuthenticationService;
import org.opendaylight.aaa.api.ClaimCache;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.IDMStoreException;
import org.opendaylight.aaa.api.IIDMStore;
import org.opendaylight.aaa.api.PasswordCredentialAuth;
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
import org.opendaylight.aaa.shiro.idm.IdmLightProxy;
import org.opendaylight.aaa.shiro.realm.BasicRealmAuthProvider;
import org.opendaylight.aaa.shiro.realm.RealmAuthProvider;
import org.opendaylight.aaa.shiro.web.env.AAAWebEnvironment;
import org.opendaylight.aaa.shiro.web.env.ShiroWebContextSecurer;
import org.opendaylight.aaa.shiro.web.env.WebInitializer;
import org.opendaylight.aaa.tokenauthrealm.auth.AuthenticationManager;
import org.opendaylight.aaa.web.FilterDetails;
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

    private final DataBroker dataBroker;
    private final ICertificateManager certificateManager;
    private final ShiroConfiguration shiroConfiguration;
    private final AuthenticationService authenticationService;
    private final DefaultPasswordHashService defaultPasswordHashService;
    private RealmAuthProvider realmAuthProvider;
    private CredentialAuth<PasswordCredentials> credentialAuth;
    private ClaimCache claimCache;
    private PasswordHashService passwordHashService;
    private IIDMStore iidmStore;
    private WebInitializer webInitializer;
    private ShiroWebContextSecurer webContextSecurer;

    private AAAWebEnvironment aaaWebEnvironment;

    private AAALightyShiroProvider(final DataBroker dataBroker,
                                   final AAAConfiguration aaaConfiguration,
                                   final CredentialAuth<PasswordCredentials> credentialAuth,
                                   final LightyJettyServerProvider server) {
        this.dataBroker = dataBroker;
        this.certificateManager = aaaConfiguration.getCertificateManager();
        this.credentialAuth = credentialAuth;
        this.shiroConfiguration = aaaConfiguration.getShiroConf();
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
        try {
            final StoreBuilder storeBuilder = new StoreBuilder(iidmStore);
            final String initDomain = storeBuilder.initDomainAndRolesWithoutUsers(IIDMStore.DEFAULT_DOMAIN);

            // If the domain already exists, the init method returns null.
            // We fallback to the default domain string to ensure we have a valid ID for user creation.
            final String domain = initDomain == null ? IIDMStore.DEFAULT_DOMAIN : initDomain;
            // Create custom user from the JSON config
            try {
                storeBuilder.createUser(domain, aaaConfiguration.getUsername(), aaaConfiguration.getPassword(), true);
                LOG.info("Pre-seeded database with custom user '{}'", aaaConfiguration.getUsername());
            } catch (IDMStoreException e) {
                LOG.debug("User already exists, skipping creation.");
            }

        } catch (final IDMStoreException e) {
            LOG.error("Failed to pre-seed data in store", e);
        }
        // Because the database is no longer empty, BasicRealmAuthProvider will
        // see the existing domain/users and gracefully skip its hardcoded admin injection
        this.realmAuthProvider = buildTokenAuthenticators((PasswordCredentialAuth) this.credentialAuth, iidmStore);

        initAAAonServer(server);
    }

    private void initAAAonServer(final LightyJettyServerProvider server) {
        final Map<String, String> properties = new HashMap<>();
        final CustomFilterAdapterConfigurationImpl customFilterAdapterConfig =
            new CustomFilterAdapterConfigurationImpl();

        // Cross-origin filter
        customFilterAdapterConfig.addFilter(FilterDetails.builder()
            .filter(new CrossOriginFilter())
            .addUrlPattern("/*")
            .putInitParam("allowedMethods", "GET,POST,OPTIONS,DELETE,PUT,HEAD")
            .putInitParam("allowedHeaders", "origin, content-type, accept, authorization, Authorization")
            .build().filter());
        // CustomFilterAdapter
        customFilterAdapterConfig.addFilter(FilterDetails.builder()
            .filter(new CustomFilterAdapter(customFilterAdapterConfig))
            .addUrlPattern("/*")
            .build().filter());

        customFilterAdapterConfig.update(properties);

        this.aaaWebEnvironment = new AAAWebEnvironment(
            shiroConfiguration,
            dataBroker,
            certificateManager,
            authenticationService,
            realmAuthProvider,
            passwordHashService,
            new JerseyServletSupport());

        this.webContextSecurer = new ShiroWebContextSecurer(aaaWebEnvironment);

        try {
            this.webInitializer = new WebInitializer(server.getServer(), claimCache, new JerseyServletSupport(),
                webContextSecurer, iidmStore, customFilterAdapterConfig);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

    }

    public static CompletableFuture<AAALightyShiroProvider> newInstance(final DataBroker dataBroker,
            final AAAConfiguration aaaConfig, final CredentialAuth<PasswordCredentials> credentialAuth,
            final LightyJettyServerProvider server) {
        final CompletableFuture<AAALightyShiroProvider> completableFuture = new CompletableFuture<>();
        INSTANCE = new AAALightyShiroProvider(dataBroker, aaaConfig, credentialAuth, server);
        completableFuture.complete(INSTANCE);
        return completableFuture;
    }

    public static synchronized AAALightyShiroProvider getInstance() {
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

    public RealmAuthProvider getRealmAuthProvider() {
        return this.realmAuthProvider;
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

    public ShiroWebContextSecurer webContextSecurer() {
        return webContextSecurer;
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
        if (webInitializer != null) {
            webInitializer.close();
        }
    }

    private static RealmAuthProvider buildTokenAuthenticators(
            final PasswordCredentialAuth auth, final IIDMStore iidmStore) {
        return new BasicRealmAuthProvider(auth, iidmStore);
    }
}
