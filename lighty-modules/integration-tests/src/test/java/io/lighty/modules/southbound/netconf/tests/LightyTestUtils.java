/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
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
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class LightyTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LightyTestUtils.class);

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
            LOG.info("Starting LightyController (waiting 10s after start)");
            final ListenableFuture<Boolean> started = lightyController.start();
            started.get();
            LOG.info("LightyController started");
            return lightyController;
        } catch (ConfigurationException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static CommunityRestConf startRestconf(final LightyServices services) {
        LOG.info("Building CommunityRestConf");
        final CommunityRestConfBuilder builder = new CommunityRestConfBuilder();
        try {
            builder.from(RestConfConfigUtils.getDefaultRestConfConfiguration(services));
            final CommunityRestConf communityRestConf = builder.build();

            LOG.info("Starting CommunityRestConf (waiting 10s after start)");
            final ListenableFuture<Boolean> restconfStart = communityRestConf.start();
            restconfStart.get();
            Thread.sleep(3_000);
            LOG.info("CommunityRestConf started");
            return communityRestConf;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
