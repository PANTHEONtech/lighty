/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClusteringConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteringConfigUtils.class);

    public static final String MODULE_SHARDS_TMP_PATH = "/tmp/module-shards.conf";
    public static final String K8S_POD_RESTART_TIMEOUT_PATH = "akka.lighty-kubernetes.pod-restart-timeout";

    private ClusteringConfigUtils() {
        // this class should not be instantiated
    }

    /**
     * Generate content of a Module-Shards.conf that specifies the members on which the Shards should be replicated.
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
        return actorSystemConfig.hasPath("akka.discovery.method")
                && actorSystemConfig.getString("akka.discovery.method").equalsIgnoreCase("kubernetes-api");
    }

    /**
     * Reads pod-namespace from akka.discovery.kubernetes-api.
     * @param actorSystemConfig provided akka configuration
     * @return configured pod-namespace value
     */
    public static Optional<String> getPodNamespaceFromConfig(Config actorSystemConfig){
        String path = "akka.discovery.kubernetes-api.pod-namespace";

        return actorSystemConfig.hasPath(path) ? Optional.of(actorSystemConfig.getString(path)) : Optional.empty();
    }
    /**
     * Reads pod-label-selector from akka.discovery.kubernetes-api.
     * @param actorSystemConfig provided akka configuration
     * @return configured pod-label-selector value
     */
    public static Optional<String> getPodSelectorFromConfig(Config actorSystemConfig){
        String path = "akka.discovery.kubernetes-api.pod-label-selector";

        return actorSystemConfig.hasPath(path) ? Optional.of(actorSystemConfig.getString(path)) : Optional.empty();
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

    /**
     * Prepared for future when Module Shards Config could be created and loaded dynamically in runtime
     * instead of creating File and then passing it's path.
     *
     * @param memberRoles - roles (members) to which the module shards should be replicated to
     * @return Config object representing this Module-Shards configuration
     */
    public static Config getModuleShardsConfigForMember(List<String> memberRoles) {
        LOG.info("Generating Module-Shards CONFIG");
        return ConfigFactory.parseString(generateModuleShardsForMembers(memberRoles));
    }
}
