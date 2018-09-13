/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.controller.impl.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceRegistration;
import org.opendaylight.infrautils.diagstatus.ServiceState;

public class LightyDiagStatusServiceImpl implements DiagStatusService {

    private final static String STATE_DESCRIPTION = "Service registration";

    private Map<String, ServiceDescriptor> descriptors = new ConcurrentHashMap<>();

    @Override
    public ServiceRegistration register(String serviceIdentifier) {
        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(serviceIdentifier,
                ServiceState.STARTING, STATE_DESCRIPTION);
        descriptors.put(serviceIdentifier, serviceDescriptor);

        return new LightyDiagStatusServiceRegistration(serviceIdentifier);
    }

    @Override
    public void report(ServiceDescriptor serviceDescriptor) {
        descriptors.put(serviceDescriptor.getModuleServiceName(), serviceDescriptor);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor(String serviceIdentifier) {
        return descriptors.get(serviceIdentifier);
    }

    @Override
    public Collection<ServiceDescriptor> getAllServiceDescriptors() {
        final Collection<ServiceDescriptor> serviceDescriptors = new ArrayList<>();
        for (String key : descriptors.keySet()) {
            serviceDescriptors.add(descriptors.get(key));
        }
        return serviceDescriptors;
    }

    @Override
    public String getAllServiceDescriptorsAsJSON() {
        final Collection<ServiceDescriptor> allServiceDescriptors = getAllServiceDescriptors();
        if (allServiceDescriptors.isEmpty()) {
            return "{}";
        } else {
            ObjectMapper mapper = new ObjectMapper();
            final ArrayNode arrayNode = mapper.createArrayNode();
            for (ServiceDescriptor status : allServiceDescriptors) {
                ObjectNode serviceDescriptor = mapper.createObjectNode();
                serviceDescriptor.put("serviceName", status.getModuleServiceName());
                serviceDescriptor.put("effectiveStatus", status.getServiceState().name());
                serviceDescriptor.put("reportedStatusDescription", status.getStatusDesc());
                serviceDescriptor.put("statusTimestamp", status.getTimestamp().toString());
                arrayNode.add(serviceDescriptor);
            }
            return arrayNode.toString();
        }
    }

    @Override
    public boolean isOperational() {
        for (ServiceDescriptor sd : getAllServiceDescriptors()) {
            if(sd.getServiceState() != ServiceState.OPERATIONAL) {
                return false;
            }
        }
        return true;
    }

    private final class LightyDiagStatusServiceRegistration implements ServiceRegistration {

        private final String descriptorId;

        LightyDiagStatusServiceRegistration(String descriptorId) {
            this.descriptorId = descriptorId;
        }

        @Override
        public void unregister() throws IllegalStateException {
            descriptors.remove(descriptorId);
        }
    }
}
