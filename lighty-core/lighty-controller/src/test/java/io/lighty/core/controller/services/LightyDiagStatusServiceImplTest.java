/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import io.lighty.core.controller.impl.services.LightyDiagStatusServiceImpl;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceRegistration;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusSummary;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;

class LightyDiagStatusServiceImplTest {

    private static final String TEST_SERVICE = "testService";
    private static final String TEST_SERVICE_2 = "testService2";
    private static final String TEST_SERVICE_2_INFO = "test Service 2 description";
    DiagStatusService diagStatusService;

    @Mock
    private SystemReadyMonitor systemReadyMonitor;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        when(systemReadyMonitor.getSystemState()).thenReturn(SystemState.ACTIVE);
    }

    @Test
    void registerTest() {
        diagStatusService = new LightyDiagStatusServiceImpl(systemReadyMonitor);
        final ServiceRegistration serviceRegistration = diagStatusService.register(TEST_SERVICE);
        diagStatusService.register(TEST_SERVICE).report(
            new ServiceDescriptor(TEST_SERVICE_2, ServiceState.STARTING, TEST_SERVICE_2_INFO));

        assertEquals(2, diagStatusService.getAllServiceDescriptors().size());
        assertNotNull(diagStatusService.getServiceDescriptor(TEST_SERVICE));
        assertNotNull(diagStatusService.getServiceDescriptor(TEST_SERVICE_2));

        validateServiceStatus(diagStatusService.getServiceStatusSummary(), false, ServiceState.STARTING);

        serviceRegistration.close();
        assertEquals(1, diagStatusService.getAllServiceDescriptors().size());
    }

    @Test
    void reportTest() {
        diagStatusService = new LightyDiagStatusServiceImpl(systemReadyMonitor);
        final ServiceRegistration serviceRegistration = diagStatusService.register(TEST_SERVICE_2);

        assertEquals(1, diagStatusService.getAllServiceDescriptors().size());
        assertNotNull(diagStatusService.getServiceDescriptor(TEST_SERVICE_2));

        validateServiceStatus(diagStatusService.getServiceStatusSummary(), false, ServiceState.STARTING);
        diagStatusService.register(TEST_SERVICE_2).report(
            new ServiceDescriptor(TEST_SERVICE_2, ServiceState.OPERATIONAL, TEST_SERVICE_2_INFO));
        validateServiceStatus(diagStatusService.getServiceStatusSummary(), true, ServiceState.OPERATIONAL);

        serviceRegistration.close();
        assertEquals(0, diagStatusService.getAllServiceDescriptors().size());
    }

    private static void validateServiceStatus(final ServiceStatusSummary serviceStatusSummary, final boolean operState,
            final ServiceState srvState) {
        Collection<ServiceDescriptor> statusSummary;
        assertEquals(operState, serviceStatusSummary.isOperational());
        assertEquals(SystemState.ACTIVE, serviceStatusSummary.getSystemReadyState());
        statusSummary = serviceStatusSummary.getStatusSummary();

        for (ServiceDescriptor srvDesc : statusSummary) {
            assertEquals(srvState, srvDesc.getServiceState());
        }
    }
}