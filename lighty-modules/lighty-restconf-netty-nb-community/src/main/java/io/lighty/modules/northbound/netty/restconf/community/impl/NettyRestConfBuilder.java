package io.lighty.modules.northbound.netty.restconf.community.impl;

import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import org.apache.shiro.web.env.WebEnvironment;

public class NettyRestConfBuilder {

    private RestConfConfiguration restconfConfiguration = null;
    private WebEnvironment webEnvironment;


    private NettyRestConfBuilder(final RestConfConfiguration configuration) {
        this.restconfConfiguration = configuration;
    }

    /**
     * Create new instance of {@link NettyRestConfBuilder} from {@link RestConfConfiguration}.
     * @param configuration input RestConf configuration.
     * @return instance of {@link NettyRestConfBuilder}.
     */
    public static NettyRestConfBuilder from(final RestConfConfiguration configuration) {
        return new NettyRestConfBuilder(configuration);
    }

    /**
     * Inject lighty webEnvironment.
     *
     * @param webEnvironment
     * @return instance of {@link NettyRestConfBuilder}.
     */
    public NettyRestConfBuilder withWebEnvironment(final WebEnvironment webEnvironment) {
        this.webEnvironment = webEnvironment;
        return this;
    }

    /**
     * Add ScheduledThreadPool.
     *
     * @param pool input scheduledThreadPool.
     * @return instance of {@link CommunityRestConfBuilder}.
     */

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
            restconfConfiguration.getDomSchemaService(),
            webEnvironment
        );
    }
}
