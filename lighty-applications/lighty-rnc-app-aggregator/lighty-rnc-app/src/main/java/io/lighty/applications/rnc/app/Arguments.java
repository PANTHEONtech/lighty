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

    @Parameter(names = {"-t", "--timeout-in-seconds"}, validateWith = ApplicationTimeoutValidator.class,
               description = "Application timeout in seconds. This parameter specifies max time which application"
                       + " will wait until a timeout exception will be thrown. Default value is 60. (15 - INT.MAX)")
    private Integer applicationTimeout = 60;

    public String getConfigPath() {
        return configPath;
    }

    public String getLoggerPath() {
        return loggerPath;
    }

    public Integer getApplicationTimeout() {
        return applicationTimeout;
    }


    public static final class ApplicationTimeoutValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) throws ParameterException {
            int intValue = Integer.parseInt(value);
            if (intValue < 15) {
                throw new ParameterException("Provided application timeout " + value
                        + " is not in range (15 - INT.MAX)");
            }
        }
    }

}
