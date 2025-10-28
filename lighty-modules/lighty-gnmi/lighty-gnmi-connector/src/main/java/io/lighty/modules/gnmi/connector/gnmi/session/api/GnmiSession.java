/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.session.api;

import com.google.common.util.concurrent.ListenableFuture;
import gnmi.Gnmi;
import io.grpc.stub.StreamObserver;

/**
 * Interface exposes basic gNMI methods of session. For more information about gNMI see
 * <a href="https://github.com/openconfig/reference/blob/master/rpc/gnmi/gnmi-specification.md">official specification</a>.
 */
public interface GnmiSession {

    /**
     * Retrieve a snapshot of data from the target. A Get RPC requests that the
     * target snapshots a subset of the data tree as specified by the paths
     * included in the message and serializes this to be returned to the
     * client using the specified encoding.
     * Reference: gNMI Specification Section 3.3
     *
     * @param getRequest specify the set of requested elements
     * @return future object represents the result of GET request to target
     */
    ListenableFuture<Gnmi.GetResponse> get(Gnmi.GetRequest getRequest);

    /**
     * Set allows the client to modify the state of data on the target. The
     * paths to modified along with the new values that the client wishes
     * to set the value to.
     * Reference: gNMI Specification Section 3.4
     *
     * @param setRequest specify the set of paths and values which should be updated/replaced/deleted
     * @return future object represents the result of SET request to target
     */
    ListenableFuture<Gnmi.SetResponse> set(Gnmi.SetRequest setRequest);

    /**
     * Capabilities allows the client to retrieve the set of capabilities that
     * is supported by the target. This allows the target to validate the
     * service version that is implemented and retrieve the set of models that
     * the target supports. The models can then be specified in subsequent RPCs
     * to restrict the set of data that is utilized.
     * Reference: gNMI Specification Section 3.2
     *
     * @return future object represents the result of loading target's capabilities
     */
    ListenableFuture<Gnmi.CapabilityResponse> capabilities(Gnmi.CapabilityRequest capabilityRequest);

    /**
     * Subscribe allows a client to request the target to send it values
     * of particular paths within the data tree. These values may be streamed
     * at a particular cadence (STREAM), sent one off on a long-lived channel
     * (POLL), or sent as a one-off retrieval (ONCE).
     * Reference: gNMI Specification Section 3.5
     *
     * @param responseObserver {@link StreamObserver} ("listener") to the responses from the target. This instance will
     *                         receive requested response via {@link StreamObserver#onNext(Object)} method.
     * @return {@link StreamObserver} for requests. Request can be send to target via
     *         {@link StreamObserver#onNext(Object)} method. Response will be then passed to response observer (which
     *         was passed as parameter to this method).
     */
    StreamObserver<Gnmi.SubscribeRequest> subscribe(StreamObserver<Gnmi.SubscribeResponse> responseObserver);

}
