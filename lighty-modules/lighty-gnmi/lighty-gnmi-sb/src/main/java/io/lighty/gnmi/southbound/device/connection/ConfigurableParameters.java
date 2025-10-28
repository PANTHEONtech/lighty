/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.device.connection;

import gnmi.Gnmi;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.force.capabilities.rev210702.ForceCapabilities;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ExtensionsParameters;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.extensions.parameters.GnmiParameters;

public class ConfigurableParameters {
    private final Optional<List<Gnmi.ModelData>> modelDataList;
    private final Optional<Boolean> useModelNamePrefix;
    private final Optional<GnmiParameters.OverwriteDataType> overwriteDataType;
    private final Optional<String> pathTarget;

    public ConfigurableParameters(final ExtensionsParameters extensionsParameters) {
        final GnmiParameters gnmiParameters;
        final ForceCapabilities forceCapabilities;
        if (extensionsParameters != null) {
            gnmiParameters = extensionsParameters.getGnmiParameters();
            forceCapabilities = extensionsParameters.augmentation(ForceCapabilities.class);
        } else {
            gnmiParameters = null;
            forceCapabilities = null;
        }
        modelDataList = loadModelDataList(forceCapabilities);
        useModelNamePrefix = Optional.ofNullable(gnmiParameters)
            .map(GnmiParameters::getUseModelNamePrefix);
        overwriteDataType = Optional.ofNullable(gnmiParameters)
            .map(GnmiParameters::getOverwriteDataType);
        pathTarget = Optional.ofNullable(gnmiParameters)
            .map(GnmiParameters::getPathTarget);
    }

    private static Optional<List<Gnmi.ModelData>> loadModelDataList(final ForceCapabilities forceCapabilities) {
        if (forceCapabilities != null && forceCapabilities.getForceCapability() != null) {
            return Optional.of(forceCapabilities.getForceCapability()
                .entrySet()
                .stream()
                .map(Entry::getValue)
                .map(model -> Gnmi.ModelData.newBuilder()
                    .setName(model.getName())
                    .setVersion(model.getVersion().getValue()).build())
                .collect(Collectors.toList()));
        }
        return Optional.empty();
    }

    public Optional<Boolean> getUseModelNamePrefix() {
        return useModelNamePrefix;
    }

    public Optional<GnmiParameters.OverwriteDataType> getOverwriteDataType() {
        return overwriteDataType;
    }

    public Optional<String> getPathTarget() {
        return pathTarget;
    }

    public Optional<List<Gnmi.ModelData>> getModelDataList() {
        return this.modelDataList;
    }
}
