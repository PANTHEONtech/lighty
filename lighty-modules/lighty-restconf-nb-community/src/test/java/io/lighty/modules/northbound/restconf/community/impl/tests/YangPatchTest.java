/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.tests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.restconf.common.patch.PatchContext;
import org.opendaylight.restconf.common.patch.PatchEntity;
import org.opendaylight.restconf.common.patch.PatchStatusContext;
import org.opendaylight.restconf.common.patch.PatchStatusEntity;
import org.opendaylight.restconf.nb.rfc8040.rests.transactions.MdsalRestconfStrategy;
import org.opendaylight.restconf.server.api.DatabindContext;
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
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
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

        final PatchEntity entityReplace = new PatchEntity("edit1", Edit.Operation.Replace, targetNodeMerge,
                patchContainerNode);
        final PatchContext patchContext = new PatchContext("test-patch", List.of(entityReplace));

        final var strategy = new MdsalRestconfStrategy(DatabindContext.ofModel(getLightyController()
                .getServices().getEffectiveModelContext()), getLightyController().getServices()
                .getClusteredDOMDataBroker(), getLightyController().getServices().getDOMRpcService(),
                getLightyController().getServices()
                        .getDOMActionService(), getLightyController().getServices().getYangTextSourceExtension(),
                getLightyController().getServices()
                        .getDOMMountPointService());

        final PatchStatusContext patchStatusContext = strategy.patchData(patchContext).get().status();

        for (final PatchStatusEntity entity : patchStatusContext.editCollection()) {
            assertTrue(entity.isOk());
        }
        assertTrue(patchStatusContext.ok());

        final ContainerNode response = (ContainerNode) getLightyController().getServices().getClusteredDOMDataBroker()
                .newReadOnlyTransaction()
                .read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.of())
                .get(5000, TimeUnit.MILLISECONDS).orElseThrow();

        final DataContainerChild bodyOfResponse = response.body().iterator().next();
        assertEquals(bodyOfResponse, getContainerWithData());
    }

    private static ContainerNode getContainerWithData() {
        final LeafNode<?> nameLeafA = Builders.leafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(EXAMPLE_LIST_NAME))
                .withValue(MY_LIST_1_A)
                .build();
        final LeafNode<?> buildLeaf1 = Builders.leafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(MY_LEAF_11))
                .withValue(I_AM_LEAF_11_0)
                .build();
        final LeafNode<?> buildLeaf2 = Builders.leafBuilder()
                .withNodeIdentifier(NodeIdentifier.create(MY_LEAF_12))
                .withValue(I_AM_LEAF_12_1)
                .build();
        final MapEntryNode mapEntryNode = Builders.mapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(EXAMPLE_LIST))
                .withValue(List.of(nameLeafA, buildLeaf1, buildLeaf2))
                .build();
        final SystemMapNode myList = Builders.mapBuilder()
                .withNodeIdentifier(NodeIdentifier.create(EXAMPLE_LIST))
                .withValue(Collections.singletonList(mapEntryNode))
                .build();
        return Builders.containerBuilder()
                .withNodeIdentifier(NodeIdentifier.create(CONTAINER_ID))
                .withValue(Collections.singletonList(myList))
                .build();
    }
}
