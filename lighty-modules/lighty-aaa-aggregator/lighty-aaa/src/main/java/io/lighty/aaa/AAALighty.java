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
import org.opendaylight.mdsal.binding.api.DataBroker;

public final class AAALighty extends AbstractLightyModule {

    private final AAAShiroProviderHandler aaaShiroProviderHandler;
    private final LightyServerBuilder server;
    private final CredentialAuth<PasswordCredentials> credentialAuth;
    private final DataBroker dataBroker;

    private final AAAConfiguration aaaConfiguration;

    public AAALighty(DataBroker dataBroker, CredentialAuth<PasswordCredentials> credentialAuth,
            LightyServerBuilder server, AAAConfiguration config) {
        this.dataBroker = dataBroker;
        this.aaaConfiguration = config;
        this.credentialAuth = credentialAuth;
        this.server = server;
        this.aaaShiroProviderHandler = new AAAShiroProviderHandler();
    }

    @Override
    protected boolean initProcedure() throws InterruptedException {
        CompletableFuture<AAALightyShiroProvider> newInstance = AAALightyShiroProvider.newInstance(
                this.dataBroker, this.aaaConfiguration, this.credentialAuth, this.server);
        var cdl = new CountDownLatch(1);
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

        AAALightyShiroProvider getAaaLightyShiroProvider() {
            return this.aaaLightyShiroProvider;
        }

        void setAaaLightyShiroProvider(AAALightyShiroProvider aaaLightyShiroProvider) {
            this.aaaLightyShiroProvider = aaaLightyShiroProvider;
        }
    }
}
