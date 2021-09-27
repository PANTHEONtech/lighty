/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.impl;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Key used for caching schema context based on capabilities.
 */
public class CapabilitiesKey {

    private final List<GnmiDeviceCapability> capabilityList;

    public CapabilitiesKey(final List<GnmiDeviceCapability> capabilityList) {
        this.capabilityList = capabilityList;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        CapabilitiesKey that = (CapabilitiesKey) other;
        return CollectionUtils.isEqualCollection(this.capabilityList, that.capabilityList);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (GnmiDeviceCapability cap : capabilityList) {
            hash += cap.hashCode();
        }
        return hash;
    }
}
