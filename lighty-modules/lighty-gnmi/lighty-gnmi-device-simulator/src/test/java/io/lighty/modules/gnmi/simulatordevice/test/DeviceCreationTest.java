/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.test;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDeviceBuilder;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.yang.DatastoreType;
import java.io.IOException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DeviceCreationTest {

    private static final String SCHEMA_PATH = "src/test/resources/test_schema";
    private static final String INIT_DATA_PATH = "src/test/resources/initData";
    private static final int TARGET_PORT = 3333;
    private static final String TARGET_HOST = "127.0.0.1";
    public static final String USERNAME_TEST = "Test";
    public static final String PASSWORD_TEST = "Test";

    @Test
    public void deviceInitiatedWithDataTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfigurationMultipleTopElement = new GnmiSimulatorConfiguration();
        simulatorConfigurationMultipleTopElement.setTargetAddress(TARGET_HOST);
        simulatorConfigurationMultipleTopElement.setTargetPort(TARGET_PORT);
        simulatorConfigurationMultipleTopElement.setYangsPath(SCHEMA_PATH);
        simulatorConfigurationMultipleTopElement.setInitialConfigDataPath(INIT_DATA_PATH + "/config.json");
        simulatorConfigurationMultipleTopElement.setInitialStateDataPath(INIT_DATA_PATH + "/state.json");

        //Case with multiple top elements
        SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder()
                .from(simulatorConfigurationMultipleTopElement).build();
        target.start();
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getSchemaContext());
        Assert.assertNotNull(target.getDataService());
        Assert.assertNotNull(target.getGnmiService());
        Assert.assertNotNull(target.getGnoiCertService());
        Assert.assertNotNull(target.getGnoiFileService());
        Assert.assertNotNull(target.getGnoiOSService());
        Assert.assertNotNull(target.getGnoiSystemService());
        Assert.assertNotNull(target.getGnoiSonicService());
        target.stop();

        final GnmiSimulatorConfiguration simulatorConfigurationOneTopElement = new GnmiSimulatorConfiguration();
        simulatorConfigurationOneTopElement.setTargetAddress(TARGET_HOST);
        simulatorConfigurationOneTopElement.setTargetPort(TARGET_PORT);
        simulatorConfigurationOneTopElement.setYangsPath(SCHEMA_PATH);
        simulatorConfigurationOneTopElement.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfigurationOneTopElement.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");

        //Case with one top elements
        target = new SimulatedGnmiDeviceBuilder().from(simulatorConfigurationOneTopElement).build();
        target.start();
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getSchemaContext());
        Assert.assertNotNull(target.getDataService());
        Assert.assertNotNull(target.getGnmiService());
        Assert.assertNotNull(target.getGnoiCertService());
        Assert.assertNotNull(target.getGnoiFileService());
        Assert.assertNotNull(target.getGnoiOSService());
        Assert.assertNotNull(target.getGnoiSystemService());
        Assert.assertNotNull(target.getGnoiSonicService());
        target.stop();
    }

    @Test
    public void deviceInitiatedWithNoDataTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(SCHEMA_PATH);

        final SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getSchemaContext());
        Assert.assertNotNull(target.getDataService());
        Assert.assertNotNull(target.getGnmiService());
        Assert.assertNotNull(target.getGnoiCertService());
        Assert.assertNotNull(target.getGnoiFileService());
        Assert.assertNotNull(target.getGnoiOSService());
        Assert.assertNotNull(target.getGnoiSystemService());
        Assert.assertNotNull(target.getGnoiSonicService());
        target.stop();
    }

    @Test
    public void deviceInitiatedWithAuthTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(SCHEMA_PATH);
        simulatorConfiguration.setUsername(USERNAME_TEST);
        simulatorConfiguration.setPassword(PASSWORD_TEST);

        final SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();
        Assert.assertNotNull(target);
        Assert.assertNotNull(target.getSchemaContext());
        Assert.assertNotNull(target.getDataService());
        Assert.assertNotNull(target.getGnmiService());
        Assert.assertNotNull(target.getGnoiCertService());
        Assert.assertNotNull(target.getGnoiFileService());
        Assert.assertNotNull(target.getGnoiOSService());
        Assert.assertNotNull(target.getGnoiSystemService());
        Assert.assertNotNull(target.getGnoiSonicService());
        target.stop();
    }


    @Test
    public void initialDataPresentMultipleTopElemsTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state.json");

        final SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();

        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2019-11-19", "openconfig-interfaces"),
                                        "interfaces")));
        Assert.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/platform",
                                                "2019-04-16", "openconfig-platform"),
                                        "components")));
        Assert.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/interfaces",
                                        "2019-11-19", "openconfig-interfaces"),
                                "interfaces")));
        Assert.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/alarms",
                                        "2019-07-09", "openconfig-alarms"),
                                "alarms")));
        Assert.assertTrue(optNormalizedNode.isPresent());
        target.stop();
    }

    @Test
    public void initialDataPresentOneTopElemTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");

        final SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();

        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2019-11-19", "openconfig-interfaces"),
                                        "interfaces")));
        Assert.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/platform",
                                                "2019-04-16", "openconfig-platform"),
                                        "components")));
        Assert.assertFalse(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/interfaces",
                                        "2019-11-19", "openconfig-interfaces"),
                                "interfaces")));
        Assert.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/alarms",
                                        "2019-07-09", "openconfig-alarms"),
                                "alarms")));
        Assert.assertFalse(optNormalizedNode.isPresent());
        target.stop();
    }

    @Test
    public void initialDataPresentOneTopElemWithNoTLSTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(SCHEMA_PATH);
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");
        simulatorConfiguration.setUsePlaintext(true);

        final SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();
        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2019-11-19", "openconfig-interfaces"),
                                        "interfaces")));
        Assert.assertTrue(optNormalizedNode.isPresent());
        target.stop();
    }

    @Test
    public void initialDataPresentOneTopElemWithAuthorizationTest()
            throws IOException, ConfigurationException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = new GnmiSimulatorConfiguration();
        simulatorConfiguration.setTargetAddress(TARGET_HOST);
        simulatorConfiguration.setTargetPort(TARGET_PORT);
        simulatorConfiguration.setYangsPath(SCHEMA_PATH);
        simulatorConfiguration.setUsername(USERNAME_TEST);
        simulatorConfiguration.setPassword(PASSWORD_TEST);
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");

        final SimulatedGnmiDevice target = new SimulatedGnmiDeviceBuilder().from(simulatorConfiguration).build();
        target.start();
        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2019-11-19", "openconfig-interfaces"),
                                        "interfaces")));
        Assert.assertTrue(optNormalizedNode.isPresent());
        target.stop();
    }
}
