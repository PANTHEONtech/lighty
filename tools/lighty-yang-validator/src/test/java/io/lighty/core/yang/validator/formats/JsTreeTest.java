/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import com.google.common.collect.ImmutableList;
import io.lighty.core.yang.validator.Main;
import io.lighty.core.yang.validator.FormatTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsTreeTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new JsTree());
        this.formatter = new Format(formats);
        this.builder.setFormat("jstree");
    }

    @Test
    public void testUndeclared() throws Exception {
        //testing for undeclared choice-case statement (no case inside of choice)
        setFormat();
        final String module = Paths.get(this.yangPath).resolve("undeclared.yang").toString();
        Main.runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runJsTreeTest("undeclared.html");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runJsTreeTest("interfaces.html");
    }

    @Override
    public void runIpTest() throws Exception {
        runJsTreeTest("ip.html");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runJsTreeTest("connectionOrientedOam.html");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runJsTreeTest("routing.html");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runJsTreeTest("testModel.html");
    }

    private void runJsTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(this.outPath).resolve("out.log");
        final String fileCreated = FileUtils.readFileToString(outLog.toFile(), "utf-8");
        final String compareWith = FileUtils.readFileToString(outLog.getParent()
                .resolve("compare").resolve(comapreWithFileName).toFile(), "utf-8");
        Assert.assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}