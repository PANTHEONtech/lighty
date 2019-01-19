/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs.api;

import io.lighty.codecs.AbstractCodecTest;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.common.QName;

public class ConverterUtilsTest extends AbstractCodecTest {

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
    }
}
