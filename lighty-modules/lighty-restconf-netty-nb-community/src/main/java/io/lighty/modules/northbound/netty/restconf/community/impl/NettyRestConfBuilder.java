/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.netty.restconf.community.impl;

import io.lighty.modules.northbound.netty.restconf.community.impl.config.NettyRestConfConfiguration;
import org.apache.shiro.web.env.WebEnvironment;

public final class NettyRestConfBuilder {

    private final NettyRestConfConfiguration restconfConfiguration;
    private WebEnvironment webEnvironment;

    private NettyRestConfBuilder(final NettyRestConfConfiguration configuration) {
        this.restconfConfiguration = configuration;
    }

    /**
     * Create new instance of {@link NettyRestConfBuilder} from {@link NettyRestConfConfiguration}.
     * @param configuration input RestConf configuration.
     * @return instance of {@link NettyRestConfBuilder}.
     */
    public static NettyRestConfBuilder from(final NettyRestConfConfiguration configuration) {
        return new NettyRestConfBuilder(configuration);
    }

    /**
     * Inject webEnvironment.
     *
     * @param environment input webEnvironment.
     * @return instance of {@link NettyRestConfBuilder}.
     */
    public NettyRestConfBuilder withWebEnvironment(final WebEnvironment environment) {
        this.webEnvironment = environment;
        return this;
    }

    /**
     * Build new {@link NettyRestConf} instance from {@link NettyRestConfBuilder}.
     * @return instance of CommunityRestConf.
     */
    public NettyRestConf build() {
        return new NettyRestConf(
            restconfConfiguration.getDomDataBroker(),
            restconfConfiguration.getDomRpcService(),
            restconfConfiguration.getDomNotificationService(),
            restconfConfiguration.getDomActionService(),
            restconfConfiguration.getDomMountPointService(),
            restconfConfiguration.getSchemaService(),
            restconfConfiguration.getInetAddress(),
            restconfConfiguration.getHttpPort(),
            restconfConfiguration.getRestconfServletContextPath(),
            webEnvironment
        );
    }
}
