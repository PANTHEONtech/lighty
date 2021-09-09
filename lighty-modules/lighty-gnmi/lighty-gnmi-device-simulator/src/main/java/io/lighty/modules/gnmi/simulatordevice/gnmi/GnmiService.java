/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnmi;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnmi.Gnmi;
import gnmi.gNMIGrpc;
import io.grpc.stub.StreamObserver;
import io.lighty.modules.gnmi.simulatordevice.yang.YangDataService;
import java.util.EnumSet;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiService extends gNMIGrpc.gNMIImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiService.class);

    private final JsonFormat.Printer jsonPrinter = JsonFormat.printer().includingDefaultValueFields();

    private final GnmiCapabilitiesService gnmiCapabilitiesService;
    private final GnmiCrudService gnmiCrudService;

    public GnmiService(final EffectiveModelContext schemaContext, final YangDataService dataService,
                       @Nullable final Gson gson, @Nullable final EnumSet<Gnmi.Encoding> supportedEncodings) {
        this.gnmiCapabilitiesService = new GnmiCapabilitiesService(schemaContext, supportedEncodings);
        final Gson nonNullGson = Objects.requireNonNullElse(gson, new Gson());
        this.gnmiCrudService = new GnmiCrudService(dataService, schemaContext,  nonNullGson);
    }

    /**
     * <pre>
     * Capabilities allows the client to retrieve the set of capabilities that
     * is supported by the target. This allows the target to validate the
     * service version that is implemented and retrieve the set of models that
     * the target supports. The models can then be specified in subsequent RPCs
     * to restrict the set of data that is utilized.
     * Reference: gNMI Specification Section 3.2
     * </pre>
     */
    @Override
    public void capabilities(final gnmi.Gnmi.CapabilityRequest request,
                             final io.grpc.stub.StreamObserver<gnmi.Gnmi.CapabilityResponse> responseObserver) {
        logMessage("capabilities request", request);
        final Gnmi.CapabilityResponse response = gnmiCapabilitiesService.getResponse(request);
        logMessage("capabilities response", response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * <pre>
     * Retrieve a snapshot of data from the target. A Get RPC requests that the
     * target snapshots a subset of the data tree as specified by the paths
     * included in the message and serializes this to be returned to the
     * client using the specified encoding.
     * Reference: gNMI Specification Section 3.3
     * </pre>
     */
    @SuppressWarnings({"checkstyle:illegalCatch"})
    @Override
    public void get(final gnmi.Gnmi.GetRequest request,
                    final io.grpc.stub.StreamObserver<gnmi.Gnmi.GetResponse> responseObserver) {
        try {
            logMessage("GetRequest received", request);
            validateGetRequest(request);
            final Gnmi.GetResponse response = gnmiCrudService.get(request);
            logMessage("GetResponse ready", response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (final Exception e) {
            responseObserver.onError(e);
        }
    }

    /**
     * <pre>
     * Set allows the client to modify the state of data on the target. The
     * paths to modified along with the new values that the client wishes
     * to set the value to.
     * Reference: gNMI Specification Section 3.4
     * </pre>
     */
    @SuppressWarnings({"checkstyle:illegalCatch"})
    @Override
    public void set(final gnmi.Gnmi.SetRequest request,
                    final io.grpc.stub.StreamObserver<gnmi.Gnmi.SetResponse> responseObserver) {
        try {
            logMessage("SetRequest received", request);
            final Gnmi.SetResponse response = gnmiCrudService.set(request);
            logMessage("SetResponse ready", response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (final Exception e) {
            LOG.error("Failed to process SetRequest: {}", request, e);
            responseObserver.onError(e);
        }
    }

    /**
     * <pre>
     * Subscribe allows a client to request the target to send it values
     * of particular paths within the data tree. These values may be streamed
     * at a particular cadence (STREAM), sent one off on a long-lived channel
     * (POLL), or sent as a one-off retrieval (ONCE).
     * Reference: gNMI Specification Section 3.5
     * </pre>
     */
    @Override
    public StreamObserver<Gnmi.SubscribeRequest> subscribe(StreamObserver<Gnmi.SubscribeResponse> responseObserver) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @SuppressFBWarnings(value = "SLF4J_SIGN_ONLY_FORMAT",
            justification = "logging utility method, hence the {}: {}")
    private void logMessage(final String prefixMessage, final MessageOrBuilder message) {
        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug("{}: {}", prefixMessage, jsonPrinter.print(message));
            } catch (final InvalidProtocolBufferException e) {
                LOG.debug("{} message wasn't logged properly", prefixMessage, e);
            }
        }
    }

    private void validateGetRequest(final Gnmi.GetRequest request) {
        if (request.getEncoding() != Gnmi.Encoding.JSON_IETF && request.getEncoding() != Gnmi.Encoding.JSON) {
            throw new UnsupportedOperationException("Simulator only supports JSON_IETF encoding.");
        }
    }
}
