/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.requests;

import gnmi.Gnmi;
import io.lighty.gnmi.southbound.mountpoint.codecs.BiCodec;
import io.lighty.gnmi.southbound.mountpoint.codecs.Codec;
import io.lighty.gnmi.southbound.mountpoint.codecs.GnmiCodecException;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class GnmiSetRequestFactoryImpl implements SetRequestFactory {

    private final Codec<YangInstanceIdentifier, Gnmi.Path> instanceIdentifierToPathCodec;
    private final BiCodec<YangInstanceIdentifier, NormalizedNode<?, ?>, Gnmi.Update> updateCodec;

    public GnmiSetRequestFactoryImpl(
            final Codec<YangInstanceIdentifier, Gnmi.Path> instanceIdentifierToPathCodec,
            final BiCodec<YangInstanceIdentifier, NormalizedNode<?, ?>, Gnmi.Update> updateCodec) {
        this.instanceIdentifierToPathCodec = instanceIdentifierToPathCodec;
        this.updateCodec = updateCodec;
    }

    @Override
    public Gnmi.SetRequest newRequest(
            final List<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>> replaceList,
            final List<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>> updateList,
            final List<YangInstanceIdentifier> deleteList) throws GnmiRequestException {

        final Gnmi.SetRequest.Builder setRequestBuilder = Gnmi.SetRequest.newBuilder();

        // REPLACE
        for (ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>> toConvert : replaceList) {
            try {
                setRequestBuilder.addReplace(updateCodec.apply(toConvert.left, toConvert.right));
            } catch (GnmiCodecException e) {
                throw new GnmiRequestException(String.format("Failed to convert YangInstanceIdentifier %s and"
                        + " NormalizedNode %s to Gnmi.Update", toConvert.left, toConvert.right), e);
            }
        }

        // UPDATE
        for (ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>> toConvert : updateList) {
            try {
                setRequestBuilder.addUpdate(updateCodec.apply(toConvert.left, toConvert.right));
            } catch (GnmiCodecException e) {
                throw new GnmiRequestException(String.format("Failed to convert YangInstanceIdentifier %s and"
                        + " NormalizedNode %s to Gnmi.Update", toConvert.left, toConvert.right), e);
            }
        }
        // DELETE
        for (YangInstanceIdentifier identifier : deleteList) {
            try {
                setRequestBuilder.addDelete(instanceIdentifierToPathCodec.apply(identifier));
            } catch (GnmiCodecException e) {
                throw new GnmiRequestException(String.format("Failed to convert YangInstanceIdentifier %s and"
                        + " to Gnmi.Path", identifier), e);
            }
        }
        return setRequestBuilder.build();
    }

}
