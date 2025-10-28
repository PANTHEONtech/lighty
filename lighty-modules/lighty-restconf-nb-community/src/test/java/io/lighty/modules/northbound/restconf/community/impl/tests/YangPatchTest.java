/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.netconf.databind.DatabindContext;
import org.opendaylight.netconf.databind.DatabindPath;
import org.opendaylight.restconf.mdsal.spi.data.MdsalRestconfStrategy;
import org.opendaylight.restconf.server.api.DataYangPatchResult;
import org.opendaylight.restconf.server.api.PatchContext;
import org.opendaylight.restconf.server.api.PatchEntity;
import org.opendaylight.restconf.server.api.testlib.CompletingServerRequest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.patch.rev170222.yang.patch.yang.patch.Edit;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.testng.annotations.Test;

public class YangPatchTest extends CommunityRestConfTestBase {

    private static final QName BASE_Q_NAME = QName.create("instance:identifier:patch:module",
            "instance-identifier-patch-module", Revision.of("2015-11-21")).intern();
    private static final QName EXAMPLE_LIST = QName.create(BASE_Q_NAME, "my-list1").intern();
    private static final QName EXAMPLE_LIST_NAME =  QName.create(BASE_Q_NAME, "name").intern();
    private static final QName MY_LEAF_11 =  QName.create(EXAMPLE_LIST, "my-leaf11").intern();
    private static final QName MY_LEAF_12 = QName.create(EXAMPLE_LIST, "my-leaf12").intern();
    private static final QName CONTAINER_ID = QName.create(EXAMPLE_LIST, "patch-cont").intern();
    private static final String MY_LIST_1_A = "my-list1 - A";
    private static final String I_AM_LEAF_11_0 = "I am leaf11-0";
    private static final String I_AM_LEAF_12_1 = "I am leaf12-1";

    @Test
    public void patchDataReplaceTest() throws Exception {
        assertNotNull(getLightyController());
        assertNotNull(getCommunityRestConf());

        final ContainerNode patchContainerNode = getContainerWithData();
        final YangInstanceIdentifier targetNodeMerge = YangInstanceIdentifier.builder()
                .node(CONTAINER_ID)
                .build();

        final DatabindContext databindContext = DatabindContext.ofModel(getLightyController()
            .getServices().getDOMSchemaService().getGlobalContext());

        final PatchEntity entityReplace = new PatchEntity(
            "edit1", Edit.Operation.Replace, getPath(targetNodeMerge, databindContext), patchContainerNode);
        final PatchContext patchContext = new PatchContext("test-patch", List.of(entityReplace));

        final var strategy = new MdsalRestconfStrategy(DatabindContext.ofModel(getLightyController()
                .getServices().getDOMSchemaService().getGlobalContext()), getLightyController().getServices()
                .getClusteredDOMDataBroker());

        final CompletingServerRequest<DataYangPatchResult> dataYangPatchRequest = new CompletingServerRequest<>();

        strategy.patchData(dataYangPatchRequest, new DatabindPath.Data(DatabindContext.ofModel(getLightyController()
            .getServices().getDOMSchemaService().getGlobalContext())) ,patchContext);
        assertTrue(dataYangPatchRequest.getResult().status().ok());

        final ContainerNode response = (ContainerNode) getLightyController().getServices().getClusteredDOMDataBroker()
                .newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of())
                .get(5000, TimeUnit.MILLISECONDS).orElseThrow();
        final DataContainerChild bodyOfResponse = response.body().iterator().next();
        assertEquals(bodyOfResponse, getContainerWithData());
    }

    private static ContainerNode getContainerWithData() {
        final LeafNode<?> nameLeafA = ImmutableNodes.newLeafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(EXAMPLE_LIST_NAME))
                .withValue(MY_LIST_1_A)
                .build();
        final LeafNode<?> buildLeaf1 = ImmutableNodes.newLeafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(MY_LEAF_11))
                .withValue(I_AM_LEAF_11_0)
                .build();
        final LeafNode<?> buildLeaf2 = ImmutableNodes.newLeafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(MY_LEAF_12))
                .withValue(I_AM_LEAF_12_1)
                .build();
        final MapEntryNode mapEntryNode = ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(EXAMPLE_LIST))
                .withValue(List.of(nameLeafA, buildLeaf1, buildLeaf2))
                .build();
        final SystemMapNode myList = ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(NodeIdentifier.create(EXAMPLE_LIST))
                .withValue(Collections.singletonList(mapEntryNode))
                .build();
        return ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(CONTAINER_ID))
                .withValue(Collections.singletonList(myList))
                .build();
    }

    private DatabindPath.Data getPath(final YangInstanceIdentifier path, final DatabindContext databindContext) {
        final var childAndStack = new DatabindPath.Data(
            databindContext).databind().schemaTree().enterPath(path).orElseThrow();
        return new DatabindPath.Data(databindContext, childAndStack.stack().toInference(), path, childAndStack.node());
    }
}
