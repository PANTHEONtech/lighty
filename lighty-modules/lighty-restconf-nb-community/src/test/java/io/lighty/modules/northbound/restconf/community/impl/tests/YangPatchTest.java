package io.lighty.modules.northbound.restconf.community.impl.tests;

import static org.opendaylight.restconf.common.patch.PatchEditOperation.REPLACE;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.List;
import org.opendaylight.restconf.common.context.InstanceIdentifierContext;
import org.opendaylight.restconf.common.patch.PatchContext;
import org.opendaylight.restconf.common.patch.PatchEntity;
import org.opendaylight.restconf.common.patch.PatchStatusContext;
import org.opendaylight.restconf.common.patch.PatchStatusEntity;
import org.opendaylight.restconf.nb.rfc8040.rests.transactions.MdsalRestconfStrategy;
import org.opendaylight.restconf.nb.rfc8040.rests.transactions.RestconfStrategy;
import org.opendaylight.restconf.nb.rfc8040.rests.utils.PatchDataTransactionUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

public class YangPatchTest extends CommunityRestConfTestBase {

    @Test
    public void yangPatchTest() {
        Assert.assertNotNull(getLightyController());
        Assert.assertNotNull(getCommunityRestConf());

        final QName baseQName = QName.create("instance:identifier:patch:module",
                "instance-identifier-patch-module", Revision.of("2015-11-21"));
        final QName exampleList = QName.create(baseQName, "my-list1");
        final NodeIdentifierWithPredicates nodeWithKey = NodeIdentifierWithPredicates.of(exampleList);
        final QName exampleListName = QName.create(baseQName, "name");
        final QName myLeaf11 = QName.create(exampleList, "my-leaf11");
        final QName myLeaf12 = QName.create(exampleList, "my-leaf12");
        final QName containerId = QName.create(exampleList, "patch-cont");

        final LeafNode<?> nameLeafA = Builders.leafBuilder()
                .withNodeIdentifier(new NodeIdentifier(exampleListName))
                .withValue("my-list1 - A")
                .build();

        final LeafNode<?> buildLeaf1 = Builders.leafBuilder()
                .withNodeIdentifier(new NodeIdentifier(myLeaf11))
                .withValue("I am leaf11-0")
                .build();

        final LeafNode<?> buildLeaf2 = Builders.leafBuilder()
                .withNodeIdentifier(new NodeIdentifier(myLeaf12))
                .withValue("I am leaf12-1")
                .build();

        final MapEntryNode mapEntryNode = Builders.mapEntryBuilder()
                .withNodeIdentifier(nodeWithKey)
                .withValue(List.of(nameLeafA, buildLeaf1, buildLeaf2))
                .build();

        final SystemMapNode myList = ImmutableMapNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(exampleList))
                .withValue(Collections.singletonList(mapEntryNode))
                .build();

        final ContainerNode patchContainerNode = Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(containerId))
                .withValue(Collections.singletonList(myList))
                .build();

        final  YangInstanceIdentifier targetNodeMerge = YangInstanceIdentifier.builder()
                .node(containerId)
                .build();

        final PatchEntity entityReplace =
                new PatchEntity("edit1", REPLACE, targetNodeMerge, patchContainerNode);

        final InstanceIdentifierContext iidContext =
                InstanceIdentifierContext.ofLocalRoot(
                        getLightyController().getServices().getDOMSchemaService().getGlobalContext());

        final PatchContext patchContext = new PatchContext(iidContext, List.of(entityReplace), "test-patch");

        patch(patchContext, new MdsalRestconfStrategy(getLightyController().getServices().getClusteredDOMDataBroker()),
                false);

    }


    private void patch(final PatchContext patchContext, final RestconfStrategy strategy,
            final boolean failed) {
        final PatchStatusContext patchStatusContext =
                PatchDataTransactionUtil.patchData(patchContext, strategy,
                        getLightyController().getServices().getDOMSchemaService().getGlobalContext());
        for (final PatchStatusEntity entity : patchStatusContext.getEditCollection()) {
            assertTrue(entity.isOk());
        }
        assertTrue(patchStatusContext.isOk());
    }

}