/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import static io.lighty.core.yang.validator.Main.runLYV;

import com.google.common.collect.ImmutableList;
import io.lighty.core.yang.validator.Cleanable;
import io.lighty.core.yang.validator.Main;
import io.lighty.core.yang.validator.MainTest;
import io.lighty.core.yang.validator.config.Configuration;
import io.lighty.core.yang.validator.config.ConfigurationBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnalyzerTest implements Cleanable {

    private String yangPath;
    private Format formatter;
    private ConfigurationBuilder builder;
    private String outPath;
    private Method method;
    private Constructor<Main> constructor;

    @BeforeClass
    public void init() {
        outPath = TreeTest.class.getResource("/out").getFile();
        yangPath = MainTest.class.getResource("/yang").getFile();

        this.builder = new ConfigurationBuilder()
                .setRecursive(false)
                .setOutput(this.outPath);
    }

    @BeforeMethod
    public void setUpOutput() throws Exception {
        this.constructor = (Constructor<Main>) Main.class.getDeclaredConstructors()[0];
        this.constructor.setAccessible(true);
        Main mainClass = this.constructor.newInstance();

        this.method = Main.class.getDeclaredMethod("setMainLoggerOutput", Configuration.class);
        this.method.setAccessible(true);
        this.method.invoke(mainClass, this.builder.build());
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Analyzer());
        this.formatter = new Format(formats);
        this.builder.setFormat("analyze");
        this.builder.setTreeConfiguration(0, 0, false, false, false);
    }

    @AfterMethod
    public void removeOuptut() throws Exception {
        tearDown();
        this.method.setAccessible(false);
        this.constructor.setAccessible(false);
    }


    @Test
    public void analyzeTest() throws Exception {
        final String module = Paths.get(this.yangPath).resolve("ietf-netconf-common@2013-10-21.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runAnalyzeTest("ietf-netconf-common-analyzed");
    }


    private void runAnalyzeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(this.outPath).resolve("out.log");
        final String fileCreated = FileUtils.readFileToString(outLog.toFile(), "utf-8");
        final String compareWith = FileUtils.readFileToString(outLog.getParent().resolve("compare")
                .resolve(comapreWithFileName).toFile(), "utf-8");
        Assert.assertEquals(fileCreated, compareWith);
    }

}