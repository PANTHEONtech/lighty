/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.transactions;

import com.google.common.util.concurrent.FluentFuture;
import io.lighty.gnmi.southbound.mountpoint.ops.GnmiSet;
import org.opendaylight.mdsal.common.api.CommitInfo;

public class WriteOnlyTx extends AbstractWriteTx {

    private final GnmiSet setProvider;

    public WriteOnlyTx(final GnmiSet gnmiSet) {
        super(gnmiSet.getNodeId());
        this.setProvider = gnmiSet;
    }


    @Override
    public synchronized FluentFuture<CommitInfo> performCommit() {
        return FluentFuture.from(setProvider.set(putList, mergeList, deleteList));
    }
}
