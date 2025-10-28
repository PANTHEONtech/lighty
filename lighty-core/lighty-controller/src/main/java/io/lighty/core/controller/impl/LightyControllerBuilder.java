/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl;

import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

/**
 * Builder for {@link LightyController}.
 */
public class LightyControllerBuilder {
    private ControllerConfiguration controllerConfiguration = null;
    private ExecutorService executorService = null;

    /**
     * Create new instance of {@link LightyControllerBuilder} from {@link ControllerConfiguration}.
     *
     * @param newControllerConfiguration input Lighty Controller configuration.
     * @return instance of {@link LightyControllerBuilder}.
     */
    public LightyControllerBuilder from(final ControllerConfiguration newControllerConfiguration) {
        this.controllerConfiguration = newControllerConfiguration;
        return this;
    }

    /**
     * Inject executor service to execute futures.
     *
     * @param newExecutorService - executor
     * @return instance of {@link LightyControllerBuilder}.
     */
    public LightyControllerBuilder withExecutorService(final ExecutorService newExecutorService) {
        this.executorService = newExecutorService;
        return this;
    }

    /**
     * Build new {@link LightyController} instance from {@link LightyControllerBuilder}.
     *
     * @return instance of LightyController.
     * @throws ConfigurationException if cannot find yang model artifacts.
     */
    @SuppressWarnings("checkstyle:illegalCatch")
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
                    modelSet,
                    this.controllerConfiguration.getInitialConfigData()
            );
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

}
