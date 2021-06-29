/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnoi.invokers.api;

import com.google.common.util.concurrent.ListenableFuture;
import gnoi.system.SystemOuterClass;

/**
 * Interface exposing gnoi-system methods.
 */
public interface GnoiSystemInvoker {


    ListenableFuture<SystemOuterClass.TimeResponse> time(SystemOuterClass.TimeRequest request);

    ListenableFuture<SystemOuterClass.RebootResponse> reboot(SystemOuterClass.RebootRequest request);

    ListenableFuture<SystemOuterClass.SwitchControlProcessorResponse> switchControlProcessor(
            SystemOuterClass.SwitchControlProcessorRequest request);

    ListenableFuture<SystemOuterClass.RebootStatusResponse> rebootStatus(SystemOuterClass.RebootStatusRequest request);

    ListenableFuture<SystemOuterClass.CancelRebootResponse> cancelReboot(SystemOuterClass.CancelRebootRequest request);
}
