/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.loader.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.opendaylight.yangtools.concepts.SemVer;
import org.opendaylight.yangtools.openconfig.model.api.OpenConfigStatements;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.ir.IRArgument.Single;
import org.opendaylight.yangtools.yang.ir.IRStatement;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.TextToIRTransformer;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangIRSourceInfoExtractor;

public class YangLoadModelUtil {

    private static final String OPENCONFIG_VERSION = OpenConfigStatements.OPENCONFIG_VERSION.getStatementName()
            .getLocalName();
    private final Revision modelRevision;
    private final SemVer modelSemVer;
    private final String modelBody;
    private final String modelName;

    public YangLoadModelUtil(final YangTextSource yangTextSchemaSource, final InputStream yangTextStream)
            throws YangSyntaxErrorException, IOException {
        final var irSchemaSource = TextToIRTransformer.transformText(yangTextSchemaSource);
        final var semanticVersion = getSemVer(irSchemaSource.statement());

        final var yangModelDependencyInfo =
                YangIRSourceInfoExtractor.forYangText(yangTextSchemaSource);
        // If revision is present in fileName, prefer that
        this.modelRevision = yangModelDependencyInfo.revisions().stream()
                .max(Comparator.comparing(revision -> LocalDate.parse(revision.toString(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .orElse(null);
        this.modelSemVer = semanticVersion.orElse(null);
        this.modelBody = IOUtils.toString(yangTextStream, StandardCharsets.UTF_8);
        this.modelName = yangModelDependencyInfo.sourceId().name().getLocalName();
    }

    public String getVersionToStore() {
        if (modelSemVer != null) {
            return modelSemVer.toString();
        } else if (modelRevision != null) {
            return modelRevision.toString();
        } else {
            return "";
        }
    }

    public Revision getModelRevision() {
        return modelRevision;
    }

    public SemVer getModelSemVer() {
        return modelSemVer;
    }

    public String getModelBody() {
        return modelBody;
    }

    public String getModelName() {
        return modelName;
    }

    private static Optional<SemVer> getSemVer(final IRStatement stmt) {
        for (final var substatement : stmt.statements()) {
            if (OPENCONFIG_VERSION.equals(substatement.keyword().identifier())) {
                final var argument = substatement.argument();
                if (argument instanceof Single) {
                    return Optional.of(SemVer.valueOf(((Single) argument).string()));
                }
            }
        }
        return Optional.empty();
    }
}
