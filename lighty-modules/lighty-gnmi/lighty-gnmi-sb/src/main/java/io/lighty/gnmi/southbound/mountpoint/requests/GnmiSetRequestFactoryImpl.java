/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.requests;

import gnmi.Gnmi;
import gnmi.Gnmi.SetRequest.Builder;
import gnmi.Gnmi.Update;
import io.lighty.gnmi.southbound.mountpoint.codecs.BiCodec;
import io.lighty.gnmi.southbound.mountpoint.codecs.Codec;
import io.lighty.gnmi.southbound.mountpoint.codecs.GnmiCodecException;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiSetRequestFactoryImpl implements SetRequestFactory {

    private static final String FAILED_TO_CONVERT =
        "Failed to convert YangInstanceIdentifier %s and NormalizedNode %s to Gnmi.Update";
    private static final Logger LOG = LoggerFactory.getLogger(GnmiSetRequestFactoryImpl.class);

    private final Codec<YangInstanceIdentifier, Gnmi.Path> instanceIdentifierToPathCodec;
    private final BiCodec<YangInstanceIdentifier, NormalizedNode, Gnmi.Update> updateCodec;

    public GnmiSetRequestFactoryImpl(
            final Codec<YangInstanceIdentifier, Gnmi.Path> instanceIdentifierToPathCodec,
            final BiCodec<YangInstanceIdentifier, NormalizedNode, Gnmi.Update> updateCodec) {
        this.instanceIdentifierToPathCodec = instanceIdentifierToPathCodec;
        this.updateCodec = updateCodec;
    }

    @Override
    public Gnmi.SetRequest newRequest(
            final List<ImmutablePair<YangInstanceIdentifier, NormalizedNode>> replaceList,
            final List<ImmutablePair<YangInstanceIdentifier, NormalizedNode>> updateList,
            final List<YangInstanceIdentifier> deleteList) throws GnmiRequestException {

        final Gnmi.SetRequest.Builder setRequestBuilder = Gnmi.SetRequest.newBuilder();

        // REPLACE
        for (ImmutablePair<YangInstanceIdentifier, NormalizedNode> toConvert : replaceList) {
            try {
                setRequestBuilder.addReplace(updateCodec.apply(toConvert.left, toConvert.right));
            } catch (GnmiCodecException e) {
                throw new GnmiRequestException(String.format(FAILED_TO_CONVERT, toConvert.left, toConvert.right), e);
            }
        }

        // UPDATE
        for (ImmutablePair<YangInstanceIdentifier, NormalizedNode> toConvert : updateList) {
            try {
                setRequestBuilder.addUpdate(updateCodec.apply(toConvert.left, toConvert.right));
            } catch (GnmiCodecException e) {
                throw new GnmiRequestException(String.format(FAILED_TO_CONVERT, toConvert.left, toConvert.right), e);
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
        return filterDataStorePrepareRequest(setRequestBuilder).build();
    }

    /**
     * When PUT or PATCH request is sent through RESTCONF to gNMI device, it can produce additional merge request.
     * Inside this merge request it is intended to prepare datastore if data with specific path does not exist yet. If
     * path contains list than it create merge request which will try to create this list entity before actual write
     * request. In gNMI this behavior is not required and if part of datastore prepare request is list node than this
     * request can crash and prevent user to write data.
     *
     * @param requestBuilder {@link Builder} contains update data.
     * @return {@link Builder} with removed datastore prepare updates.
     */
    private Builder filterDataStorePrepareRequest(final Builder requestBuilder) {
        if (requestBuilder.getUpdateCount() == 0) {
            return requestBuilder;
        }
        // Expect one request per commit and if there is two request and only one is the update request
        // than the update request should be data store prepare request
        if (requestBuilder.getReplaceCount() == 1 && requestBuilder.getUpdateCount() == 1
                && isLeftValueDataStorePrepareRequest(requestBuilder.getUpdate(0), requestBuilder.getReplace(0))) {
            LOG.debug("Remove Data store prepare request [{}]", requestBuilder.getUpdate(0));
            requestBuilder.removeUpdate(0);
            return requestBuilder;
        }
        if (requestBuilder.getUpdateCount() == 2) {
            if (isLeftValueDataStorePrepareRequest(requestBuilder.getUpdate(0), requestBuilder.getUpdate(1))) {
                LOG.debug("Remove Data store prepare request [{}]", requestBuilder.getUpdate(0));
                requestBuilder.removeUpdate(0);
                return requestBuilder;
            }
            if (isLeftValueDataStorePrepareRequest(requestBuilder.getUpdate(1), requestBuilder.getUpdate(0))) {
                LOG.debug("Remove Data store prepare request [{}]", requestBuilder.getUpdate(1));
                requestBuilder.removeUpdate(1);
                return requestBuilder;
            }
        }
        return requestBuilder;
    }

    private boolean isLeftValueDataStorePrepareRequest(final Update leftValue, final Update rightValue) {
        // If update ietf value is empty then it is preparing data store request
        if (isJsonIetfValueEmpty(leftValue)) {
            return true;
        }
        if (isJsonIetfValueEmpty(rightValue)) {
            return false;
        }
        final int leftValueElemCount = leftValue.getPath().getElemCount();
        // DataStore preparing request have less elements in path
        if (leftValueElemCount >= rightValue.getPath().getElemCount()) {
            return false;
        }
        for (int i = 0; i < leftValueElemCount; i++) {
            if (!leftValue.getPath().getElem(i).equals(rightValue.getPath().getElem(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isJsonIetfValueEmpty(final Update value) {
        final String jsonValue = value.getVal().getJsonIetfVal().toStringUtf8()
                .replace("{", "")
                .replace("}", "");
        return jsonValue.isBlank();
    }
}
