/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator;

import com.google.common.collect.ImmutableList;
import io.lighty.core.yang.validator.config.Configuration;
import io.lighty.core.yang.validator.config.ConfigurationBuilder;
import io.lighty.core.yang.validator.formats.Format;
import io.lighty.core.yang.validator.formats.FormatPlugin;
import io.lighty.core.yang.validator.formats.MultiModulePrinter;
import io.lighty.core.yang.validator.simplify.SchemaSelector;
import io.lighty.core.yang.validator.simplify.SchemaTree;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

public class MainTest implements Cleanable {

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    @AfterTest
    public void removeOuptut() throws Exception {
        tearDown();
    }

    @Test
    public void testSimplifyWithYangFormat() throws Exception {
        XMLStreamReader reader;
        final List<File> xmlFiles;
        final String yangPath = MainTest.class.getResource("/yang").getFile();
        final String outPath = MainTest.class.getResource("/out").getFile();
        final Path xmlPath = Paths.get(MainTest.class.getResource("/xml").getFile());
        xmlFiles = Files.list(xmlPath)
                .map(Path::toFile)
                .collect(Collectors.toList());
        YangContextFactory contextFactory = new YangContextFactory(ImmutableList.of(yangPath), ImmutableList.of(),
                Collections.emptySet(), false);
        EffectiveModelContext effectiveModelContext = contextFactory.createContext(true);

        final SchemaTree schemaTree = resolveSchemaTree(xmlFiles, effectiveModelContext);
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new MultiModulePrinter());
        final Format format = new Format(formats);
        final Configuration config = new ConfigurationBuilder()
                .setFormat("yang")
                .setOutput(outPath)
                .build();
        format.init(config, effectiveModelContext, contextFactory.getTestFilesSourceIdentifiers(), schemaTree);
        format.emit();
        contextFactory =
                new YangContextFactory(ImmutableList.of(outPath), ImmutableList.of(), Collections.emptySet(), false);
        effectiveModelContext = contextFactory.createContext(true);
        for (File xmlFile : xmlFiles) {
            try (InputStream input = new FileInputStream(xmlFile)) {
                final NormalizedNodeResult result = new NormalizedNodeResult();
                final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
                final XmlParserStream xmlParser = XmlParserStream.create(streamWriter,
                        XmlCodecFactory.create(effectiveModelContext), effectiveModelContext);
                reader = FACTORY.createXMLStreamReader(input);
                xmlParser.parse(reader);
                xmlParser.flush();
                xmlParser.close();
                Assert.assertTrue(result.isFinished());
                final AbstractCollection<DataContainerChild<?, ?>> value =
                        (AbstractCollection<DataContainerChild<?, ?>>) result.getResult().getValue();
                Assert.assertEquals(value.size(), 1);
            }
        }
    }

    private static SchemaTree resolveSchemaTree(final List<File> xmlFiles,
            final EffectiveModelContext effectiveModelContext) throws Exception {
        final SchemaSelector schemaSelector = new SchemaSelector(effectiveModelContext);

        for (final File xmlFile : xmlFiles) {
            final FileInputStream fis = new FileInputStream(xmlFile);
            schemaSelector.addXml(fis);
        }

        return schemaSelector.getSchemaTree();
    }

}
