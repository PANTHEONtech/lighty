/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.bgpapp;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.util.Optional;

public class MainArgs {
    @Parameter(names = {"-c", "--config-path"}, description = "Path to Lighty json config file. "
            + " (If absent, the default configuration will be used)")
    private String configPath;

    public String getConfigPath() {
        return configPath;
    }

    public static Optional<MainArgs> parse(final String[] args) {
        if (args == null || args.length == 0) {
            return Optional.empty();
        }
        final MainArgs mainArgs = new MainArgs();
        new JCommander(mainArgs).parse(args);
        return Optional.of(mainArgs);

    }

}
