/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.netconf.api.ModifyAction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface NetconfNmdaBaseService extends NetconfBaseService {

    /**
     * Netconf protocol operation get-data.
     *
     * @param sourceDatastore type of the configuration datastore being queried
     * @param filterYII may contain filter YangInstanceIdentifier if needed
     * @param configFilter flag for getting the configuration nodes only
     * @param maxDepth flag for limiting the number of subtree levels returned in the reply
     * @param originFilter flag for specifying the origin filter which selects nodes
     *                    equal to or derived from any of the given values
     * @param negateOriginFilter flag if we want to negate the origin filter
     * @param withOrigin requests that the server includes "origin" metadata
     *                   annotations in its response, as detailed in the NMDA.
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> getData(QName sourceDatastore,
                                                     Optional<YangInstanceIdentifier> filterYII,
                                                     Optional<Boolean> configFilter,
                                                     Optional<Integer> maxDepth,
                                                     Optional<Set<QName>> originFilter,
                                                     Optional<Boolean> negateOriginFilter,
                                                     Optional<Boolean> withOrigin);

    /**
     * Netconf protocol operation edit-data.
     *
     * @param targetDatastore type of the configuration datastore being edited
     * @param data configuration data
     * @param dataPath YangInstanceIdentifier for the configuration data
     * @param dataModifyActionAttribute may contain operation attribute for the configuration data
     * @param defaultModifyAction may contain default operation
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> editData(QName targetDatastore,
                                                      Optional<NormalizedNode> data,
                                                      YangInstanceIdentifier dataPath,
                                                      Optional<ModifyAction> dataModifyActionAttribute,
                                                      Optional<ModifyAction> defaultModifyAction);
}
