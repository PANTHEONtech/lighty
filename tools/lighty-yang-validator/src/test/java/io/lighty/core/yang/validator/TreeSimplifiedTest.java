/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator;

import static io.lighty.core.yang.validator.Main.runLYV;

import com.google.common.collect.ImmutableList;
import io.lighty.core.yang.validator.config.Configuration;
import io.lighty.core.yang.validator.config.ConfigurationBuilder;
import io.lighty.core.yang.validator.formats.Format;
import io.lighty.core.yang.validator.formats.FormatPlugin;
import io.lighty.core.yang.validator.formats.MultiModulePrinter;
import io.lighty.core.yang.validator.formats.Tree;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.nodes.LazyLeafOperations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TreeSimplifiedTest implements Cleanable {

    private String yangPath;
    private Format formatter;
    private ConfigurationBuilder builder;
    private String outPath;

    private Method method;
    private Constructor<Main> constructor;

    @BeforeClass
    public void init() {
        this.outPath = TreeSimplifiedTest.class.getResource("/out").getFile();
        this.yangPath = TreeSimplifiedTest.class.getResource("/yang").getFile();
    }

    @BeforeMethod
    public void setUpOutput() throws Exception {
        this.constructor = (Constructor<Main>) Main.class.getDeclaredConstructors()[0];
        this.constructor.setAccessible(true);
        // Just to ignore Log message from LazyLeafOperations saying "Leaf nodes are treated as transient nodes"
        LazyLeafOperations.isEnabled();
        this.method = Main.class.getDeclaredMethod("setMainLoggerOutput", Configuration.class);
        final Main mainClass = this.constructor.newInstance();
        this.method.setAccessible(true);
        this.builder = new ConfigurationBuilder()
                .setRecursive(false)
                .setOutput(this.outPath)
                .setPath(Collections.singletonList(this.yangPath));
        this.method.invoke(mainClass, this.builder.build());
    }

    @AfterMethod
    public void removeOuptut() throws Exception {
        tearDown();
        this.method.setAccessible(false);
        this.constructor.setAccessible(false);
    }

    @Test
    public void runTreeSimplifiedTest() throws Exception {
        prepare("tree", new Tree());
        final String module = Paths.get(this.yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        final Path outLog = Paths.get(this.outPath).resolve("out.log");
        final String fileCreated = FileUtils.readFileToString(outLog.toFile(), "utf-8");
        final String compareWith = FileUtils.readFileToString(outLog.getParent().resolve("compare")
                .resolve("interfacesSimplified.tree").toFile(), "utf-8");
        Assert.assertEquals(fileCreated, compareWith);
    }

    @Test
    public void runYangSimplifiedTest() throws Exception {
        prepare("yang", new MultiModulePrinter());
        final String module = Paths.get(this.yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        final Path outLog = Paths.get(this.outPath).resolve("ietf-interfaces@2018-02-20.yang");
        final String fileCreated = FileUtils.readFileToString(outLog.toFile(), "utf-8");
        final String compareWith = FileUtils.readFileToString(outLog.getParent().resolve("compare")
                .resolve("interfaces-simplified.yang").toFile(), "utf-8");
        Assert.assertEquals(fileCreated, compareWith);
    }

    private void prepare(final String format, final FormatPlugin plugin) {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(plugin);
        final String xmlPath = TreeSimplifiedTest.class.getResource("/xml").getFile();

        this.formatter = new Format(formats);
        this.builder.setFormat(format)
                .setSimplify(xmlPath)
                .setTreeConfiguration(0, 0, false, false, false)
                .build();
    }

}
