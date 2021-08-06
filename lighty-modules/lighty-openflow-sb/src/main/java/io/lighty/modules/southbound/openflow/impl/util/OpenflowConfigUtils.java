/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.openflow.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.southbound.openflow.impl.config.OpenflowpluginConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenflowConfigUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowConfigUtils.class);

    private static final String OFP_CONFIG_ROOT_ELEMENT_NAME = "openflow";

    private OpenflowConfigUtils() {
        // Utility class, constructor hidden.
    }

    @SuppressWarnings("checkstyle:LineLength")
    public static final Set<YangModuleInfo> OFP_MODELS = ImmutableSet.of(
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.async.config.service.rev170619.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.augments.rev150225.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev150203.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.instruction.rev130731.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.arbitrary.bitmask.fields.rev160130.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.multipart.types.rev170112.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.opendaylight.ipv6.arbitrary.bitmask.fields.rev160224.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flat.batch.service.rev160321.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.errors.rev131116.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev150304.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flows.service.rev160314.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.groups.service.rev160315.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.meters.service.rev160316.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.module.config.rev141015.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.node.error.service.rev140410.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.queue.service.rev150305.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.role.service.rev150727.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.service.barrier.common.rev160315.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.service.batch.common.rev160322.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.table.service.rev131026.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.provider.config.rev160510.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.rf.state.rev170713.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.sm.control.rev150812.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.experimenter.message.service.rev151020.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.experimenter.mp.message.service.rev151020.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.experimenter.types.rev151020.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.onf.bundle.service.rev170124.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.onf.rev170124.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.arbitrator.reconcile.service.rev180227.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsal.core.general.entity.rev150930.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.frm.reconciliation.service.rev180227.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.topology.inventory.rev131030.$YangModuleInfoImpl.getInstance()
    );

    /**
     * Create new default configuration for OFP.
     * @return Default OFP configuration.
     */
    public static OpenflowpluginConfiguration getDefaultOfpConfiguration() {
        try (InputStream inputStream = OpenflowConfigUtils.class.getClassLoader()
                .getResourceAsStream("defaultOfpConfig.json")) {
            return getOfpConfiguration(inputStream);
        } catch (ConfigurationException | IOException e) {
            LOG.error("Failed to load  default configuration for Openflow plugin!", e);
        }
        return null;
    }

    /**
     * Read configuration from InputStream representing JSON configuration data.
     * @param jsonConfigInputStream InputStream representing JSON configuration.
     * @return Instance of OFP configuration data.
     * @throws ConfigurationException Thrown if JSON configuration cannot be deserializable to JSON
     *     tree nodes or cannot bind JSON tree node to type.
     */
    public static OpenflowpluginConfiguration getOfpConfiguration(final InputStream jsonConfigInputStream)
            throws ConfigurationException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode configNode;
        try {
            configNode = mapper.readTree(jsonConfigInputStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Cannot deserialize Json content to Json tree nodes", e);
        }
        if (!configNode.has(OFP_CONFIG_ROOT_ELEMENT_NAME)) {
            LOG.warn("Json config does not contain {} element. Using defaults.", OFP_CONFIG_ROOT_ELEMENT_NAME);
            return getDefaultOfpConfiguration();
        }
        final JsonNode ofpNode = configNode.path(OFP_CONFIG_ROOT_ELEMENT_NAME);

        try {
            return mapper.treeToValue(ofpNode, OpenflowpluginConfiguration.class);
        } catch (final JsonProcessingException e) {
            throw new ConfigurationException(String.format("Cannot bind Json tree to type: %s",
                    OpenflowpluginConfiguration.class), e);
        }
    }
}