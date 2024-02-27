/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.utils;

import com.google.common.io.ByteSource;
import io.lighty.modules.gnmi.commons.util.YangModelSanitizer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor.BuildAction;

/**
 * EffectiveModelContextBuilder build {@link EffectiveModelContext} from provided path to folder which contains
 * yang models and also from provided instances of {@link YangModuleInfo}.
 */
public class EffectiveModelContextBuilder {

    private String yangModulesPath;
    private Set<YangModuleInfo> yangModulesInfo;

    /**
     * Add path to yang models folder. This models will be used for constructing {@link EffectiveModelContext}.
     *
     * @param path could be null but YangModuleInfo have to be added otherwise build method fail
     * @return {@link EffectiveModelContextBuilder}
     * @throws EffectiveModelContextBuilderException if path is nonnull and empty
     */
    public EffectiveModelContextBuilder addYangModulesPath(@Nullable final String path)
            throws EffectiveModelContextBuilderException {
        if (path != null && path.isEmpty()) {
            throw new EffectiveModelContextBuilderException("Provided path to YANG modules is empty");
        }
        this.yangModulesPath = path;
        return this;
    }

    /**
     * Add models YangModuleInfo. This models will be used for constructing {@link EffectiveModelContext}.
     *
     * @param yangModuleInfoSet could be null but yangModulesPath have to be added otherwise build method fail
     * @return {@link EffectiveModelContextBuilder}
     * @throws EffectiveModelContextBuilderException if yangModulesPath is nonnull and empty
     */
    public EffectiveModelContextBuilder addYangModulesInfo(@Nullable final Set<YangModuleInfo> yangModuleInfoSet)
            throws EffectiveModelContextBuilderException {
        if (yangModuleInfoSet != null && yangModuleInfoSet.isEmpty()) {
            throw new EffectiveModelContextBuilderException("Provided list of YangModuleInfo  is empty");
        }
        this.yangModulesInfo = yangModuleInfoSet;
        return this;
    }

    /**
     * Construct {@link EffectiveModelContext} from provided yang models in {@link #addYangModulesInfo(Set)} and {@link
     * #addYangModulesPath(String)}'.
     *
     * @return {@link EffectiveModelContext}
     * @throws EffectiveModelContextBuilderException if models information was not provided or occur any exception
     *                                               during construct EffectiveModelContext
     */
    public EffectiveModelContext build() throws EffectiveModelContextBuilderException {
        if (this.yangModulesPath != null || this.yangModulesInfo != null) {
            final BuildAction buildAction = RFC7950Reactors.defaultReactorBuilder().build().newBuild();
            if (this.yangModulesInfo != null) {
                buildAction.addSources(getYangStatementsFromYangModulesInfo(this.yangModulesInfo));
            }
            if (this.yangModulesPath != null) {
                buildAction.addSources(getYangStatementsFromYangModulesPath(this.yangModulesPath));
            }
            try {
                return buildAction.buildEffective();
            } catch (ReactorException e) {
                throw new EffectiveModelContextBuilderException("Failed to create EffectiveModelContext", e);
            }
        } else {
            throw new EffectiveModelContextBuilderException("Cannot create EffectiveModelContext without"
                    + "yangModulesPath or yangModulesInfo");
        }
    }

    private static List<YangStatementStreamSource> getYangStatementsFromYangModulesPath(final String path)
            throws EffectiveModelContextBuilderException {
        final ArrayList<YangStatementStreamSource> sourceArrayList = new ArrayList<>();
        try (Stream<Path> pathStream = Files.walk(Path.of(path))) {
            final List<File> filesInFolder = pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            for (File file : filesInFolder) {
                final ByteSource sanitizedYangByteSource = YangModelSanitizer
                        .removeRegexpPosix(com.google.common.io.Files.asByteSource(file));
                final YangStatementStreamSource statementSource = YangStatementStreamSource.create(
                        new DelegatedYangTextSource(
                                YangTextSchemaSource.identifierFromFilename(file.getName()),
                                sanitizedYangByteSource, StandardCharsets.UTF_8));

                sourceArrayList.add(statementSource);
            }
            return sourceArrayList;
        } catch (IOException | YangParserException e) {
            final String errorMsg = String.format("Failed to create YangStatementStreamSource from"
                    + "provided path: [%s]", path);
            throw new EffectiveModelContextBuilderException(errorMsg, e);
        }
    }

    private static List<YangStatementStreamSource> getYangStatementsFromYangModulesInfo(
            final Set<YangModuleInfo> yangModulesInfo) throws EffectiveModelContextBuilderException {
        final ArrayList<YangStatementStreamSource> sourceArrayList = new ArrayList<>();
        for (YangModuleInfo yangModuleInfo : yangModulesInfo) {
            try {
                final ByteSource sanitizedYangByteSource = YangModelSanitizer
                        .removeRegexpPosix(yangModuleInfo.getYangTextByteSource());
                final YangStatementStreamSource statementSource
                        = YangStatementStreamSource.create(new DelegatedYangTextSource(
                        YangTextSchemaSource.identifierFromFilename(yangModuleInfo.getName().getLocalName() + ".yang"),
                        sanitizedYangByteSource, StandardCharsets.UTF_8));
                sourceArrayList.add(statementSource);
            } catch (IOException | YangParserException e) {
                final String errorMsg = String.format("Failed to create YangStatementStreamSource from"
                        + "provided YangModuleInfo: [%s]", yangModuleInfo);
                throw new EffectiveModelContextBuilderException(errorMsg, e);
            }
        }
        return sourceArrayList;
    }

    public static class EffectiveModelContextBuilderException extends Exception {

        public EffectiveModelContextBuilderException(String message) {
            super(message);
        }

        public EffectiveModelContextBuilderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
