/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.tests;

import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyServices;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightyTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LightyTestUtils.class);
    public static final long MAX_START_TIME_MILLIS = 30_000;

    private LightyTestUtils() {
    }

    public static LightyController startController() {
        return startController(Collections.emptyList());
    }

    public static LightyController startController(final Collection<YangModuleInfo> additionalModels) {
        LOG.info("Building LightyController");
        final LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        final Set<YangModuleInfo> mavenModelPaths = new HashSet<>();
        mavenModelPaths.addAll(RestConfConfigUtils.YANG_MODELS);
        mavenModelPaths.addAll(NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS);
        mavenModelPaths.addAll(additionalModels);
        final LightyController lightyController;
        try {
            lightyController = lightyControllerBuilder.from(ControllerConfigUtils.getDefaultSingleNodeConfiguration(
                    mavenModelPaths)).build();
            LOG.info("Starting LightyController");
            final ListenableFuture<Boolean> started = lightyController.start();
            started.get(MAX_START_TIME_MILLIS, TimeUnit.MILLISECONDS);
            LOG.info("LightyController started");
            return lightyController;
        } catch (ConfigurationException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public static CommunityRestConf startRestconf(RestConfConfiguration restConfConfiguration,
            final LightyServices services) {
        LOG.info("Building CommunityRestConf");
        try {
            final CommunityRestConf communityRestConf = CommunityRestConfBuilder
                    .from(RestConfConfigUtils.getRestConfConfiguration(restConfConfiguration,
                            services))
                    .withScheduledThreadPool(services.getScheduledThreadPool())
                    .build();

            LOG.info("Starting CommunityRestConf");
            final ListenableFuture<Boolean> restconfStart = communityRestConf.start();
            restconfStart.get(MAX_START_TIME_MILLIS, TimeUnit.MILLISECONDS);
            LOG.info("CommunityRestConf started");
            return communityRestConf;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

}
