/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import com.google.common.base.Preconditions;
import io.lighty.server.LightyServerBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.aaa.AAAShiroProvider;
import org.opendaylight.aaa.api.ClaimCache;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.IDMStoreException;
import org.opendaylight.aaa.api.IIDMStore;
import org.opendaylight.aaa.api.IdMServiceImpl;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.opendaylight.aaa.api.StoreBuilder;
import org.opendaylight.aaa.api.TokenStore;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.aaa.datastore.h2.H2Store;
import org.opendaylight.aaa.datastore.h2.H2TokenStore;
import org.opendaylight.aaa.datastore.h2.IdmLightConfig;
import org.opendaylight.aaa.datastore.h2.IdmLightConfigBuilder;
import org.opendaylight.aaa.datastore.h2.IdmLightSimpleConnectionProvider;
import org.opendaylight.aaa.filterchain.configuration.CustomFilterAdapterConfiguration;
import org.opendaylight.aaa.filterchain.configuration.impl.CustomFilterAdapterConfigurationImpl;
import org.opendaylight.aaa.filterchain.filters.CustomFilterAdapter;
import org.opendaylight.aaa.impl.password.service.DefaultPasswordHashService;
import org.opendaylight.aaa.shiro.filters.AAAShiroFilter;
import org.opendaylight.aaa.shiro.idm.IdmLightApplication;
import org.opendaylight.aaa.shiro.idm.IdmLightProxy;
import org.opendaylight.aaa.shiro.moon.MoonTokenEndpoint;
import org.opendaylight.aaa.shiro.tokenauthrealm.auth.AuthenticationManager;
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
    private static IIDMStore iidmStore;

    private final List<Handler> handlers;
    private final DataBroker dataBroker;
    private final ICertificateManager certificateManager;
    private final ShiroConfiguration shiroConfiguration;
    private CredentialAuth<PasswordCredentials> credentialAuth;
    private ClaimCache claimCache;

    private TokenStore tokenStore;
    private IdMServiceImpl idmService;

    private AAALightyShiroProvider(final DataBroker dataBroker, final ICertificateManager certificateManager,
            final CredentialAuth<PasswordCredentials> credentialAuth, final ShiroConfiguration shiroConfiguration,
            final String moonEndpointPath, final DatastoreConfig datastoreConfig,
            final String dbUsername, final String dbPassword, final LightyServerBuilder server) {
        this.dataBroker = dataBroker;
        this.certificateManager = certificateManager;
        this.credentialAuth = credentialAuth;
        this.shiroConfiguration = shiroConfiguration;
        this.handlers = new ArrayList<>();

        injectLightyShiroProviderMethodsToOriginalProvider();

        initAAAonServer(server);
        final DefaultPasswordHashService defaultPasswordHashService;
        if (datastoreConfig != null && datastoreConfig.getStore().equals(DatastoreConfig.Store.H2DataStore)) {
            final IdmLightConfig config = new IdmLightConfigBuilder().dbUser(dbUsername).dbPwd(dbPassword).build();
            final PasswordServiceConfig passwordServiceConfig = new PasswordServiceConfigBuilder().setAlgorithm(
                    "SHA-512").setIterations(20000).build();
            defaultPasswordHashService = new DefaultPasswordHashService(passwordServiceConfig);
            iidmStore = new H2Store(new IdmLightSimpleConnectionProvider(config), defaultPasswordHashService);
            this.tokenStore = new H2TokenStore(datastoreConfig.getTimeToLive().longValue(), datastoreConfig.getTimeToWait()
                    .longValue());
        } else {
            iidmStore = null;
            this.tokenStore = null;
            LOG.info("AAA Datastore has not been initialized");
            return;
        }
        if(credentialAuth == null) {
            IdmLightProxy idmLightProxy = new IdmLightProxy(iidmStore, defaultPasswordHashService);
            this.credentialAuth = idmLightProxy;
            this.claimCache = idmLightProxy;
        }
        this.idmService = new IdMServiceImpl(iidmStore);
        try {
            new StoreBuilder(iidmStore).initWithDefaultUsers(IIDMStore.DEFAULT_DOMAIN);
        } catch (final IDMStoreException e) {
            LOG.error("Failed to initialize data in store", e);
        }
        final LocalHttpServer httpService = new LocalHttpServer(server);
        registerServletContexts(httpService, moonEndpointPath);
    }

    private void initAAAonServer(final LightyServerBuilder server) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        final ServletContextHandler mainHandler = new ServletContextHandler(contexts, "/auth", true, false);
        final IdmLightApplication idmLightApplication = new IdmLightApplication(iidmStore, claimCache);
        final ServletHolder idmLightServlet = new ServletHolder(new ServletContainer(ResourceConfig.forApplication(idmLightApplication)));
        idmLightServlet.setInitParameter("jersey.config.server.provider.packages",
                "org.opendaylight.aaa.impl.provider");
        mainHandler.addServlet(idmLightServlet, "/*");
        server.addContextHandler(contexts);
        this.handlers.add(contexts);

        server.addCommonInitParameter("shiroEnvironmentClass",
                "org.opendaylight.aaa.shiro.web.env.KarafIniWebEnvironment");
        server.addCommonEventListener(new EnvironmentLoaderListener());

        final Map<String, String> properties = new HashMap<>();
        final CustomFilterAdapterConfiguration customFilterAdapterConfig = new CustomFilterAdapterConfigurationImpl(properties);
        final FilterHolder customFilterAdapter = new FilterHolder(new CustomFilterAdapter(customFilterAdapterConfig));
        server.addCommonFilter(customFilterAdapter, "/*");

        final FilterHolder shiroFilter = new FilterHolder(new AAAShiroFilter());
        server.addCommonFilter(shiroFilter, "/*");

        final FilterHolder crossOriginFilter = new FilterHolder(new CrossOriginFilter());
        crossOriginFilter.setInitParameter("allowedMethods", "GET,POST,OPTIONS,DELETE,PUT,HEAD");
        crossOriginFilter.setInitParameter("allowedHeaders",
                "origin, content-type, accept, authorization, Authorization");
        server.addCommonFilter(crossOriginFilter, "/*");
    }

    public static CompletableFuture<AAALightyShiroProvider> newInstance(final DataBroker dataBroker,
            final ICertificateManager certificateManager, final CredentialAuth<PasswordCredentials> credentialAuth,
            final ShiroConfiguration shiroConfiguration, final String moonEndpointPath,
            final DatastoreConfig datastoreConfig, final String dbUsername, final String dbPassword,
            final LightyServerBuilder server) {
        final CompletableFuture<AAALightyShiroProvider> completableFuture = new CompletableFuture<>();
        INSTANCE = new AAALightyShiroProvider(dataBroker, certificateManager, credentialAuth,
                shiroConfiguration, moonEndpointPath, datastoreConfig, dbUsername, dbPassword, server);
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

    /**
     * Get IDM data store.
     *
     * @return IIDMStore data store
     */
    public static IIDMStore getIdmStore() {
        return iidmStore;
    }

    /**
     * Set IDM data store, only used for test.
     *
     * @param store data store
     */
    public static void setIdmStore(final IIDMStore store) {
        iidmStore = store;
    }

    public void close() {
        this.handlers.forEach((handler) -> {
            handler.destroy();
        });
    }

    private void registerServletContexts(final LocalHttpServer httpService, final String moonEndpointPath) {
        LOG.info("attempting registration of AAA moon and auth servlets");

        Preconditions.checkNotNull(httpService, "httpService cannot be null");
        httpService.registerServlet(moonEndpointPath, new MoonTokenEndpoint(), null);
    }

    /**
     * In this way, we do not need to call new instance of {@link AAAShiroProvider}. We want to avoid dependence
     * on OSGi. According to {@link AAAShiroProvider} is singleton, we need to hook public and public static methods
     * from the {@link AAAShiroProvider} with methods from {@link AAALightyShiroProvider}.
     */
    private void injectLightyShiroProviderMethodsToOriginalProvider() {
        try {

            final CtClass ctClass = ClassPool.getDefault().get("org.opendaylight.aaa.AAAShiroProvider");
            final CtConstructor c = CtNewConstructor.make("public AAAShiroProvider(){}", ctClass);
            ctClass.addConstructor(c);

            ctClass.removeMethod(ctClass.getDeclaredMethod("getInstance"));
            final CtMethod getInstance = CtNewMethod.make(
                    "public static org.opendaylight.aaa.AAAShiroProvider getInstance() {"
                            + "return new org.opendaylight.aaa.AAAShiroProvider();}",
                            ctClass);
            ctClass.addMethod(getInstance);

            ctClass.removeMethod(ctClass.getDeclaredMethod("getInstanceFuture"));
            final CtMethod getInstanceFuture = CtNewMethod.make(
                    "public static java.util.concurrent.CompletableFuture getInstanceFuture() {"
                            + "java.util.concurrent.CompletableFuture completableFuture = "
                            + "new java.util.concurrent.CompletableFuture();"
                            + "completableFuture.complete(org.opendaylight.aaa.AAAShiroProvider.getInstance());"
                            + "return completableFuture;}",
                            ctClass);
            ctClass.addMethod(getInstanceFuture);

            ctClass.removeMethod(ctClass.getDeclaredMethod("getIdmStore"));
            final CtMethod getIdmStore = CtNewMethod.make(
                    "public static org.opendaylight.aaa.api.IIDMStore getIdmStore() {"
                            + "return io.lighty.aaa.AAALightyShiroProvider.getIdmStore();}",
                            ctClass);
            ctClass.addMethod(getIdmStore);

            ctClass.removeMethod(ctClass.getDeclaredMethod("setIdmStore"));
            final CtMethod setIdmStore = CtNewMethod.make(
                    "public static void setIdmStore(org.opendaylight.aaa.api.IIDMStore store) {"
                            + "return io.lighty.aaa.AAALightyShiroProvider.setIdmStore(store);}", ctClass);
            ctClass.addMethod(setIdmStore);

            ctClass.removeMethod(ctClass.getDeclaredMethod("getDataBroker"));
            final CtMethod getDataBroker = CtNewMethod.make(
                    "public org.opendaylight.controller.md.sal.binding.api.DataBroker getDataBroker() {"
                            + "return io.lighty.aaa.AAALightyShiroProvider.getInstance().getDataBroker();}", ctClass);
            ctClass.addMethod(getDataBroker);

            ctClass.removeMethod(ctClass.getDeclaredMethod("getCertificateManager"));
            final CtMethod getCertificateManager = CtNewMethod.make(
                    "public org.opendaylight.aaa.cert.api.ICertificateManager getCertificateManager() {"
                            + "return io.lighty.aaa.AAALightyShiroProvider.getInstance().getCertificateManager();}",
                            ctClass);
            ctClass.addMethod(getCertificateManager);

            ctClass.removeMethod(ctClass.getDeclaredMethod("getShiroConfiguration"));
            final CtMethod getShiroConfiguration = CtNewMethod.make(
                    "public org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration "
                            + "getShiroConfiguration() {"
                            + "return io.lighty.aaa.AAALightyShiroProvider.getInstance().getShiroConfiguration();}",
                            ctClass);
            ctClass.addMethod(getShiroConfiguration);

            ctClass.toClass();

        } catch (NotFoundException | CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }
}
