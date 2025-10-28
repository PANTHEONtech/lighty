/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.impl;

import com.google.common.util.concurrent.ListenableFuture;
import gnoi.system.SystemGrpc;
import gnoi.system.SystemOuterClass;
import io.grpc.Channel;
import io.lighty.modules.gnmi.connector.gnoi.invokers.api.GnoiSystemInvoker;

public final class GnoiSystemInvokerImpl implements GnoiSystemInvoker {

    private final SystemGrpc.SystemFutureStub futureStub;

    private GnoiSystemInvokerImpl(final SystemGrpc.SystemFutureStub futureStub) {
        this.futureStub = futureStub;
    }

    public static GnoiSystemInvoker fromChannel(final Channel channel) {
        return new GnoiSystemInvokerImpl(SystemGrpc.newFutureStub(channel));
    }

    @Override
    public ListenableFuture<SystemOuterClass.TimeResponse> time(final SystemOuterClass.TimeRequest request) {
        return futureStub.time(request);
    }

    @Override
    public ListenableFuture<SystemOuterClass.RebootResponse> reboot(final SystemOuterClass.RebootRequest request) {
        return futureStub.reboot(request);
    }

    @Override
    public ListenableFuture<SystemOuterClass.SwitchControlProcessorResponse> switchControlProcessor(
            final SystemOuterClass.SwitchControlProcessorRequest request) {
        return futureStub.switchControlProcessor(request);
    }

    @Override
    public ListenableFuture<SystemOuterClass.RebootStatusResponse> rebootStatus(
            final SystemOuterClass.RebootStatusRequest request) {
        return futureStub.rebootStatus(request);
    }

    @Override
    public ListenableFuture<SystemOuterClass.CancelRebootResponse> cancelReboot(
            final SystemOuterClass.CancelRebootRequest request) {
        return futureStub.cancelReboot(request);
    }

}
