/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.mountpoint.codecs.testcases;

import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.lightymodule.config.GnmiConfiguration;
import io.lighty.gnmi.southbound.lightymodule.util.GnmiConfigUtils;
import io.lighty.gnmi.southbound.mountpoint.codecs.TestSchemaContextProvider;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.provider.SchemaContextProvider;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Contains various YangInstanceIdentifier and NormalizeNode pairs uses for testing codecs.
 */
public class CodecTestCasesBase {
    private static final String BASE_YANGS_PATH = "src/test/resources/additional/test/schema";
    private static final String OC_GNMI_CONFIG = "/lightyconfigs/openconfig_gnmi_config.json";
    private static final String OC_IF_TYPES_ID = "openconfig-if-types";
    private static final String OC_VLAN_ID = "openconfig-vlan";
    private static final String OC_PLATFORM_ID = "openconfig-platform";
    static final String OC_IF_ETHERNET_ID = "openconfig-if-ethernet";
    static final String OC_INTERFACES_ID = "openconfig-interfaces";
    static final String OC_IF_AGGREGATE_ID = "openconfig-if-aggregate";

    private final SchemaContextProvider schemaContextProvider;

    public CodecTestCasesBase() throws YangLoadException, SchemaException, ConfigurationException {
        final GnmiConfiguration gnmiConfiguration = GnmiConfigUtils.getGnmiConfiguration(
                this.getClass().getResourceAsStream(OC_GNMI_CONFIG));
        Assertions.assertNotNull(gnmiConfiguration.getYangModulesInfo());
        this.schemaContextProvider = TestSchemaContextProvider.createInstance(Paths.get(BASE_YANGS_PATH),
                gnmiConfiguration.getYangModulesInfo());
    }

    /**
     * Returns test case for root schema element.
     *
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> rootElementTestCase() {
        return ImmutablePair.of(YangInstanceIdentifier.empty(), makeRoot());
    }

    /**
     * Returns test case for top schema element (openconfig-interfaces:interfaces).
     *
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> topElementCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(
                        getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"));

        return ImmutablePair.of(identifier, makeInterfaces());
    }

    /**
     * Returns test case for list entry (openconfig-interfaces:interfaces/interface=eth3).
     *
     * @param wrapInMapNode should the resulting MapEntryNode be wrapped in MapNode ?
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> listEntryCase(final boolean wrapInMapNode) {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "eth3"));

        return ImmutablePair.of(identifier, wrapInMapNode
                ? ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                .withValue(List.of(interfaceEth3Node())).build()
                : interfaceEth3Node());
    }

    /**
     * Returns test case for simple container (openconfig-interfaces:interfaces/interface=eth3/config).
     *
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> containerCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "eth3"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "config"));

        return ImmutablePair.of(identifier, interfaceConfigNode());
    }

    /**
     * Returns test case for augmented container (openconfig-interfaces:interfaces/interface=br0/
     * openconfig-ethernet:ethernet/config.
     *
     * @return test case ((inputs to codec), expected output).
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> containerAugmentedCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "br0"))
                        .node(YangInstanceIdentifier.NodeIdentifier.create(
                                (QName.create(getQNameOfModule(OC_IF_ETHERNET_ID), "ethernet"))))
                        .node(getNodeIdentifierOfNodeInModule(OC_IF_ETHERNET_ID, "config"));

        return ImmutablePair.of(identifier, ethConfigNode());
    }

    /**
     * Returns test case for number leaf (openconfig-interfaces:interfaces/interface=eth3/config/mtu).
     *
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> leafNumberCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "eth3"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "config"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "mtu"));

        return ImmutablePair.of(identifier, makeLeafNode(OC_INTERFACES_ID, "mtu", Uint16.valueOf(1500)));
    }

    /**
     * Returns test case for string leaf (openconfig-interfaces:interfaces/interface=eth3/config/name).
     *
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> leafStringCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "eth3"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "config"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "name"));

        return ImmutablePair.of(identifier,
                makeLeafNode(OC_INTERFACES_ID, "name", "admin"));
    }

    /**
     * Returns test case for boolean leaf (openconfig-interfaces:interfaces/interface=eth3/config/loopback-mode).
     *
     * @return test case.
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> leafBooleanCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "eth3"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "config"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "loopback-mode"));
        return ImmutablePair.of(identifier, makeLeafNode(OC_INTERFACES_ID, "loopback-mode", false));
    }

    /**
     * Returns test case for augmented leaf (openconfig-interfaces:interfaces/interface=br0/
     * openconfig-ethernet:ethernet/config/openconfig-if-aggregate:aggregate-id).
     *
     * @return test case ((inputs to codec), expected output).
     */
    protected ImmutablePair<YangInstanceIdentifier, NormalizedNode> leafAgumentedCase() {
        final YangInstanceIdentifier identifier =
                YangInstanceIdentifier.create(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                        .node(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .node(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "br0"))
                        .node(YangInstanceIdentifier.NodeIdentifier.create(
                                QName.create(getQNameOfModule(OC_IF_ETHERNET_ID), "ethernet")))
                        .node(getNodeIdentifierOfNodeInModule(OC_IF_ETHERNET_ID, "config"))
                        .node(YangInstanceIdentifier.NodeIdentifier.create(
                                QName.create(getQNameOfModule(OC_IF_AGGREGATE_ID), "aggregate-id")));

        return ImmutablePair.of(identifier,
                makeLeafNode(OC_IF_AGGREGATE_ID, "aggregate-id", "admin"));
    }

    private YangInstanceIdentifier.NodeIdentifierWithPredicates getMapEntryIdentifierOfNodeInModule(
            final String moduleName, final String nodeName, final String keyName, final Object value) {

        return schemaContextProvider.getSchemaContext().getModules()
                .stream()
                .filter(m -> m.getName().contains(moduleName))
                .map(m -> m.getRevision().isPresent()
                        ? YangInstanceIdentifier.NodeIdentifierWithPredicates.of(QName.create(
                        m.getNamespace(), m.getRevision(), nodeName),
                        QName.create(m.getNamespace(), m.getRevision(), keyName), value)
                        : YangInstanceIdentifier.NodeIdentifierWithPredicates.of(QName.create(
                        m.getNamespace(), nodeName),
                        QName.create(m.getNamespace(), keyName), value))
                .findFirst().orElseThrow();
    }

    public YangInstanceIdentifier.NodeIdentifier getNodeIdentifierOfNodeInModule(final String moduleName,
                                                                                 final String nodeName) {
        return schemaContextProvider.getSchemaContext().getModules()
                .stream()
                .filter(m -> m.getName().contains(moduleName))
                .map(m -> m.getRevision().isPresent()
                        ? YangInstanceIdentifier.NodeIdentifier.create(
                        QName.create(m.getNamespace(), m.getRevision(), nodeName))
                        : YangInstanceIdentifier.NodeIdentifier.create(
                        QName.create(m.getNamespace(), nodeName)))
                .findFirst().orElseThrow();
    }

    public QNameModule getQNameOfModule(final String moduleName) {
        return schemaContextProvider.getSchemaContext().getModules()
                .stream()
                .filter(m -> m.getName().contains(moduleName))
                .map(m -> m.getQNameModule())
                .findFirst().orElseThrow();
    }

    private NormalizedNode makeRoot() {
        final NormalizedNode normalizedNode = ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(SchemaContext.NAME))
                .withChild((DataContainerChild) makeInterfaces())
                .withChild((DataContainerChild) makeComponents()).build();
        return normalizedNode;
    }

    private NormalizedNode makeComponents() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_PLATFORM_ID, "components"))
                .withChild(ImmutableNodes.newSystemMapBuilder()
                        .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_PLATFORM_ID, "component"))
                        .withChild(ImmutableNodes.newMapEntryBuilder()
                                .withNodeIdentifier(getMapEntryIdentifierOfNodeInModule(OC_PLATFORM_ID, "component",
                                        "name", "admin"))
                                .withChild(makeLeafNode(OC_PLATFORM_ID,"name","admin"))
                                .withChild(ImmutableNodes.newContainerBuilder()
                                        .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_PLATFORM_ID, "config"))
                                        .withChild(makeLeafNode(OC_PLATFORM_ID,"name","admin"))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public NormalizedNode makeInterfaces() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interfaces"))
                .withChild(ImmutableNodes.newSystemMapBuilder()
                        .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface"))
                        .withChild(interfaceEth3Node())
                        .withChild(ImmutableNodes.newMapEntryBuilder()
                                .withNodeIdentifier(getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface",
                                        "name", "br0"))
                                .withChild(makeLeafNode(OC_INTERFACES_ID, "name", "br0"))
                                .withChild(interfaceConfigNode())
                                .withChild(ImmutableNodes.newContainerBuilder()
                                    .withNodeIdentifier(
                                        getNodeIdentifierOfNodeInModule(OC_IF_ETHERNET_ID, "ethernet"))
                                    .withChild(ethConfigNode())
                                    .withChild(switchedVlanNode())
                                    .build())
                                .withChild(ImmutableNodes.newContainerBuilder()
                                        .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(
                                                QName.create(getQNameOfModule(OC_IF_AGGREGATE_ID),
                                                        "aggregation")))
                                                .withChild(ImmutableNodes.newContainerBuilder()
                                                        .withNodeIdentifier(getNodeIdentifierOfNodeInModule(
                                                                OC_IF_AGGREGATE_ID, "config"))
                                                        .withValue(List.of(
                                                                makeLeafNode(OC_IF_AGGREGATE_ID, "lag-type", "LACP"),
                                                                makeLeafNode(OC_IF_AGGREGATE_ID, "min-links",
                                                                        Uint16.valueOf(5))
                                                        )).build())
                                                .withChild(switchedVlanNode())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private ContainerNode ethConfigNode() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(
                        getNodeIdentifierOfNodeInModule(OC_IF_ETHERNET_ID, "config"))
                .withValue(List.of(
                        makeLeafNode(OC_IF_ETHERNET_ID, "auto-negotiate", true),
                        makeLeafNode(
                                OC_IF_AGGREGATE_ID, "aggregate-id", "admin"),
                        makeLeafNode(OC_IF_ETHERNET_ID, "port-speed",
                                OC_IF_ETHERNET_ID, "SPEED_10MB"),
                        makeLeafNode(OC_IF_ETHERNET_ID, "enable-flow-control", true)
                )).build();
    }

    public MapEntryNode interfaceEth3Node() {
        return ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(
                        getMapEntryIdentifierOfNodeInModule(OC_INTERFACES_ID, "interface", "name", "eth3"))
                .withChild(interfaceConfigNode())
                .withChild(makeLeafNode(OC_INTERFACES_ID, "name", "eth3"))
                .build();
    }

    public ContainerNode interfaceConfigNode() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_INTERFACES_ID, "config"))
                .withValue(List.of(
                        makeLeafNode(OC_INTERFACES_ID, "name", "admin"),
                        makeLeafNode(OC_INTERFACES_ID, "enabled", false),
                        makeLeafNode(OC_INTERFACES_ID, "type", OC_IF_TYPES_ID, "IF_ETHERNET"),
                        makeLeafNode(OC_INTERFACES_ID, "mtu", Uint16.valueOf(1500)),
                        makeLeafNode(OC_INTERFACES_ID, "loopback-mode", false)
                )).build();
    }

    public ContainerNode switchedVlanNode() {
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_VLAN_ID, "switched-vlan"))
                .withChild(ImmutableNodes.newContainerBuilder()
                        .withNodeIdentifier(getNodeIdentifierOfNodeInModule(OC_VLAN_ID, "config"))
                        .withValue(List.of(
                                makeLeafNode(OC_VLAN_ID, "native-vlan", Uint16.valueOf(37)),
                                makeLeafNode(OC_VLAN_ID, "access-vlan", Uint16.valueOf(45)),
                                makeLeafNode(OC_VLAN_ID, "interface-mode", "ACCESS")
                        )).build())
                .build();
    }

    public LeafNode<Object> makeLeafNode(final String moduleName, final String nodeName, final Object value) {
        return ImmutableNodes.leafNode(getNodeIdentifierOfNodeInModule(moduleName, nodeName), value);
    }

    public LeafNode<Object> makeLeafNode(final String moduleName,
                                         final String nodeName,
                                         final String valueModule,
                                         final String value) {
        return ImmutableNodes.leafNode(getNodeIdentifierOfNodeInModule(moduleName, nodeName),
                        QName.create(getQNameOfModule(valueModule), value));
    }

    public SchemaContextProvider getSchemaContextProvider() {
        return schemaContextProvider;
    }

    protected static String makePrefixString(String moduleIdentifier, String element) {
        return String.format("%s:%s", moduleIdentifier, element);
    }
}
