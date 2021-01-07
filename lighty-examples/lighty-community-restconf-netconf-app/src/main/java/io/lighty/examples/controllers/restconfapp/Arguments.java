package io.lighty.examples.controllers.restconfapp;

import com.beust.jcommander.Parameter;

public class Arguments {

    @Parameter(names = {"-c", "--config-path"}, description = "Path to Lighty json config file. "
            + " (If absent, the default will be used)")
    private String configPath;

    @Parameter(names = {"-l", "--logger-config-path"}, description = "Path to custom xml log4j properties file. "
            + " (If absent, will look on classpath for it")
    private String loggerPath;

    public String getConfigPath() {
        return configPath;
    }

    public String getLoggerPath() {
        return loggerPath;
    }

}