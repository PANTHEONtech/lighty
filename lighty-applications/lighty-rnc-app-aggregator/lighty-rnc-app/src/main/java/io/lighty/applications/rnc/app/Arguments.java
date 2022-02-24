/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.app;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
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

    @Parameter(names = {"-t", "--timeout-in-seconds"}, validateWith = ModuleTimeoutValidator.class,
               description = "Lighty modules timeout in seconds. Timeout exception is thrown when lighty module fails "
                       + "to start within the specified time. Default value is 60. (range: 15 - Integer.MAX_VALUE)")
    private Integer moduleTimeout = 60;

    public String getConfigPath() {
        return configPath;
    }

    public String getLoggerPath() {
        return loggerPath;
    }

    public Integer getModuleTimeout() {
        return moduleTimeout;
    }

    public static final class ModuleTimeoutValidator implements IParameterValidator {
        @Override
        public void validate(final String name, final String value) throws ParameterException {
            final int intValue;
            try {
                intValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ParameterException("Expected number after parameter \"-t\", \"--timeout-in-seconds\". "
                        + "Provided: " + value, e);
            }
            if (intValue < 15) {
                throw new ParameterException("Provided application timeout " + value
                        + " is not in range (15 - Integer.MAX_VALUE)");
            }
        }
    }
}
