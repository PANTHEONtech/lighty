/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.services;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceRegistration;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusSummary;
import org.opendaylight.infrautils.ready.SystemReadyMonitor;
import org.opendaylight.infrautils.ready.SystemState;

public class LightyDiagStatusServiceImpl implements DiagStatusService {
    private static final String STATE_DESCRIPTION = "Service registration";

    private final Map<String, ServiceDescriptor> descriptors = new ConcurrentHashMap<>();
    private final SystemReadyMonitor systemReadyMonitor;

    public LightyDiagStatusServiceImpl(final SystemReadyMonitor systemReadyMonitor) {
        this.systemReadyMonitor = requireNonNull(systemReadyMonitor);
    }

    @Override
    public ServiceRegistration register(final String serviceIdentifier) {
        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(serviceIdentifier,
                ServiceState.STARTING, STATE_DESCRIPTION);
        descriptors.put(serviceIdentifier, serviceDescriptor);

        return new LightyDiagStatusServiceRegistration(serviceIdentifier);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor(final String serviceIdentifier) {
        return descriptors.get(serviceIdentifier);
    }

    @Override
    public Set<ServiceDescriptor> getAllServiceDescriptors() {
        return new HashSet<>(descriptors.values());
    }

    @Override
    public ServiceStatusSummary getServiceStatusSummary() {
        SystemState systemState = systemReadyMonitor.getSystemState();
        Set<ServiceDescriptor> serviceDescriptors = getAllServiceDescriptors();
        return new ServiceStatusSummary(isOperational(systemState, serviceDescriptors),
                systemState, systemReadyMonitor.getFailureCause(), serviceDescriptors);
    }

    private static boolean isOperational(final SystemState systemState,
            final Collection<ServiceDescriptor> serviceDescriptors) {
        if (!systemState.equals(SystemState.ACTIVE)) {
            return false;
        }
        for (ServiceDescriptor sd : serviceDescriptors) {
            if (sd.getServiceState() != ServiceState.OPERATIONAL) {
                return false;
            }
        }
        return true;
    }

    private final class LightyDiagStatusServiceRegistration implements ServiceRegistration {

        private final String descriptorId;

        LightyDiagStatusServiceRegistration(final String descriptorId) {
            this.descriptorId = descriptorId;
        }

        @Override
        public void report(ServiceDescriptor serviceDescriptor) {
            descriptors.put(serviceDescriptor.getModuleServiceName(), serviceDescriptor);
        }

        @Override
        public void close() {
            descriptors.remove(descriptorId);
        }
    }
}
