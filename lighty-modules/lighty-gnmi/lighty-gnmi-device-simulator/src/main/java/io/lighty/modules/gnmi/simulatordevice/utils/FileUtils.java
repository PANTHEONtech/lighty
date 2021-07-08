/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

public final class FileUtils {

    private FileUtils() {
        //Utility class
    }

    public static InputStream getResourceAsStream(final String resource) {
        return FileUtils.class.getClassLoader().getResourceAsStream(resource);
    }

    public static EffectiveModelContext buildSchemaFromYangsDir(final String path) {
        final CrossSourceStatementReactor.BuildAction buildAction = RFC7950Reactors.defaultReactorBuilder()
                .build().newBuild();
        try {
            final List<File> filesInFolder = Files.walk(Path.of(path))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File file : filesInFolder) {
                final YangStatementStreamSource statementSource = YangStatementStreamSource.create(
                        YangTextSchemaSource.delegateForByteSource(
                                YangTextSchemaSource.identifierFromFilename(file.getName()),
                                com.google.common.io.Files.asByteSource(file)));

                buildAction.addSource(statementSource);
            }
            return buildAction.buildEffective();
        } catch (IOException | YangParserException | ReactorException e) {
            throw new RuntimeException("Constructing schema from provided path failed!", e);
        }
    }
}
