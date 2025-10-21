/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.simulatordevice.gnmi;

import com.google.gson.Gson;
import gnmi.Gnmi;
import gnmi.Gnmi.Path;
import io.lighty.modules.gnmi.simulatordevice.config.GnmiSimulatorConfiguration;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder;
import io.lighty.modules.gnmi.simulatordevice.utils.EffectiveModelContextBuilder.EffectiveModelContextBuilderException;
import io.lighty.modules.gnmi.simulatordevice.utils.GnmiSimulatorConfUtils;
import io.lighty.modules.gnmi.simulatordevice.yang.DatastoreType;
import io.lighty.modules.gnmi.simulatordevice.yang.YangDataService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingCodecContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.testng.Assert;

public class LoadAugmentationFromGnmiPath {

    private static final String SIMULATOR_CONFIG = "/initData/simulator_config.json";
    private static final String INIT_DATA_PATH = "src/test/resources/initData";

    BindingCodecContext codecContext;
    EffectiveModelContext schemaContext;
    YangDataService dataService;
    GnmiCrudService gnmiCrudService;

    @Before
    public void startUp() throws IOException, EffectiveModelContextBuilderException {
        final GnmiSimulatorConfiguration simulatorConfiguration = GnmiSimulatorConfUtils
                .loadGnmiSimulatorConfiguration(this.getClass().getResourceAsStream(SIMULATOR_CONFIG));
        this.schemaContext = new EffectiveModelContextBuilder()
                .addYangModulesInfo(simulatorConfiguration.getYangModulesInfo())
                .build();
        this.dataService = new YangDataService(schemaContext, INIT_DATA_PATH + "/config.json",
                INIT_DATA_PATH + "/state.json");
        this.gnmiCrudService = new GnmiCrudService(dataService, schemaContext, new Gson());
    }

    @After
    public void tearDown() {
        this.gnmiCrudService = null;
        this.dataService = null;
        this.schemaContext = null;
        this.codecContext = null;
    }

    @Test
    public void validateParsingProcessFromGnmiPathToYangInstanceIdentifierToJson() {
        Map<Path, YangInstanceIdentifier> yangInstanceIdMap = this.gnmiCrudService.pathToIdentifierMap(getValidData());
        Set<String> validJsonResponse = getValidJsonResponse();
        for (final Map.Entry<Gnmi.Path, YangInstanceIdentifier> entry : yangInstanceIdMap.entrySet()) {
            Optional<NormalizedNode> result
                    = this.dataService.readDataByPath(DatastoreType.CONFIGURATION, entry.getValue());
            Assert.assertTrue(result.isPresent(),
                    String.format("Failed to load [%s] from data-store", entry.getValue().getLastPathArgument()));
            NormalizedNode normalizedNode = result.get();
            Assert.assertEquals(normalizedNode.name(), entry.getValue().getLastPathArgument());
            //Test to parse data retrieved from data-store to JSON format.
            Map.Entry<Path, String> resultInJsonFormat
                    = this.gnmiCrudService.getResultInJsonFormat(entry, normalizedNode);
            Assert.assertNotNull(resultInJsonFormat.getValue());
            Assert.assertTrue(validJsonResponse.contains(resultInJsonFormat.getValue()));
        }
    }

    @Test
    public void compareWrongIdentifiersWithDataInDatastore() {
        Map<Path, YangInstanceIdentifier> yangInstanceIdMap = this.gnmiCrudService.pathToIdentifierMap(getWrongData());
        for (final Map.Entry<Gnmi.Path, YangInstanceIdentifier> entry : yangInstanceIdMap.entrySet()) {
            Optional<NormalizedNode> result
                    = this.dataService.readDataByPath(DatastoreType.CONFIGURATION, entry.getValue());
            Assert.assertTrue(result.isEmpty(),
                    String.format("Loaded wrong data [%s] from data-store", entry.getValue().getLastPathArgument()));
        }
    }

    private static List<Path> getValidData() {
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .build());

        paths.add(Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("port-speed")
                        .build())
                .build());

        paths.add(Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("aggregation")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("switched-vlan")
                        .build())
                .build());

        paths.add(Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("switched-vlan")
                        .build())
                .build());
        return paths;
    }

    private static List<Path> getWrongData() {
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("state")
                        .build())
                .build());
        paths.add(Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interfaces")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("foo")
                        .putKey("name", "br0")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config")
                        .build())
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("state")
                        .build())
                .build());
        return paths;
    }

    private static Set<String> getValidJsonResponse() {
        HashSet<String> data = new HashSet<>();
        data.add("{\"openconfig-vlan:switched-vlan\":{"
                + "\"config\":{\"native-vlan\":37,\"access-vlan\":45,\"interface-mode\":\"ACCESS\"}}}");
        //TODO: The namespace should be replaced by the module name.
        data.add("{\"port-speed\":(http://openconfig.net/yang/interfaces/ethernet?revision=2020-05-06)SPEED_10MB}");
        data.add("{\"openconfig-vlan:switched-vlan\":{"
                + "\"config\":{\"native-vlan\":34,\"access-vlan\":54,\"interface-mode\":\"ACCESS\"}}}");
        data.add("{\"openconfig-if-ethernet:ethernet\":{"
                + "\"config\":{\"enable-flow-control\":true,\"openconfig-if-aggregate:aggregate-id\":\"admin\","
                + "\"auto-negotiate\":true,\"port-speed\":\"openconfig-if-ethernet:SPEED_10MB\"},"
                + "\"openconfig-vlan:switched-vlan\":{\"config\":{"
                + "\"native-vlan\":37,\"access-vlan\":45,\"interface-mode\":\"ACCESS\"}}}}");

        return data;
    }
}
