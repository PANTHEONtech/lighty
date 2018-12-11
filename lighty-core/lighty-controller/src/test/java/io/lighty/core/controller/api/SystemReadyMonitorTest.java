/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.api;

import io.lighty.core.controller.impl.services.LightySystemReadyMonitorImpl;
import org.mockito.Mockito;
import org.opendaylight.infrautils.ready.SystemReadyListener;
import org.opendaylight.infrautils.ready.SystemState;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SystemReadyMonitorTest {

    @Test
    public void testSystemBootFailed() {
        SystemReadyListener listener1 = Mockito.mock(SystemReadyListener.class);
        SystemReadyListener listener2 = Mockito.mock(SystemReadyListener.class);
        LightySystemReadyMonitorImpl systemReadyMonitor = new LightySystemReadyMonitorImpl();
        Assert.assertTrue(SystemState.BOOTING.equals(systemReadyMonitor.getSystemState()));
        systemReadyMonitor.registerListener(listener1);
        systemReadyMonitor.registerListener(listener2);
        int i = systemReadyMonitor.onSystemBootFailed();
        Assert.assertEquals(2, i);
        Assert.assertTrue(SystemState.FAILURE.equals(systemReadyMonitor.getSystemState()));
        verify(listener1, times(0)).onSystemBootReady();
        verify(listener2, times(0)).onSystemBootReady();

        SystemReadyListener listener3 = Mockito.mock(SystemReadyListener.class);
        SystemReadyListener listener4 = Mockito.mock(SystemReadyListener.class);
        systemReadyMonitor.registerListener(listener3);
        systemReadyMonitor.registerListener(listener4);
        verify(listener1, times(0)).onSystemBootReady();
        verify(listener2, times(0)).onSystemBootReady();
    }

    @Test
    public void testSystemBootOK() {
        SystemReadyListener listener1 = Mockito.mock(SystemReadyListener.class);
        SystemReadyListener listener2 = Mockito.mock(SystemReadyListener.class);
        LightySystemReadyMonitorImpl systemReadyMonitor = new LightySystemReadyMonitorImpl();
        Assert.assertTrue(SystemState.BOOTING.equals(systemReadyMonitor.getSystemState()));
        systemReadyMonitor.registerListener(listener1);
        systemReadyMonitor.registerListener(listener2);
        int i = systemReadyMonitor.onSystemBootReady();
        Assert.assertEquals(2, i);
        Assert.assertTrue(SystemState.ACTIVE.equals(systemReadyMonitor.getSystemState()));
        verify(listener1, times(1)).onSystemBootReady();
        verify(listener2, times(1)).onSystemBootReady();

        SystemReadyListener listener3 = Mockito.mock(SystemReadyListener.class);
        SystemReadyListener listener4 = Mockito.mock(SystemReadyListener.class);
        systemReadyMonitor.registerListener(listener3);
        systemReadyMonitor.registerListener(listener4);
        verify(listener1, times(1)).onSystemBootReady();
        verify(listener2, times(1)).onSystemBootReady();
    }

}
