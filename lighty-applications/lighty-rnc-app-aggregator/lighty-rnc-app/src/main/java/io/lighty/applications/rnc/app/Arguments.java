/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.app;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Arguments {
    private static final Logger LOG = LoggerFactory.getLogger(Arguments.class);
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
