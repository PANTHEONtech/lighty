/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import com.google.common.collect.ImmutableSet;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.server.LightyServerBuilder;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AAALighty extends AbstractLightyModule {
    private static final Logger LOG = LoggerFactory.getLogger(AAALighty.class);

    public static final Set<YangModuleInfo> YANG_MODELS = ImmutableSet.of(
            org.opendaylight.yang.gen.v1.config.aaa.authn.encrypt.service.config.rev160915.$YangModuleInfoImpl
            .getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.yang.aaa.cert.mdsal.rev160321.$YangModuleInfoImpl
            .getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.$YangModuleInfoImpl
            .getInstance());

    private final AAAShiroProviderHandler aaaShiroProviderHandler;
    private final LightyServerBuilder server;
    private final String dbPassword;
    private final String dbUsername;
    private final DatastoreConfig datastoreConfig;
    private final String moonEndpointPath;
    private final ShiroConfiguration shiroConfiguration;
    private final CredentialAuth<PasswordCredentials> credentialAuth;
    private final ICertificateManager certificateManager;
    private final DataBroker dataBroker;

    public AAALighty(final DataBroker dataBroker, final ICertificateManager certificateManager, final CredentialAuth<
            PasswordCredentials> credentialAuth, final ShiroConfiguration shiroConfiguration,
            final String moonEndpointPath, final DatastoreConfig datastoreConfig,
            final String dbUsername, final String dbPassword, final LightyServerBuilder server) {
        this.dataBroker = dataBroker;
        this.certificateManager = certificateManager;
        this.credentialAuth = credentialAuth;
        this.shiroConfiguration = shiroConfiguration;
        this.moonEndpointPath = moonEndpointPath;
        this.datastoreConfig = datastoreConfig;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.server = server;
        this.aaaShiroProviderHandler = new AAAShiroProviderHandler();
    }

    @Override
    protected boolean initProcedure() {
        final CompletableFuture<AAALightyShiroProvider> newInstance = AAALightyShiroProvider.newInstance(
                this.dataBroker, this.certificateManager, this.credentialAuth, this.shiroConfiguration,
                this.moonEndpointPath, this.datastoreConfig, this.dbUsername, this.dbPassword, this.server);
        final CountDownLatch cdl = new CountDownLatch(1);
        newInstance.whenComplete((t, u) -> {
            AAALighty.this.aaaShiroProviderHandler.setAaaLightyShiroProvider(t);
            cdl.countDown();
        });
        try {
            cdl.await();
        } catch (final InterruptedException e) {
            LOG.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        this.aaaShiroProviderHandler.getAaaLightyShiroProvider().close();
        return true;
    }

    private static class AAAShiroProviderHandler {

        AAALightyShiroProvider aaaLightyShiroProvider;

        void setAaaLightyShiroProvider(final AAALightyShiroProvider aaaLightyShiroProvider) {
            this.aaaLightyShiroProvider = aaaLightyShiroProvider;
        }

        AAALightyShiroProvider getAaaLightyShiroProvider() {
            return this.aaaLightyShiroProvider;
        }
    }
}
