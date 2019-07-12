package io.lighty.examples.controllers.cluster;

import com.beust.jcommander.Parameter;

public class Arguments {

    @Parameter(names = {"-c", "--config-path" }, description = "Lighty config path")
    private String configPath;

    @Parameter(names = {"-n", "--member-ordinal" }, description = "Ordinal number od cluster member: 1 or 2 or 3", required = true)
    private Integer memberOrdinal;

    public String getConfigPath() {
        return configPath;
    }

    public Integer getMemberOrdinal() {
        return memberOrdinal;
    }

}
