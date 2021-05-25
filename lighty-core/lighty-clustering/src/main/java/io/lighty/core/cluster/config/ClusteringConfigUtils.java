/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster.config;

import com.typesafe.config.Config;
import java.util.List;

public final class ClusteringConfigUtils {

    public static final String MODULE_SHARDS_TMP_PATH = "/tmp/module-shards.conf";
    public static final String AKKA_DISCOVERY_METHOD_PATH = "akka.discovery.method";
    public static final String K8S_DISCOVERY_API_NAME = "kubernetes-api";

    private ClusteringConfigUtils() {
        // this class should not be instantiated
    }

    /**
     * Generate content of a Module-Shards.conf that specifies the members on which the Shards should be replicated.
     *
     * @param memberRoles - roles (members) to which the module shards should be replicated to
     * @return generated content
     */
    public static String generateModuleShardsForMembers(List<String> memberRoles) {
        return String.format("module-shards = [%n%s]", String.join(",\n",
                new String[]{generateShard("default", memberRoles),
                        generateShard("topology", memberRoles),
                        generateShard("inventory", memberRoles)
                }));
    }

    public static boolean isKubernetesDeployment(Config actorSystemConfig) {
        return actorSystemConfig.hasPath(AKKA_DISCOVERY_METHOD_PATH)
                && actorSystemConfig.getString(AKKA_DISCOVERY_METHOD_PATH).equalsIgnoreCase(K8S_DISCOVERY_API_NAME);
    }

    private static String generateShard(String name, List<String> replicas) {
        return "    {"
                + "        name = \"" + name + "\"\n"
                + "        shards = [\n"
                + "            {\n"
                + "                name=\"" + name + "\"\n"
                + "                replicas = " + replicas
                + "                \n"
                + "            }\n"
                + "        ]\n"
                + "    }";
    }

}
