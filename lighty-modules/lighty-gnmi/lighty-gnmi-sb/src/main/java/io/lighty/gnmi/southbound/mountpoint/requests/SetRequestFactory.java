/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.requests;

import gnmi.Gnmi;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface SetRequestFactory {

    Gnmi.SetRequest newRequest(List<ImmutablePair<YangInstanceIdentifier, NormalizedNode>> putList,
                               List<ImmutablePair<YangInstanceIdentifier, NormalizedNode>> mergeList,
                               List<YangInstanceIdentifier> deleteList) throws GnmiRequestException;

}
