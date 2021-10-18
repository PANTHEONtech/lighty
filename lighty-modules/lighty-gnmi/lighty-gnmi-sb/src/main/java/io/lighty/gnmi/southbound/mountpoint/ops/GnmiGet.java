/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.ops;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import gnmi.Gnmi;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.lighty.gnmi.southbound.device.session.provider.GnmiSessionProvider;
import io.lighty.gnmi.southbound.mountpoint.codecs.BiCodec;
import io.lighty.gnmi.southbound.mountpoint.codecs.GnmiCodecException;
import io.lighty.gnmi.southbound.mountpoint.requests.GnmiGetRequestFactory;
import io.lighty.gnmi.southbound.mountpoint.requests.GnmiRequestException;
import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides gNMI GET operation.
 */
public class GnmiGet {
    private static final Logger LOG = LoggerFactory.getLogger(GnmiGet.class);

    private final BiCodec<Gnmi.GetResponse, YangInstanceIdentifier,
            Optional<NormalizedNode>> getResponseToNormalizedNodeCodec;
    private final GnmiSessionProvider sessionProvider;
    private final GnmiGetRequestFactory getRequestFactory;
    private final NodeId nodeId;

    public GnmiGet(final GnmiSessionProvider sessionProvider, final NodeId nodeId,
                   final BiCodec<Gnmi.GetResponse, YangInstanceIdentifier,
                           Optional<NormalizedNode>> getResponseNormalizedNodeCodec,
                   final GnmiGetRequestFactory gnmiGetRequestFactory) {
        this.sessionProvider = sessionProvider;
        this.nodeId = nodeId;
        this.getResponseToNormalizedNodeCodec = getResponseNormalizedNodeCodec;
        this.getRequestFactory = gnmiGetRequestFactory;
    }

    public ListenableFuture<Optional<NormalizedNode>> readOperationalData(final YangInstanceIdentifier path) {
        return readData(Gnmi.GetRequest.DataType.STATE, path);
    }

    public ListenableFuture<Optional<NormalizedNode>> readConfigurationData(final YangInstanceIdentifier path) {
        return readData(Gnmi.GetRequest.DataType.CONFIG, path);
    }

    public ListenableFuture<Optional<NormalizedNode>> readData(final Gnmi.GetRequest.DataType dataType,
                                                                     final YangInstanceIdentifier path) {
        final SettableFuture<Optional<NormalizedNode>> ret = SettableFuture.create();
        try {
            final Gnmi.GetRequest request = getRequestFactory.newRequest(path, dataType);
            LOG.debug("[{}] Sending gNMI GetRequest:\n{}", nodeId.getValue(), request);
            final ListenableFuture<Gnmi.GetResponse> getResponseFuture = sessionProvider.getGnmiSession().get(request);

            Futures.addCallback(getResponseFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(Gnmi.GetResponse getResponse) {
                    try {
                        LOG.debug("[{}] Got gNMI GetResponse:\n{}", nodeId.getValue(), getResponse);
                        final Optional<NormalizedNode> optNormalizedNode = getResponseToNormalizedNodeCodec
                                .apply(getResponse, path);
                        LOG.debug("[{}] Parsed Normalized nodes from gNMI GetResponse:\n{}", nodeId.getValue(),
                                optNormalizedNode.isPresent()
                                ? optNormalizedNode.get() : "NONE");
                        ret.set(optNormalizedNode);
                    } catch (GnmiCodecException ex) {
                        LOG.warn("[{}] Can't convert gNMI getResponse {} to normalized nodes", nodeId.getValue(),
                                getResponse);
                        ret.setException(ex);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable instanceof StatusRuntimeException) {
                        final StatusRuntimeException grpcException = (StatusRuntimeException) throwable;
                        // Status.NOT_FOUND could mean that STATE/CONFIG data was not found on path
                        if (grpcException.getStatus().getCode().toStatus() == Status.NOT_FOUND) {
                            ret.set(Optional.empty());
                        } else {
                            ret.setException(grpcException);
                        }
                    }
                    ret.setException(throwable);
                }
            }, MoreExecutors.directExecutor());
        } catch (GnmiRequestException ex) {
            LOG.warn("[{}] Can't make gNMI GET request", nodeId.getValue(), ex);
            ret.setException(ex);
        }
        return ret;
    }

    public NodeId getNodeId() {
        return nodeId;
    }
}
