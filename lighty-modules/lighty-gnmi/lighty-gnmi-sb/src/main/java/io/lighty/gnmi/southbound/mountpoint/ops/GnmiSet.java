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
import io.lighty.gnmi.southbound.device.session.provider.GnmiSessionProvider;
import io.lighty.gnmi.southbound.mountpoint.requests.GnmiRequestException;
import io.lighty.gnmi.southbound.mountpoint.requests.SetRequestFactory;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides gNMI SET operation.
 */
public class GnmiSet {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiSet.class);
    private final GnmiSessionProvider sessionProvider;
    private final SetRequestFactory setRequestFactory;
    private final NodeId nodeId;

    public GnmiSet(final GnmiSessionProvider sessionProvider,
                   final SetRequestFactory gnmiSetRequestFactory,
                   final NodeId nodeId) {
        this.sessionProvider = sessionProvider;
        this.setRequestFactory = gnmiSetRequestFactory;
        this.nodeId = nodeId;

    }

    public ListenableFuture<CommitInfo> set(
            final List<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>> replaceList,
            final List<ImmutablePair<YangInstanceIdentifier, NormalizedNode<?, ?>>> updateList,
            final List<YangInstanceIdentifier> deleteList) {

        final SettableFuture<CommitInfo> ret = SettableFuture.create();
        try {
            final Gnmi.SetRequest request = setRequestFactory.newRequest(replaceList, updateList, deleteList);
            LOG.debug("[{}] Sending gNMI SetRequest:\n{}", nodeId.getValue(), request);
            final ListenableFuture<Gnmi.SetResponse> setResponseFuture = sessionProvider.getGnmiSession().set(request);
            Futures.addCallback(setResponseFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(Gnmi.@Nullable SetResponse setResponse) {
                    LOG.debug("[{}] SetResponse: {}", nodeId.getValue(), setResponse);
                    ret.set(CommitInfo.empty());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOG.error("[{}] Gnmi.SET to device failed!", nodeId.getValue());
                    ret.setException(throwable);
                }
            }, MoreExecutors.directExecutor());


        } catch (GnmiRequestException ex) {
            LOG.warn("[{}] Can't make gNMI SET request", nodeId.getValue(), ex);
            ret.setException(ex);
        }
        return ret;

    }

    public NodeId getNodeId() {
        return nodeId;
    }
}
