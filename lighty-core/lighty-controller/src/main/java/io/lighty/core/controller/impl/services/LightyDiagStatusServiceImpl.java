package io.lighty.core.controller.impl.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceRegistration;
import org.opendaylight.infrautils.diagstatus.ServiceState;

public class LightyDiagStatusServiceImpl implements DiagStatusService {

    private final static String STATE_DESCRIPTION = "Service registration";

    private Map<String, ServiceDescriptor> descriptors = new HashMap<>();

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
