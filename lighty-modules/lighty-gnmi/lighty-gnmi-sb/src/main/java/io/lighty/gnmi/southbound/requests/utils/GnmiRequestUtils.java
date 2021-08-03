/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.requests.utils;

import gnmi.Gnmi;
import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.SchemaConstants;
import java.util.ArrayList;
import java.util.List;

public final class GnmiRequestUtils {

    private GnmiRequestUtils() {
        // Utility class
    }

    public static Gnmi.CapabilityRequest makeDefaultCapabilityRequest() {
        return Gnmi.CapabilityRequest.newBuilder().build();
    }

    public static List<GnmiDeviceCapability> fromCapabilitiesResponse(final Gnmi.CapabilityResponse response) {
        final List<GnmiDeviceCapability> caps = new ArrayList<>();
        for (Gnmi.ModelData model : response.getSupportedModelsList()) {
            final GnmiDeviceCapability capability;
            if (model.getVersion().matches(SchemaConstants.REVISION_REGEX)) {
                capability = new GnmiDeviceCapability(model.getName(), null, model.getVersion());
            } else if (model.getVersion().matches(SchemaConstants.SEMVER_REGEX)) {
                capability = new GnmiDeviceCapability(model.getName(), model.getVersion(), null);
            } else {
                capability = new GnmiDeviceCapability(model.getName());
            }

            caps.add(capability);
        }
        return caps;
    }
}
