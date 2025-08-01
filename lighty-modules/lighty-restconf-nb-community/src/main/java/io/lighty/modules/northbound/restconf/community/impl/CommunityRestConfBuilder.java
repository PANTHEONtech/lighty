/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl;

import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.server.LightyJettyServerProvider;
import org.opendaylight.aaa.web.WebContextSecurer;

/**
 * Builder for {@link CommunityRestConf}.
 */
public final class CommunityRestConfBuilder {

    private RestConfConfiguration restconfConfiguration = null;
    private LightyJettyServerProvider lightyServerBuilder = null;
    private WebContextSecurer webContextSecurer;


    private CommunityRestConfBuilder(final RestConfConfiguration configuration) {
        this.restconfConfiguration = configuration;
    }

    /**
     * Create new instance of {@link CommunityRestConfBuilder} from {@link RestConfConfiguration}.
     * @param configuration input RestConf configuration.
     * @return instance of {@link CommunityRestConfBuilder}.
     */
    public static CommunityRestConfBuilder from(final RestConfConfiguration configuration) {
        return new CommunityRestConfBuilder(configuration);
    }


    /**
     * Inject lighty server builder.
     *
     * @param serverBuilder input server builder.
     * @return instance of {@link CommunityRestConfBuilder}.
     */
    public CommunityRestConfBuilder withLightyServer(final LightyJettyServerProvider serverBuilder) {
        this.lightyServerBuilder = serverBuilder;
        return this;
    }

    /**
     * Inject lighty web securer.
     *
     * @param webSecurer input web securer (can be null for no auth).
     * @return instance of {@link CommunityRestConfBuilder}.
     */
    public CommunityRestConfBuilder withWebSecurer(final WebContextSecurer webSecurer) {
        this.webContextSecurer = webSecurer;
        return this;
    }

    /**
     * Add ScheduledThreadPool.
     *
     * @param pool input scheduledThreadPool.
     * @return instance of {@link CommunityRestConfBuilder}.
     */

    /**
     * Build new {@link CommunityRestConf} instance from {@link CommunityRestConfBuilder}.
     * @return instance of CommunityRestConf.
     */
    public CommunityRestConf build() {
        return new CommunityRestConf(
            restconfConfiguration.getDomDataBroker(),
            restconfConfiguration.getDomRpcService(),
            restconfConfiguration.getDomNotificationService(),
            restconfConfiguration.getDomActionService(),
            restconfConfiguration.getDomMountPointService(),
            restconfConfiguration.getDomSchemaService(),
            restconfConfiguration.getInetAddress(),
            restconfConfiguration.getHttpPort(),
            lightyServerBuilder,
            webContextSecurer
        );
    }
}
