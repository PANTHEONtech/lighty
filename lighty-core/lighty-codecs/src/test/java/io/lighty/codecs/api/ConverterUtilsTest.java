/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import io.lighty.codecs.AbstractCodecTest;
import io.lighty.codecs.xml.XmlElement;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;

public class ConverterUtilsTest extends AbstractCodecTest {

    public ConverterUtilsTest() throws YangParserException {
    }

    @Test
    public void testGetRpcQName_norevision() throws Exception {
        XmlElement xmlElement = XmlElement.fromString(loadResourceAsString("make-toast-input_norev.xml"));
        Optional<QName> rpcQName = ConverterUtils.getRpcQName(xmlElement);
        Assert.assertTrue(rpcQName.isPresent());
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getLocalName().equals(rpcQName.get().getLocalName()));
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getNamespace().equals(rpcQName.get().getNamespace()));
        Assert.assertFalse(MAKE_TOAST_RPC_QNAME.getRevision().equals(rpcQName.get().getRevision()));
    }

    @Test
    public void testGetRpcQNameFromXML_norevision() {
        Optional<QName> rpcQName = ConverterUtils.getRpcQName(loadResourceAsString("make-toast-input_norev.xml"));
        Assert.assertTrue(rpcQName.isPresent());
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getLocalName().equals(rpcQName.get().getLocalName()));
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getNamespace().equals(rpcQName.get().getNamespace()));
        Assert.assertFalse(MAKE_TOAST_RPC_QNAME.getRevision().equals(rpcQName.get().getRevision()));
    }

    @Test
    public void testGetRpcQName_revision() throws Exception {
        XmlElement xmlElement = XmlElement.fromString(loadResourceAsString("make-toast-input_rev.xml"));
        Optional<QName> rpcQName = ConverterUtils.getRpcQName(xmlElement);
        Assert.assertTrue(rpcQName.isPresent());
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getLocalName().equals(rpcQName.get().getLocalName()));
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getNamespace().equals(rpcQName.get().getNamespace()));
        Assert.assertTrue(MAKE_TOAST_RPC_QNAME.getRevision().equals(rpcQName.get().getRevision()));
    }

    @Test
    public void testRpcAsInput() throws Exception {
        XmlElement makeToastRpc = XmlElement.fromString(loadResourceAsString("make-toast-input_rev.xml"));
        XmlElement rpcAsInput =
                ConverterUtils.rpcAsInput(makeToastRpc, "http://netconfcentral.org/ns/toaster?revision=2009-11-20");
        Assert.assertNotNull(rpcAsInput);
        Assert.assertTrue(rpcAsInput.getName().equals("input"));
        rpcAsInput = ConverterUtils.rpcAsInput(makeToastRpc);
        Assert.assertNotNull(rpcAsInput);
        Assert.assertTrue(rpcAsInput.getName().equals("input"));
    }

    @Test
    public void testRpcAsOutput() throws Exception {
        XmlElement makeToastRpc = XmlElement.fromString(loadResourceAsString("make-toast-input_rev.xml"));
        XmlElement rpcAsOutput =
                ConverterUtils.rpcAsOutput(makeToastRpc, "http://netconfcentral.org/ns/toaster?revision=2009-11-20");
        Assert.assertNotNull(rpcAsOutput);
        Assert.assertTrue(rpcAsOutput.getName().equals("output"));
        rpcAsOutput = ConverterUtils.rpcAsOutput(makeToastRpc);
        Assert.assertNotNull(rpcAsOutput);
        Assert.assertTrue(rpcAsOutput.getName().equals("output"));
    }

    @Test
    public void testGetSchemaNode() {
        SchemaNode node = ConverterUtils.getSchemaNode(this.effectiveModelContext, Toaster.QNAME)
                .orElseThrow().getDataSchemaNode();
        Assert.assertNotNull(node);
        Assert.assertEquals(Toaster.QNAME, node.getQName());
        node = ConverterUtils.getSchemaNode(this.effectiveModelContext, TOASTER_YANG_INSTANCE_IDENTIFIER)
                .orElseThrow().getDataSchemaNode();
        Assert.assertNotNull(node);
        Assert.assertEquals(Toaster.QNAME, node.getQName());
    }
}
