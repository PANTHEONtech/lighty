/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.requests;

import gnmi.Gnmi;
import gnmi.Gnmi.Encoding;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetRequest.Builder;
import gnmi.Gnmi.Path;
import io.lighty.gnmi.southbound.device.connection.ConfigurableParameters;
import io.lighty.gnmi.southbound.device.connection.DeviceConnection;
import io.lighty.gnmi.southbound.mountpoint.codecs.Codec;
import io.lighty.gnmi.southbound.mountpoint.codecs.GnmiCodecException;
import java.util.List;
import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.extensions.parameters.GnmiParameters;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * gNMI GET Request provider with specific DataType.
 * This Provider builds them that way.
 */
public class GnmiGetRequestFactoryImpl implements GnmiGetRequestFactory {

    private final DeviceConnection deviceConnection;
    private final Codec<YangInstanceIdentifier, Gnmi.Path> instanceIdentifierToPathCodec;

    public GnmiGetRequestFactoryImpl(
            final DeviceConnection deviceConnection,
            final Codec<YangInstanceIdentifier, Path> instanceIdentifierToPathCodec) {
        this.deviceConnection = deviceConnection;
        this.instanceIdentifierToPathCodec = instanceIdentifierToPathCodec;
    }

    @Override
    public Gnmi.GetRequest newRequest(final YangInstanceIdentifier path, final Gnmi.GetRequest.DataType datastoreType)
            throws GnmiRequestException {
        try {
            final Builder requestBuilder = GetRequest.newBuilder();
            final ConfigurableParameters parameters = deviceConnection.getConfigurableParameters();
            final Optional<GnmiParameters.OverwriteDataType> overwriteDataType =
                parameters.getOverwriteDataType();
            if (overwriteDataType.isEmpty()) {
                requestBuilder.setType(datastoreType);
            } else if (overwriteDataType.get() != GnmiParameters.OverwriteDataType.NONE) {
                requestBuilder.setType(Gnmi.GetRequest.DataType
                    .valueOf(overwriteDataType.get().getName()));
            }
            final Optional<String> optPathTarget = parameters.getPathTarget();
            optPathTarget.ifPresent(pathTarget -> requestBuilder.setPrefix(Path.newBuilder().setTarget(pathTarget)));

            final Optional<List<Gnmi.ModelData>> optModelDataList = parameters.getModelDataList();
            optModelDataList.ifPresent(requestBuilder::addAllUseModels);

            final Gnmi.Path gnmiPath = instanceIdentifierToPathCodec.apply(path);
            return requestBuilder
                    .setEncoding(Encoding.JSON_IETF)
                    .addPath(gnmiPath).build();
        } catch (GnmiCodecException e) {
            throw new GnmiRequestException(String.format("Cannot convert YangInstanceIdentifier %s to gNMI.Path", path),
                    e);
        }
    }
}
