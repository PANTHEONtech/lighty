/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnmi;

import gnmi.Gnmi;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiCapabilitiesService {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiCapabilitiesService.class);
    private static final String GNMI_VERSION = "0.7.0";
    private static final EnumSet<Gnmi.Encoding> GNMI_ENCODINGS = EnumSet.of(Gnmi.Encoding.JSON,
        Gnmi.Encoding.JSON_IETF);

    private final EffectiveModelContext schemaContext;

    public GnmiCapabilitiesService(final EffectiveModelContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    @SuppressWarnings("UnstableApiUsage")
    public Gnmi.CapabilityResponse getResponse(final Gnmi.CapabilityRequest request) {
        LOG.debug("Received capabilities request {}", request);
        final List<Module> modules = new ArrayList<>(schemaContext.getModules());
        final List<Gnmi.ModelData> modelDataList = new ArrayList<>();
        for (final Module module : modules) {
            final Gnmi.ModelData.Builder builder = Gnmi.ModelData.newBuilder()
                .setOrganization(module.getOrganization().orElse("UNKNOWN-ORGANIZATION"))
                .setName(module.getName());
            if (module.getSemanticVersion().isPresent()) {
                builder.setVersion(module.getSemanticVersion().get().toString());
            } else if (module.getRevision().isPresent()) {
                builder.setVersion(module.getRevision().get().toString());
            }
            modelDataList.add(builder.build());
        }

        return Gnmi.CapabilityResponse.newBuilder()
            .addAllSupportedModels(modelDataList)
            .addAllSupportedEncodings(GNMI_ENCODINGS)
            .setGNMIVersion(GNMI_VERSION)
            .build();
    }
}
