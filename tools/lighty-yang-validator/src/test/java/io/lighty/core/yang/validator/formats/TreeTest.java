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
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TreeTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Tree());
        this.formatter = new Format(formats);
        this.builder.setFormat("tree");
        this.builder.setTreeConfiguration(0, 0, false, false, false);
    }

    @Test
    public void treePrefixMainModuleTest() throws Exception {
        setFormat();
        this.builder.setTreeConfiguration(0, 0, false, false,true);
        final String module = Paths.get(this.yangPath).resolve("ietf-ip@2018-02-22.yang").toString();
        Main.runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runTreeTest("ip-prefix-main-module.tree");
    }

    @Test
    public void treePrefixModuleTest() throws Exception {
        setFormat();
        this.builder.setTreeConfiguration(0, 0, false, true, false);
        final String module = Paths.get(this.yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        Main.runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runTreeTest("interfaces-prefix-module.tree");
    }

    @Test
    public void treeLineLengthTest() throws Exception {
        setFormat();
        this.builder.setTreeConfiguration(0, 20, false, false, false);
        final String module = Paths.get(this.yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        Main.runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runTreeTest("interfaces-line-length.tree");
    }

    @Test
    public void treeHelpTest() throws Exception {
        setFormat();
        this.builder.setTreeConfiguration(0, 0, true, false, false);
        Main.runLYV(Collections.emptyList(), this.builder.build(), this.formatter);
        runTreeTest("tree-help");
    }

    @Test
    public void treeDepthTest() throws Exception {
        setFormat();
        this.builder.setTreeConfiguration(3, 0, false, false, false);
        final String module = Paths.get(this.yangPath).resolve("ietf-interfaces@2018-02-20.yang").toString();
        Main.runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runTreeTest("interfaces-limited-depth.tree");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runTreeTest("interfaces.tree");
    }

    @Override
    public void runIpTest() throws Exception {
        runTreeTest("ip.tree");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runTreeTest("connectionOrientedOam.tree");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runTreeTest("routing.tree");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runTreeTest("testModel.tree");
    }

    private void runTreeTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(this.outPath).resolve("out.log");
        final String fileCreated = FileUtils.readFileToString(outLog.toFile(), "utf-8");
        final String compareWith = FileUtils.readFileToString(outLog.getParent().resolve("compare")
                .resolve(comapreWithFileName).toFile(), "utf-8");
        Assert.assertEquals(fileCreated, compareWith);
    }

}