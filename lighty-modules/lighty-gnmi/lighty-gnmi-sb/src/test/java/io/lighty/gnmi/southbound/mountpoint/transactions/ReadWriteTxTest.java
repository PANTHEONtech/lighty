/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.transactions;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

public class ReadWriteTxTest {
    private ReadWriteTx readWriteTx;
    @Mock
    private ReadOnlyTx readOnlyTx;
    @Mock
    private WriteOnlyTx writeOnlyTx;
    private YangInstanceIdentifier yiid;
    private NormalizedNode node;
    private LogicalDatastoreType logicalDatastoreType;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        yiid = YangInstanceIdentifier.builder()
                .node(QName.create("test-namespace", "2021-05-03", "test-node"))
                .build();
        node = ImmutableNodes.leafNode(
                YangInstanceIdentifier.NodeIdentifier.create(QName.create("test-namespace",
                        "2021-05-03", "test-node")), 123);

        readWriteTx = new ReadWriteTx(readOnlyTx, writeOnlyTx);
        logicalDatastoreType = LogicalDatastoreType.CONFIGURATION;
    }

    @Test
    public void mergeTest() {
        readWriteTx.merge(logicalDatastoreType, yiid, node);
        verify(writeOnlyTx, times(1)).merge(eq(logicalDatastoreType), eq(yiid), eq(node));
    }

    @Test
    public void readTest() {
        readWriteTx.read(logicalDatastoreType, yiid);
        verify(readOnlyTx, times(1)).read(eq(logicalDatastoreType), eq(yiid));
    }

    @Test
    public void deleteTest() {
        readWriteTx.delete(logicalDatastoreType, yiid);
        verify(writeOnlyTx, times(1)).delete(eq(logicalDatastoreType), eq(yiid));
    }

    @Test
    public void cancelTest() {
        readWriteTx.cancel();
        verify(writeOnlyTx, times(1)).cancel();
    }

    @Test
    public void existsTest() {
        readWriteTx.exists(logicalDatastoreType, yiid);
        verify(readOnlyTx, times(1)).exists(eq(logicalDatastoreType), eq(yiid));
    }

    @Test
    public void combinedTest() {
        readWriteTx.merge(logicalDatastoreType, yiid, node);
        readWriteTx.exists(logicalDatastoreType, yiid);
        readWriteTx.delete(logicalDatastoreType, yiid);
        readWriteTx.read(logicalDatastoreType, yiid);
        readWriteTx.cancel();
        verify(writeOnlyTx, times(1)).merge(eq(logicalDatastoreType), eq(yiid), eq(node));
        verify(readOnlyTx, times(1)).exists(eq(logicalDatastoreType), eq(yiid));
        verify(readOnlyTx, times(1)).read(eq(logicalDatastoreType), eq(yiid));
        verify(writeOnlyTx, times(1)).delete(eq(logicalDatastoreType), eq(yiid));
        verify(writeOnlyTx, times(1)).cancel();

    }

}
