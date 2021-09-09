/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.codecs;

import static org.junit.Assert.fail;

import io.lighty.codecs.api.ConverterUtils;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestockedBuilder;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleList;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleListBuilder;
import org.opendaylight.yang.gen.v1.http.pantheon.tech.ns.test.models.rev180119.SampleListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;

/**
 * Basic tests for {@link DataCodec} class.
 */
public class DataCodecTest extends AbstractCodecTest {

    public DataCodecTest() throws YangParserException {
    }

    /*
     * Deserialization of {@link Container} as top root element
     */
    @Test
    public void testDeserializeData_container() {
        DataCodec<Toaster> dataCodec = new DataCodec<>(this.bindingCodecContext);
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> deserializeData =
                dataCodec.convertToNormalizedNode(TOASTER_INSTANCE_IDENTIFIER, this.testedToaster);

        Assert.assertNotNull(deserializeData);
        Assert.assertNotNull(deserializeData.getKey());
        Assert.assertNotNull(deserializeData.getValue());
        Assert.assertEquals(TOASTER_YANG_INSTANCE_IDENTIFIER, deserializeData.getKey());

        NormalizedNode<?, ?> normalizedNode = deserializeData.getValue();
        if (!(normalizedNode instanceof ContainerNode)) {
            fail(String.format("%s node is not an instance of container node", normalizedNode));
        }
    }

    /*
     * "Serialization" of {@link Container} as top root element
     */
    @Test
    public void testConvertBindingIndependentIntoBindingAware_container() throws IOException, XMLStreamException {
        DataCodec<Toaster> dataCodec = new DataCodec<>(this.bindingCodecContext);
        Toaster serializedToaster =
                dataCodec.convertToBindingAwareData(TOASTER_YANG_INSTANCE_IDENTIFIER, testedToasterNormalizedNodes);
        Assert.assertEquals(this.testedToaster, serializedToaster);
    }

    /*
     * "Serialization" of {@link RpcInput} object
     */
    @Test
    public void testConvertBiIntoBaRpc_rpcInput() {
        DataCodec<MakeToastInput> dataCodec = new DataCodec<>(this.bindingCodecContext);
        QName makeToastQName = QName.create(TOASTER_NAMESPACE, TOASTER_REVISION, "make-toast");
        Optional<? extends RpcDefinition> loadRpc = ConverterUtils.loadRpc(this.effectiveModelContext, makeToastQName);
        if (!loadRpc.isPresent()) {
            throw new IllegalStateException("make-toast RPC was not found");
        }
        MakeToastInput toastInput = dataCodec.convertToBindingAwareRpc(loadRpc.get().getInput().getPath().asAbsolute(),
                (ContainerNode) this.testedMakeToasterNormalizedNodes);
        Assert.assertEquals(this.testedMakeToasterInput.getToasterDoneness(), toastInput.getToasterDoneness());
    }

    @Test
    public void testDeserializeRpc_rpcInput() {
        DataCodec<MakeToastInput> dataCodec = new DataCodec<>(this.bindingCodecContext);
        ContainerNode deserializeRpc = dataCodec.convertToBindingIndependentRpc(this.testedMakeToasterInput);
        Assert.assertNotNull(deserializeRpc);
        Assert.assertEquals(EXPECTED_ONE, deserializeRpc.getValue().iterator().next().getValue());
    }

    @Test
    public void testDeserializeNotification_notificationData() {
        ToasterRestocked toasterRestocked = new ToasterRestockedBuilder().setAmountOfBread(EXPECTED_ONE).build();
        DataCodec<ToasterRestocked> dataCodec = new DataCodec<>(this.bindingCodecContext);
        ContainerNode deserializeNotification = dataCodec.convertToBindingIndependentNotification(toasterRestocked);

        Assert.assertNotNull(deserializeNotification);
        Assert.assertEquals(EXPECTED_ONE, deserializeNotification.getValue().iterator().next().getValue());
    }

    @Test
    public void testConvertIdentifier() {
        DataCodec<Toaster> dataCodec = new DataCodec<>(this.bindingCodecContext);
        YangInstanceIdentifier yangInstanceIdentifier = dataCodec.deserializeIdentifier(TOASTER_INSTANCE_IDENTIFIER);
        Assert.assertEquals(TOASTER_YANG_INSTANCE_IDENTIFIER, yangInstanceIdentifier);
    }

    @Test
    public void testDeserializeIdentifier() {
        DataCodec<Toaster> dataCodec = new DataCodec<>(this.bindingCodecContext);
        String yangInstanceIdentifierString = dataCodec.deserializeIdentifier(TOASTER_YANG_INSTANCE_IDENTIFIER);
        Assert.assertNotNull(yangInstanceIdentifierString);
        Assert.assertTrue(yangInstanceIdentifierString.length() > 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testConvertNonexistingIdentifier() {
        DataCodec<Toaster> dataCodec = new DataCodec<>(this.bindingCodecContext);
        dataCodec.convertIdentifier("/badToaster:badToaster");
    }

    @Test(expected = Exception.class)
    public void testSerializeXMLError_invalidErrorXML() throws YangParserException {
        List<YangModuleInfo> yangModuleInfos = Collections.singletonList(org.opendaylight.yang.gen.v1.urn.ietf.params
                .xml.ns.yang.ietf.restconf.rev170126.$YangModuleInfoImpl.getInstance());
        BindingCodecContext codecContext = createCodecContext(yangModuleInfos);
        DataCodec<Toaster> dataCodec = new DataCodec<>(codecContext);
        dataCodec.serializeXMLError(loadResourceAsString("error.xml"));
    }

    @Test
    public void convertToNormalizedNode_list() {
        SampleList sampleList = new SampleListBuilder().withKey(
                new SampleListKey("name")).setName("name").setValue(Uint8.valueOf(1)).build();
        DataCodec<SampleList> codec = new DataCodec<>(this.bindingCodecContext);

        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> convertToNormalizedNode =
                codec.convertToNormalizedNode(InstanceIdentifier.create(SampleList.class), sampleList);
        Assert.assertNotNull(convertToNormalizedNode.getValue());
    }

    @Test
    public void convertFromNormalizedNode_list() {
        DataCodec<SampleList> codec = new DataCodec<>(this.bindingCodecContext);
        SampleList convertToBindingAwareData = codec.convertToBindingAwareData(
                YangInstanceIdentifier.of(SampleList.QNAME), testedSampleListNormalizedNodes);

        Assert.assertNotNull(convertToBindingAwareData);
        Assert.assertNotNull(convertToBindingAwareData.key());
    }

    @Test
    public void testConvertBindingAwareList() throws Exception {
        DataCodec<SampleList> codec = new DataCodec<>(this.bindingCodecContext);
        Collection<SampleList> list = codec.convertBindingAwareList(YangInstanceIdentifier.of(SampleList.QNAME),
                (MapNode) testedSampleMapNodeNormalizedNodes);
        Assert.assertNotNull(list);
        Assert.assertFalse(list.isEmpty());
    }
}
