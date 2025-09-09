/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.TopLevelContainer;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.container.group.SampleContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;

public class ConverterUtilsTest extends AbstractCodecTest {

    public ConverterUtilsTest() throws YangParserException {
        // Constructor required to declare YangParserException due to superclass.
    }

    @Test
    public void testGetRpcQName_norevision() throws Exception {
        final XmlElement xmlElement = XmlElement.fromString(loadResourceAsString("rpc-norev.xml"));
        final Optional<QName> rpcQName = ConverterUtils.getRpcQName(xmlElement);
        assertTrue(rpcQName.isPresent());
        assertEquals(MAKE_TOAST_RPC_QNAME.getLocalName(), rpcQName.get().getLocalName());
        assertEquals(MAKE_TOAST_RPC_QNAME.getNamespace(), rpcQName.get().getNamespace());
        assertNotEquals(MAKE_TOAST_RPC_QNAME.getRevision(), rpcQName.get().getRevision());
    }

    @Test
    public void testGetRpcQNameFromXML_norevision() {
        final Optional<QName> rpcQName = ConverterUtils.getRpcQName(loadResourceAsString("rpc-norev.xml"));
        assertTrue(rpcQName.isPresent());
        assertEquals(MAKE_TOAST_RPC_QNAME.getLocalName(), rpcQName.get().getLocalName());
        assertEquals(MAKE_TOAST_RPC_QNAME.getNamespace(), rpcQName.get().getNamespace());
        assertNotEquals(MAKE_TOAST_RPC_QNAME.getRevision(), rpcQName.get().getRevision());
    }

    @Test
    public void testGetRpcQName_revision() throws Exception {
        final XmlElement xmlElement = XmlElement.fromString(loadResourceAsString("rpc-rev.xml"));
        final Optional<QName> rpcQName = ConverterUtils.getRpcQName(xmlElement);
        assertTrue(rpcQName.isPresent());
        assertEquals(MAKE_TOAST_RPC_QNAME, rpcQName.get());
    }

    @Test
    public void testRpcAsInput() throws Exception {
        final XmlElement makeToastRpc = XmlElement.fromString(loadResourceAsString("rpc-rev.xml"));
        XmlElement rpcAsInput =
                ConverterUtils.rpcAsInput(makeToastRpc, "http://netconfcentral.org/ns/toaster?revision=2009-11-20");
        assertNotNull(rpcAsInput);
        assertEquals("input", rpcAsInput.getName());
        rpcAsInput = ConverterUtils.rpcAsInput(makeToastRpc);
        assertNotNull(rpcAsInput);
        assertEquals("input", rpcAsInput.getName());
    }

    @Test
    public void testRpcAsOutput() throws Exception {
        final XmlElement makeToastRpc = XmlElement.fromString(loadResourceAsString("rpc-rev.xml"));
        XmlElement rpcAsOutput =
                ConverterUtils.rpcAsOutput(makeToastRpc, "http://netconfcentral.org/ns/toaster?revision=2009-11-20");
        assertNotNull(rpcAsOutput);
        assertEquals("output", rpcAsOutput.getName());
        rpcAsOutput = ConverterUtils.rpcAsOutput(makeToastRpc);
        assertNotNull(rpcAsOutput);
        assertEquals("output", rpcAsOutput.getName());
    }

    @Test
    public void testGetTopLevelSchemaNode() {
        final SchemaNode node = ConverterUtils.getSchemaNode(this.effectiveModelContext, Toaster.QNAME)
            .orElseThrow().dataSchemaNode();
        assertNotNull(node);
        assertEquals(Toaster.QNAME, node.getQName());
    }

    @Test
    public void testGetInnerSchemaNode() {
        final SchemaNode node = ConverterUtils.getSchemaNode(this.effectiveModelContext,
                YangInstanceIdentifier.create(
                        YangInstanceIdentifier.NodeIdentifier.create(TopLevelContainer.QNAME),
                        YangInstanceIdentifier.NodeIdentifier.create(SampleContainer.QNAME)))
                .orElseThrow().dataSchemaNode();
        assertNotNull(node);
        assertEquals(SampleContainer.QNAME, node.getQName());
    }

    @Test
    public void testLoadRpc() {
        Optional<? extends RpcDefinition> loadedRpc = ConverterUtils.loadRpc(
                this.effectiveModelContext, LEAF_RPC_QNAME);
        assertTrue(loadedRpc.isPresent());
        loadedRpc = ConverterUtils.loadRpc(this.effectiveModelContext, CONTAINER_RPC_QNAME);
        assertTrue(loadedRpc.isPresent());
    }

    @Test
    public void testLoadNotification() {
        final Optional<? extends NotificationDefinition> loadNotification =
                ConverterUtils.loadNotification(this.effectiveModelContext, NOTIFICATION_QNAME);
        assertTrue(loadNotification.isPresent());
    }

}
