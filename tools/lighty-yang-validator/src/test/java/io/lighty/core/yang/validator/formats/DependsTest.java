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
import io.lighty.core.yang.validator.FormatTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DependsTest extends FormatTest {

    @Override
    public void setFormat() {
        final List<FormatPlugin> formats = new ArrayList<>();
        formats.add(new Depends());
        this.formatter = new Format(formats);
        this.builder.setFormat("depend");
        this.builder.setDependConfiguration(false, false, false,
                new HashSet<>());
    }

    @Test//(enabled = false) // TODO enable when rt:address-family will be fixed (jira: PTDL-1158)
    public void onlySubmodulesTest() throws Exception {
        setFormat();
        this.builder.setDependConfiguration(false, false, true,
                new HashSet<>());
        final String module = Paths.get(this.yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_submodule-dependencies");
    }

    @Test
    public void onlyImportsTest() throws Exception {
        setFormat();
        this.builder.setDependConfiguration(false, true, false,
                new HashSet<>());
        final String module = Paths.get(this.yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_import-dependencies");
    }

    @Test
    public void dependsTestNotRecursive() throws Exception {
        setFormat();
        this.builder.setDependConfiguration(true, false, false,
                new HashSet<>());
        final String module = Paths.get(this.yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_non-recursive-dependencies");
    }

    @Test
    public void dependsTestNotRecursiveSubmodulesOnly() throws Exception {
        setFormat();
        this.builder.setDependConfiguration(true, false, true,
                new HashSet<>());
        final String module = Paths.get(this.yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_non-recursive-only-submodules-dependencies");
    }

    @Test
    public void dependsTestNotRecursiveModulesOnly() throws Exception {
        setFormat();
        this.builder.setDependConfiguration(true, true, false,
                new HashSet<>());
        final String module = Paths.get(this.yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_non-recursive-only-imports-dependencies");
    }

    @Test
    public void dependsTestExcludeModule() throws Exception {
        setFormat();
        this.builder.setDependConfiguration(false, false, false,
                new HashSet<>(Collections.singleton("ietf-ipv6-router-advertisements")));
        final String module = Paths.get(this.yangPath).resolve("ietf-ipv6-unicast-routing@2018-03-13.yang").toString();
        runLYV(ImmutableList.of(module), this.builder.build(), this.formatter);
        runDependendsTest("ietf-ipv6-router-advertisements_exclude-module-dependencies");
    }

    @Override
    public void runInterfacesTest() throws Exception {
        runDependendsTest("interfaces-dependencies");
    }

    @Override
    public void runIpTest() throws Exception {
        runDependendsTest("ip-dependencies");
    }

    @Override
    public void runConnectionOrentedOamTest() throws Exception {
        runDependendsTest("connectionOrientedOam-dependencies");
    }

    @Override
    public void runRoutingTest() throws Exception {
        runDependendsTest("routing-dependencies");
    }

    @Override
    public void runCustomModuleTest() throws Exception {
        runDependendsTest("testModel-dependencies");
    }

    private void runDependendsTest(final String comapreWithFileName) throws Exception {
        final Path outLog = Paths.get(this.outPath).resolve("out.log");
        final String fileCreated = FileUtils.readFileToString(outLog.toFile(), "utf-8");
        final String compareWith = FileUtils.readFileToString(outLog.getParent()
                .resolve("compare").resolve(comapreWithFileName).toFile(), "utf-8");
        Assert.assertEquals(fileCreated.replaceAll("\\s+", ""), compareWith.replaceAll("\\s+", ""));
    }

}
