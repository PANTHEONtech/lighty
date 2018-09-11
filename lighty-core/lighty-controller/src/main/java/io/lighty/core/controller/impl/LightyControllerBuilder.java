/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl;

import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.schema.ScanningSchemaServiceProvider;
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
    public LightyControllerBuilder from(ControllerConfiguration controllerConfiguration) {
        this.controllerConfiguration = controllerConfiguration;
        return this;
    }

    /**
     * Inject executor service to execute futures
     * @param executorService - executor
     * @return instance of {@link LightyControllerBuilder}.
     */
    public LightyControllerBuilder withExecutorService(ExecutorService executorService) {
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
            Set<YangModuleInfo> modelSet = controllerConfiguration.getSchemaServiceConfig().getModels();
            List<URL> yangUrls = YangModuleUtils.searchYangsInYangModuleInfo(modelSet);
            final ScanningSchemaServiceProvider scanningSchemaServiceProvider = new ScanningSchemaServiceProvider();
            scanningSchemaServiceProvider.registerAvailableYangs(yangUrls);
            final DOMNotificationRouter domNotificationRouter = DOMNotificationRouter.create(
                    controllerConfiguration.getDomNotificationRouterConfig().getQueueDepth(),
                    controllerConfiguration.getDomNotificationRouterConfig().getSpinTime(),
                    controllerConfiguration.getDomNotificationRouterConfig().getParkTime(),
                    controllerConfiguration.getDomNotificationRouterConfig().getUnit());
            return new LightyControllerImpl(executorService,
                    controllerConfiguration.getActorSystemConfig().getConfig(),
                    controllerConfiguration.getActorSystemConfig().getClassLoader(),
                    scanningSchemaServiceProvider,
                    domNotificationRouter,
                    controllerConfiguration.getRestoreDirectoryPath(),
                    controllerConfiguration.getMaxDataBrokerFutureCallbackQueueSize(),
                    controllerConfiguration.getMaxDataBrokerFutureCallbackPoolSize(),
                    controllerConfiguration.isMetricCaptureEnabled(),
                    controllerConfiguration.getMailboxCapacity(),
                    controllerConfiguration.getDistributedEosProperties(),
                    controllerConfiguration.getModuleShardsConfig(),
                    controllerConfiguration.getModulesConfig(),
                    controllerConfiguration.getConfigDatastoreContext(),
                    controllerConfiguration.getOperDatastoreContext()
            );
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

}
