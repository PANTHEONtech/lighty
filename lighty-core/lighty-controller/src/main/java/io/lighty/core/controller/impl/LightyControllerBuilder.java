/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
            return new LightyControllerImpl(this.executorService,
                    this.controllerConfiguration.getActorSystemConfig().getConfig(),
                    this.controllerConfiguration.getActorSystemConfig().getClassLoader(),
                    controllerConfiguration.getDomNotificationRouterConfig(),
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
                    this.controllerConfiguration.getDatastoreProperties(),
                    modelSet
                    );
        } catch (final Exception e) {
            throw new ConfigurationException(e);
        }
    }

}
