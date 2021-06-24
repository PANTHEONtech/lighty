/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device.connection;

import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ExtensionsParameters;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.extensions.parameters.GnmiParameters;

public class ConfigurableParameters {

    private final GnmiParameters gnmiParameters;

    public ConfigurableParameters(final ExtensionsParameters extensionsParameters) {
        gnmiParameters = extensionsParameters == null ? null : extensionsParameters.getGnmiParameters();
    }

    public Optional<Boolean> getUseModelNamePrefix() {
        if (gnmiParameters != null) {
            return Optional.ofNullable(gnmiParameters.getUseModelNamePrefix());
        }
        return Optional.empty();
    }

    public Optional<GnmiParameters.OverwriteDataType> getOverwriteDataType() {
        if (gnmiParameters != null) {
            return Optional.ofNullable(gnmiParameters.getOverwriteDataType());
        }
        return Optional.empty();
    }

    public Optional<String> getPathTarget() {
        if (gnmiParameters != null) {
            return Optional.ofNullable(gnmiParameters.getPathTarget());
        }
        return Optional.empty();
    }
}
