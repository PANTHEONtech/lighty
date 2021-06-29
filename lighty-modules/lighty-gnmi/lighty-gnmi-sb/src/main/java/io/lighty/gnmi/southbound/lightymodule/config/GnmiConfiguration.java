/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule.config;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of gNMI Southbound.
 */
public class GnmiConfiguration {
    //-----JSON configurables---
    /**
     * Optional list of paths from which ByPathYangLoaderService instances will be created and added to initialLoaders.
     */
    private final List<String> initialYangsPaths;

    public GnmiConfiguration() {
        initialYangsPaths = new ArrayList<>();
    }

    @JsonSetter("initialYangsPaths")
    public void addInitialYangsPaths(final List<String> additionalPaths) {
        this.initialYangsPaths.addAll(additionalPaths);
    }

    public List<String> getInitialYangsPaths() {
        return initialYangsPaths;
    }


}
