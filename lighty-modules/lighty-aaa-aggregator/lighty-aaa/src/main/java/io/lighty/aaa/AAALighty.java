/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import io.lighty.aaa.config.AAAConfiguration;
import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.server.LightyServerBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.opendaylight.aaa.api.CredentialAuth;
import org.opendaylight.aaa.api.PasswordCredentials;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;

public final class AAALighty extends AbstractLightyModule {

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

    public AAALighty(final DataBroker dataBroker, final CredentialAuth<PasswordCredentials> credentialAuth,
            final LightyServerBuilder server, final AAAConfiguration config) {
        this.dataBroker = dataBroker;
        this.certificateManager = config.getCertificateManager();
        this.credentialAuth = credentialAuth;
        this.shiroConfiguration = config.getShiroConf();
        this.moonEndpointPath = config.getMoonEndpointPath();
        this.datastoreConfig = config.getDatastoreConf();
        this.dbUsername = config.getDbUsername();
        this.dbPassword = config.getDbPassword();
        this.server = server;
        this.aaaShiroProviderHandler = new AAAShiroProviderHandler();
    }

    @Override
    protected boolean initProcedure() throws InterruptedException {
        final CompletableFuture<AAALightyShiroProvider> newInstance = AAALightyShiroProvider.newInstance(
                this.dataBroker, this.certificateManager, this.credentialAuth, this.shiroConfiguration,
                this.moonEndpointPath, this.datastoreConfig, this.dbUsername, this.dbPassword, this.server);
        final CountDownLatch cdl = new CountDownLatch(1);
        newInstance.whenComplete((t, u) -> {
            AAALighty.this.aaaShiroProviderHandler.setAaaLightyShiroProvider(t);
            cdl.countDown();
        });

        cdl.await();
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
