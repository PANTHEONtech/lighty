/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMService;
import org.opendaylight.netconf.api.ModifyAction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * This service implements Netconf protocol operations as specified in
 * <a href="https://tools.ietf.org/html/rfc6241#section-7">NETCONF Protocol Operations</a>
 * Instance of this service is bound to specific Netconf device in topology-netconf.
 * Supported Netconf operations: get, get-config ,edit-config, copy-config, delete-config, lock, unlock.
 * Operations close-session and kill-session are not supported by this service, because this functionality is in
 * responsibility of Netconf SBP itself.
 */
public interface NetconfBaseService extends DOMService {

    /**
     * Netconf protocol operation get.
     *
     * @param filterYII may contain filter YangInstanceIdentifier if needed
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> get(Optional<YangInstanceIdentifier> filterYII);

    /**
     * Netconf protocol operation get-config.
     *
     * @param sourceDatastore type of the configuration datastore being queried
     * @param filterYII may contain filter YangInstanceIdentifier if needed
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> getConfig(QName sourceDatastore,
            Optional<YangInstanceIdentifier> filterYII);

    /**
     * Netconf protocol operation edit-config.
     *
     * @param targetDatastore type of the configuration datastore being edited
     * @param data configuration data
     * @param dataPath YangInstanceIdentifier for the configuration data
     * @param dataModifyActionAttribute may contain operation attribute for the configuration data
     * @param defaultModifyAction may contain default operation
     * @param rollback if true, rollback on error option is added to the edit-config message
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> editConfig(QName targetDatastore, Optional<NormalizedNode<?, ?>> data,
            YangInstanceIdentifier dataPath, Optional<ModifyAction> dataModifyActionAttribute,
            Optional<ModifyAction> defaultModifyAction, boolean rollback);

    /**
     * Netconf protocol operation copy-config.
     *
     * @param sourceDatastore type of the configuration datastore to use as the source of the copy-config operation
     * @param targetDatastore type of the configuration datastore to use as the destination of the copy-config operation
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> copyConfig(QName sourceDatastore, QName targetDatastore);

    /**
     * Netconf protocol operation delete-config.
     *
     * @param targetDatastore type of the configuration datastore to delete
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> deleteConfig(QName targetDatastore);

    /**
     * Netconf protocol operation lock.
     *
     * @param targetDatastore of the configuration datastore to lock
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> lock(QName targetDatastore);

    /**
     * Netconf protocol operation unlock.
     *
     * @param targetDatastore of the configuration datastore to unlock
     * @return future with RPC result
     */
    ListenableFuture<? extends DOMRpcResult> unlock(QName targetDatastore);

    /**
     * Get Id of the Netconf device of this service instance.
     *
     * @return future with RPC result
     */
    NodeId getDeviceId();

}
