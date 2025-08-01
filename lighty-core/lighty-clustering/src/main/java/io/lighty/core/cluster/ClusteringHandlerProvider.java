/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster;

import com.typesafe.config.Config;
import io.lighty.core.cluster.config.ClusteringConfigUtils;
import io.lighty.core.cluster.kubernetes.KubernetesClusteringHandlerImpl;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.ActorSystemProvider;

public final class ClusteringHandlerProvider {

    private ClusteringHandlerProvider() {
        // should not be instantiated
    }

    public static Optional<ClusteringHandler> getClusteringHandler(@NonNull ActorSystemProvider actorSystemProvider,
                                                                   @NonNull Config pekkoDeploymentConfig) {
        if (ClusteringConfigUtils.isKubernetesDeployment(pekkoDeploymentConfig)) {
            return Optional.of(new KubernetesClusteringHandlerImpl(actorSystemProvider, pekkoDeploymentConfig));
        }
        return Optional.empty();
    }
}
