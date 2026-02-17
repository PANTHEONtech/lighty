/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.test;

import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.impl.SimulatedGnmiDevice;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import io.lighty.modules.gnmi.simulatordevice.yang.DatastoreType;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DeviceCreationTest {

    private static final String INIT_DATA_PATH = "src/test/resources/initData";
    private static final String SIMULATOR_CONFIG = "/initData/simulator_config.json";
    public static final String USERNAME_TEST = "Test";
    public static final String PASSWORD_TEST = "Test";

    @Test
    public void deviceInitiatedWithDataTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfigurationMultipleTopElement = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfigurationMultipleTopElement.setInitialConfigDataPath(INIT_DATA_PATH + "/config.json");
        simulatorConfigurationMultipleTopElement.setInitialStateDataPath(INIT_DATA_PATH + "/state.json");

        //Case with multiple top elements
        SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfigurationMultipleTopElement);
        target.start();
        Assertions.assertNotNull(target);
        Assertions.assertNotNull(target.getSchemaContext());
        Assertions.assertNotNull(target.getDataService());
        Assertions.assertNotNull(target.getGnmiService());
        Assertions.assertNotNull(target.getGnoiCertService());
        Assertions.assertNotNull(target.getGnoiFileService());
        Assertions.assertNotNull(target.getGnoiOSService());
        Assertions.assertNotNull(target.getGnoiSystemService());
        Assertions.assertNotNull(target.getGnoiSonicService());
        target.stop();

        final GnmiSimulatorConfiguration simulatorConfigurationOneTopElement = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfigurationOneTopElement.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfigurationOneTopElement.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");

        //Case with one top elements
        target = new SimulatedGnmiDevice(simulatorConfigurationOneTopElement);
        target.start();
        Assertions.assertNotNull(target);
        Assertions.assertNotNull(target.getSchemaContext());
        Assertions.assertNotNull(target.getDataService());
        Assertions.assertNotNull(target.getGnmiService());
        Assertions.assertNotNull(target.getGnoiCertService());
        Assertions.assertNotNull(target.getGnoiFileService());
        Assertions.assertNotNull(target.getGnoiOSService());
        Assertions.assertNotNull(target.getGnoiSystemService());
        Assertions.assertNotNull(target.getGnoiSonicService());
        target.stop();
    }

    @Test
    public void deviceInitiatedWithNoDataTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));

        final SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();
        Assertions.assertNotNull(target);
        Assertions.assertNotNull(target.getSchemaContext());
        Assertions.assertNotNull(target.getDataService());
        Assertions.assertNotNull(target.getGnmiService());
        Assertions.assertNotNull(target.getGnoiCertService());
        Assertions.assertNotNull(target.getGnoiFileService());
        Assertions.assertNotNull(target.getGnoiOSService());
        Assertions.assertNotNull(target.getGnoiSystemService());
        Assertions.assertNotNull(target.getGnoiSonicService());
        target.stop();
    }

    @Test
    public void deviceInitiatedWithAuthTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setUsername(USERNAME_TEST);
        simulatorConfiguration.setPassword(PASSWORD_TEST);

        final SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();
        Assertions.assertNotNull(target);
        Assertions.assertNotNull(target.getSchemaContext());
        Assertions.assertNotNull(target.getDataService());
        Assertions.assertNotNull(target.getGnmiService());
        Assertions.assertNotNull(target.getGnoiCertService());
        Assertions.assertNotNull(target.getGnoiFileService());
        Assertions.assertNotNull(target.getGnoiOSService());
        Assertions.assertNotNull(target.getGnoiSystemService());
        Assertions.assertNotNull(target.getGnoiSonicService());
        target.stop();
    }


    @Test
    public void initialDataPresentMultipleTopElemsTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state.json");

        final SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();

        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2021-04-06", "openconfig-interfaces"),
                                        "interfaces")));
        Assertions.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/platform",
                                                "2021-01-18", "openconfig-platform"),
                                        "components")));
        Assertions.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/interfaces",
                                        "2021-04-06", "openconfig-interfaces"),
                                "interfaces")));
        Assertions.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/system",
                                        "2020-04-13", "openconfig-system"),
                                "system")));
        Assertions.assertTrue(optNormalizedNode.isPresent());
        target.stop();
    }

    @Test
    public void initialDataPresentOneTopElemTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");

        final SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();

        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2021-04-06", "openconfig-interfaces"),
                                        "interfaces")));
        Assertions.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/platform",
                                                "2019-04-16", "openconfig-platform"),
                                        "components")));
        Assertions.assertFalse(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/interfaces",
                                        "2021-04-06", "openconfig-interfaces"),
                                "interfaces")));
        Assertions.assertTrue(optNormalizedNode.isPresent());

        optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.STATE, YangInstanceIdentifier.of(
                        QName.create(
                                QName.create("http://openconfig.net/yang/system",
                                        "2020-04-13", "openconfig-system"),
                                "system")));
        Assertions.assertFalse(optNormalizedNode.isPresent());
        target.stop();
    }

    @Test
    public void initialDataPresentOneTopElemWithNoTLSTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");
        simulatorConfiguration.setUsePlaintext(true);

        final SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();
        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2021-04-06", "openconfig-interfaces"),
                                        "interfaces")));
        Assertions.assertTrue(optNormalizedNode.isPresent());
        target.stop();
    }

    @Test
    public void initialDataPresentOneTopElemWithAuthorizationTest()
            throws IOException, EffectiveModelContextBuilderException {

        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        simulatorConfiguration.setUsername(USERNAME_TEST);
        simulatorConfiguration.setPassword(PASSWORD_TEST);
        simulatorConfiguration.setInitialConfigDataPath(INIT_DATA_PATH + "/config_one_top.json");
        simulatorConfiguration.setInitialStateDataPath(INIT_DATA_PATH + "/state_one_top.json");

        final SimulatedGnmiDevice target = new SimulatedGnmiDevice(simulatorConfiguration);
        target.start();
        Optional<NormalizedNode> optNormalizedNode = target.getDataService()
                .readDataByPath(DatastoreType.CONFIGURATION,
                        YangInstanceIdentifier.of(
                                QName.create(
                                        QName.create("http://openconfig.net/yang/interfaces",
                                                "2021-04-06", "openconfig-interfaces"),
                                        "interfaces")));
        Assertions.assertTrue(optNormalizedNode.isPresent());
        target.stop();
    }
}
