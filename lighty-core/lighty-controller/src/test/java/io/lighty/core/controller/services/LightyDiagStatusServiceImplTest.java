/*
 * Copyright (c) 2019 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.services;

import io.lighty.core.controller.impl.services.LightyDiagStatusServiceImpl;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceRegistration;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusSummary;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.mockito.Mockito.when;

public class LightyDiagStatusServiceImplTest {

    private static final String TEST_SERVICE = "testService";
    private static final String TEST_SERVICE_2 = "testService2";
    private static final String TEST_SERVICE_2_INFO = "test Service 2 description";
    DiagStatusService diagStatusService;

    @Mock
    private SystemReadyMonitor systemReadyMonitor;

    @BeforeClass
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(systemReadyMonitor.getSystemState()).thenReturn(SystemState.ACTIVE);
    }

    @Test
    public void registerTest() {
        diagStatusService = new LightyDiagStatusServiceImpl(systemReadyMonitor);
        ServiceRegistration serviceRegistration = diagStatusService.register(TEST_SERVICE);
        diagStatusService.report(new ServiceDescriptor(TEST_SERVICE_2, ServiceState.STARTING, TEST_SERVICE_2_INFO));

        Assert.assertEquals(diagStatusService.getAllServiceDescriptors().size(), 2);
        Assert.assertNotNull(diagStatusService.getServiceDescriptor(TEST_SERVICE));
        Assert.assertNotNull(diagStatusService.getServiceDescriptor(TEST_SERVICE_2));

        validateServiceStatus(diagStatusService.getServiceStatusSummary(), false, ServiceState.STARTING);

        serviceRegistration.unregister();
        Assert.assertEquals(diagStatusService.getAllServiceDescriptors().size(), 1);
    }

    @Test
    public void reportTest() {
        diagStatusService = new LightyDiagStatusServiceImpl(systemReadyMonitor);
        ServiceRegistration serviceRegistration = diagStatusService.register(TEST_SERVICE_2);

        Assert.assertEquals(diagStatusService.getAllServiceDescriptors().size(), 1);
        Assert.assertNotNull(diagStatusService.getServiceDescriptor(TEST_SERVICE_2));

        validateServiceStatus(diagStatusService.getServiceStatusSummary(), false, ServiceState.STARTING);
        diagStatusService.report(new ServiceDescriptor(TEST_SERVICE_2, ServiceState.OPERATIONAL, TEST_SERVICE_2_INFO));
        validateServiceStatus(diagStatusService.getServiceStatusSummary(), true, ServiceState.OPERATIONAL);

        serviceRegistration.unregister();
        Assert.assertEquals(diagStatusService.getAllServiceDescriptors().size(), 0);
    }

    private void validateServiceStatus(ServiceStatusSummary serviceStatusSummary, boolean operState,
                                       ServiceState srv_state) {
        Collection<ServiceDescriptor> statusSummary;
        Assert.assertEquals(serviceStatusSummary.isOperational(), operState);
        Assert.assertEquals(serviceStatusSummary.getSystemReadyState(), SystemState.ACTIVE);
        statusSummary = serviceStatusSummary.getStatusSummary();

        for (ServiceDescriptor srv_desc : statusSummary) {
            Assert.assertEquals(srv_desc.getServiceState(), srv_state);
        }
    }
}
