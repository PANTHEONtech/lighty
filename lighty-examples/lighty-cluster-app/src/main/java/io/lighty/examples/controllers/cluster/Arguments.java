/*
 * Copyright (c) 2020 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.cluster;

import com.beust.jcommander.Parameter;

public class Arguments {

    @Parameter(names = {"-c", "--config-path" }, description = "Lighty config path")
    private String configPath;

    @Parameter(names = {"-n", "--member-ordinal" }, description = "Ordinal number od cluster member: 1 or 2 or 3", required = true)
    private Integer memberOrdinal;

    @Parameter(names = {"-k", "--kubernetes-deployment"}, description = "Type of deployment, default is false if not used.")
    private Boolean kubernetesDeployment = false;

    public String getConfigPath() {
        return configPath;
    }

    public Integer getMemberOrdinal() {
        return memberOrdinal;
    }

    public Boolean getKubernetesDeployment() {
        return kubernetesDeployment;
    }

}
