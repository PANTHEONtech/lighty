package io.lighty.core.controller.api;

import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Yang module registration service for global schema context.
 *
 * @author juraj.veverka
 */
public interface LightyModuleRegistryService {

    /**
     * Register an instance of Yang module into global schema context.
     * @param yangModuleInfo
     * @return
     */
    ObjectRegistration<YangModuleInfo> registerModuleInfo(final YangModuleInfo yangModuleInfo);

}
