/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.cluster;

import com.typesafe.config.Config;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.impl.cluster.kubernetes.KubernetesClusteringHandlerImpl;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;

public final class ClusteringHandlerProvider {

    private ClusteringHandlerProvider() {
        // should not be instantiated
    }

    public static Optional<ClusteringHandler> getClusteringHandler(@NonNull LightyController controller,
            @NonNull Config akkaDeploymentConfig) {
        if (akkaDeploymentConfig.hasPath("akka.discovery.method")) {
            String clusteringTool = akkaDeploymentConfig.getString("akka.discovery.method");
            if ("kubernetes-api".equals(clusteringTool)) {
                return Optional.of(new KubernetesClusteringHandlerImpl(controller, akkaDeploymentConfig));
            }
        }
        return Optional.empty();
    }
}
