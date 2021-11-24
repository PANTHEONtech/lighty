/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema.loader.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.opendaylight.yangtools.concepts.SemVer;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangModelDependencyInfo;

public class YangLoadModelUtil {

    private final Revision modelRevision;
    private final SemVer modelSemVer;
    private final String modelBody;
    private final String modelName;

    public YangLoadModelUtil(final YangTextSchemaSource yangTextSchemaSource, final InputStream yangTextStream)
            throws YangSyntaxErrorException, IOException {
        final YangModelDependencyInfo yangModelDependencyInfo =
                YangModelDependencyInfo.forYangText(yangTextSchemaSource);
        // If revision is present in fileName, prefer that
        this.modelRevision = yangTextSchemaSource.getIdentifier().getRevision()
                .or(yangModelDependencyInfo::getRevision).orElse(null);
        this.modelSemVer = yangModelDependencyInfo.getSemanticVersion().orElse(null);
        this.modelBody = IOUtils.toString(yangTextStream, Charset.defaultCharset());
        this.modelName = yangModelDependencyInfo.getName();
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
}

