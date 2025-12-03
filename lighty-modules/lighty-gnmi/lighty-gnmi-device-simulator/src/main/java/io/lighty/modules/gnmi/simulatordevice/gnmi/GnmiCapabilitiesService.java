/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnmi;

import static org.opendaylight.yangtools.openconfig.model.api.OpenConfigStatements.OPENCONFIG_VERSION;

import gnmi.Gnmi;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.SemVer;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.ri.stmt.impl.decl.UnrecognizedStatementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnmiCapabilitiesService {

    private static final Logger LOG = LoggerFactory.getLogger(GnmiCapabilitiesService.class);
    private static final String GNMI_VERSION = "0.7.0";

    private final EffectiveModelContext schemaContext;
    private final EnumSet<Gnmi.Encoding> supportedEncodings;

    public GnmiCapabilitiesService(final EffectiveModelContext schemaContext,
                                   @Nullable final EnumSet<Gnmi.Encoding> supportedEncodings) {
        this.schemaContext = schemaContext;
        this.supportedEncodings = Objects.requireNonNullElse(supportedEncodings,
                EnumSet.of(Gnmi.Encoding.JSON, Gnmi.Encoding.JSON_IETF));
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
            final Optional<SemVer> optSemVer = getSemVer(module);
            if (optSemVer.isPresent()) {
                builder.setVersion(optSemVer.orElseThrow().toString());
            } else if (module.getRevision().isPresent()) {
                builder.setVersion(module.getRevision().orElseThrow().toString());
            }
            modelDataList.add(builder.build());
        }

        return Gnmi.CapabilityResponse.newBuilder()
            .addAllSupportedModels(modelDataList)
            .addAllSupportedEncodings(supportedEncodings)
            .setGNMIVersion(GNMI_VERSION)
            .build();
    }

    private static Optional<SemVer> getSemVer(final Module module) {
        final var declared = module.asEffectiveStatement().getDeclared();
        if (declared == null) {
            return Optional.empty();
        }

        return declared.declaredSubstatements(UnrecognizedStatementImpl.class).stream()
                .filter(t -> OPENCONFIG_VERSION.getStatementName()
                        .equals(t.statementDefinition().getStatementName().withoutRevision()))
                .findFirst()
                .map(stmt -> SemVer.valueOf(stmt.argument().toString()));
    }
}
