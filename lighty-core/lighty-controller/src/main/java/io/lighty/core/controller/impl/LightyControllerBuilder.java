/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Builder for {@link LightyController}.
 */
public class LightyControllerBuilder {

    private ControllerConfiguration controllerConfiguration;
    private ExecutorService executorService = null;

    public LightyControllerBuilder() {
    }

    /**
     * Create new instance of {@link LightyControllerBuilder} from {@link ControllerConfiguration}.
     * @param controllerConfiguration input Lighty Controller configuration.
     * @return instance of {@link LightyControllerBuilder}.
     */
    public LightyControllerBuilder from(final ControllerConfiguration controllerConfiguration) {
        this.controllerConfiguration = controllerConfiguration;
        return this;
    }

    /**
     * Inject executor service to execute futures
     * @param executorService - executor
     * @return instance of {@link LightyControllerBuilder}.
     */
    public LightyControllerBuilder withExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Build new {@link LightyController} instance from {@link LightyControllerBuilder}.
     * @return instance of LightyController.
     * @throws ConfigurationException if cannot find yang model artifacts.
     */
    public LightyController build() throws ConfigurationException {
        try {
            final Set<YangModuleInfo> modelSet = this.controllerConfiguration.getSchemaServiceConfig().getModels();
            final DOMNotificationRouter domNotificationRouter = DOMNotificationRouter.create(
                    this.controllerConfiguration.getDomNotificationRouterConfig().getQueueDepth(),
                    this.controllerConfiguration.getDomNotificationRouterConfig().getSpinTime(),
                    this.controllerConfiguration.getDomNotificationRouterConfig().getParkTime(),
                    this.controllerConfiguration.getDomNotificationRouterConfig().getUnit());
            return new LightyControllerImpl(this.executorService,
                    this.controllerConfiguration.getActorSystemConfig().getConfig(),
                    this.controllerConfiguration.getActorSystemConfig().getClassLoader(),
                    domNotificationRouter,
                    this.controllerConfiguration.getRestoreDirectoryPath(),
                    this.controllerConfiguration.getMaxDataBrokerFutureCallbackQueueSize(),
                    this.controllerConfiguration.getMaxDataBrokerFutureCallbackPoolSize(),
                    this.controllerConfiguration.isMetricCaptureEnabled(),
                    this.controllerConfiguration.getMailboxCapacity(),
                    this.controllerConfiguration.getDistributedEosProperties(),
                    this.controllerConfiguration.getModuleShardsConfig(),
                    this.controllerConfiguration.getModulesConfig(),
                    this.controllerConfiguration.getConfigDatastoreContext(),
                    this.controllerConfiguration.getOperDatastoreContext(),
                    modelSet
                    );
        } catch (final Exception e) {
            throw new ConfigurationException(e);
        }
    }

}
