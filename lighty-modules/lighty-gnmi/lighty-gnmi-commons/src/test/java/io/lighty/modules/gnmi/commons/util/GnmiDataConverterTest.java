/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.commons.util;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

class GnmiDataConverterTest {
    private static final String YANG_MODEL_1_FILE_NAME = "rootModel1.yang";
    private static final String YANG_MODEL_2_FILE_NAME = "rootModel2.yang";

    @Test
    public void findCorrectRootYangModel() throws ReactorException, YangSyntaxErrorException, IOException {
        EffectiveModelContext schemaContext = prepareSchemaWithMultipleRootContainersWithSameName();
        final Optional<? extends Module> rootModel1
                = DataConverter.findModuleByElement("root-model-1:root-container", schemaContext);
        Assertions.assertTrue(rootModel1.isPresent());
        final Optional<? extends Module> rootModel2
                = DataConverter.findModuleByElement("root-model-2:root-container", schemaContext);
        Assertions.assertTrue(rootModel2.isPresent());
        Assertions.assertNotEquals(rootModel1, rootModel2);

        final Optional<? extends Module> unspecifiedRootModule
                = DataConverter.findModuleByElement("root-container", schemaContext);
        Assertions.assertTrue(unspecifiedRootModule.isEmpty());
    }

    private static EffectiveModelContext prepareSchemaWithMultipleRootContainersWithSameName()
            throws ReactorException, IOException, YangSyntaxErrorException {

        final CrossSourceStatementReactor.BuildAction buildAction = RFC7950Reactors.defaultReactorBuilder()
                .build().newBuild();

        for (String modelName : Arrays.asList(YANG_MODEL_1_FILE_NAME, YANG_MODEL_2_FILE_NAME)) {
            final YangStatementStreamSource yangModelRootSource = YangStatementStreamSource.create(
                    YangTextSchemaSource.delegateForByteSource(
                            YangTextSchemaSource.identifierFromFilename(modelName),
                            ByteSource.wrap(Thread.currentThread().getContextClassLoader()
                                    .getResourceAsStream("test/schema/" + modelName).readAllBytes())));

            buildAction.addSource(yangModelRootSource);
        }

        return buildAction.buildEffective();
    }
}
