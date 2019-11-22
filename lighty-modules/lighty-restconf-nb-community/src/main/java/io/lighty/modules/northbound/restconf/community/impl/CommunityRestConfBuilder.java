/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl;

import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.server.LightyServerBuilder;
import java.util.concurrent.ExecutorService;

/**
 * Builder for {@link CommunityRestConf}.
 */
public class CommunityRestConfBuilder {

    private RestConfConfiguration restconfConfiguration;
    private ExecutorService executorService = null;
    private LightyServerBuilder lightyServerBuilder = null;

    /**
     * Create new instance of {@link CommunityRestConfBuilder} from {@link RestConfConfiguration}.
     * @param restconfConfiguration input RestConf configuration.
     * @return instance of {@link CommunityRestConfBuilder}.
     */
    public CommunityRestConfBuilder from(final RestConfConfiguration restconfConfiguration) {
        this.restconfConfiguration = restconfConfiguration;
        return this;
    }

    /**
     * Inject executor service to execute futures
     * @param executorService
     * @return instance of {@link CommunityRestConfBuilder}.
     */
    public CommunityRestConfBuilder withExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Inject lighty server builder
     *
     * @param lightyServerBuilder
     * @return instance of {@link CommunityRestConfBuilder}.
     */
    public CommunityRestConfBuilder withLightyServer(final LightyServerBuilder lightyServerBuilder) {
        this.lightyServerBuilder = lightyServerBuilder;
        return this;
    }

    /**
     * Build new {@link CommunityRestConf} instance from {@link CommunityRestConfBuilder}.
     * @return instance of CommunityRestConf.
     */
    public CommunityRestConf build() {
        return new CommunityRestConf(this.restconfConfiguration.getDomDataBroker(),
            this.restconfConfiguration.getDomSchemaService(), this.restconfConfiguration.getDomRpcService(),
            this.restconfConfiguration.getDomActionService(), this.restconfConfiguration.getDomNotificationService(),
            this.restconfConfiguration.getDomMountPointService(), this.restconfConfiguration.getWebSocketPort(),
            this.restconfConfiguration.getJsonRestconfServiceType(), this.restconfConfiguration.getDomSchemaService(),
            this.restconfConfiguration.getInetAddress(), this.restconfConfiguration.getHttpPort(),
            this.restconfConfiguration.getRestconfServletContextPath(), this.executorService, this.lightyServerBuilder);
    }
}
