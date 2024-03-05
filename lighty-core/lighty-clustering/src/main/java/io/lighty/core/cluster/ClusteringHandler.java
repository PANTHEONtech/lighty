/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster;

import com.typesafe.config.Config;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.RpcService;

public interface ClusteringHandler {

    void initClustering();

    void start(@NonNull RpcService clusterAdminRPCService);

    Optional<Config> getModuleShardsConfig();
}
