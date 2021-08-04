/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.impl;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import java.util.ArrayList;
import java.util.List;

// Exception when creating schema context for device
public class SchemaException extends Exception {

    private final List<GnmiDeviceCapability> missingModels;
    private final List<String> errorMessages;

    public SchemaException() {
        missingModels = new ArrayList<>();
        errorMessages = new ArrayList<>();
    }

    public List<GnmiDeviceCapability> getMissingModels() {
        return missingModels;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void addErrorMessage(String message) {
        errorMessages.add(message);
    }

    public void addMissingModel(GnmiDeviceCapability missingModel) {
        missingModels.add(missingModel);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Errors: ");
        for (String error : errorMessages) {
            builder.append("\t").append(error).append("\n");
        }
        if (!missingModels.isEmpty()) {
            builder.append("Missing models: ");
            for (GnmiDeviceCapability cap : missingModels) {
                builder.append("\t").append(cap).append("\n");
            }
        }
        return builder.toString();
    }
}
