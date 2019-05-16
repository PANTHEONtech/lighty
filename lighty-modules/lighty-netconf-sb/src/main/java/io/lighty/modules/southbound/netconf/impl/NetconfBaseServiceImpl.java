/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_LOCK_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME;
import static org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import io.lighty.modules.southbound.netconf.impl.util.NetconfUtils;
import java.util.Optional;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.netconf.api.ModifyAction;
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NetconfBaseServiceImpl implements NetconfBaseService {

    private final NodeId nodeId;
    private final DOMRpcService domRpcService;
    private final SchemaContext schemaContext;

    public NetconfBaseServiceImpl(final NodeId nodeId,
                                  final DOMRpcService domRpcService, final SchemaContext schemaContext) {
        this.nodeId = nodeId;
        this.domRpcService = domRpcService;
        this.schemaContext = schemaContext;
    }

    @Override
    public ListenableFuture<DOMRpcResult> get(final Optional<YangInstanceIdentifier> filterYII) {
        if (filterYII.isPresent() && !filterYII.get().isEmpty()) {
            final DataContainerChild<?, ?> filter =
                    NetconfMessageTransformUtil.toFilterStructure(filterYII.get(), schemaContext);
            return domRpcService.invokeRpc(toPath(NETCONF_GET_QNAME),
                    NetconfMessageTransformUtil.wrap(NETCONF_GET_QNAME, filter));
        } else {
            return domRpcService.invokeRpc(toPath(NETCONF_GET_QNAME), NetconfMessageTransformUtil.GET_RPC_CONTENT);
        }
    }

    @Override
    public ListenableFuture<DOMRpcResult> getConfig(final QName sourceDatastore,
            final Optional<YangInstanceIdentifier> filterYII) {
        Preconditions.checkNotNull(sourceDatastore);

        if (filterYII.isPresent() && !filterYII.get().isEmpty()) {
            final DataContainerChild<?, ?> filter =
                    NetconfMessageTransformUtil.toFilterStructure(filterYII.get(), schemaContext);
            return domRpcService.invokeRpc(toPath(NETCONF_GET_CONFIG_QNAME),
                    NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME,
                            NetconfUtils.getSourceNode(sourceDatastore), filter));
        } else {
            return domRpcService.invokeRpc(toPath(NETCONF_GET_CONFIG_QNAME),
                    NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME,
                            NetconfUtils.getSourceNode(sourceDatastore)));
        }
    }

    @Override
    public ListenableFuture<DOMRpcResult> editConfig(final QName targetDatastore,
            final Optional<NormalizedNode<?, ?>> data, final YangInstanceIdentifier dataPath,
            final Optional<ModifyAction> dataModifyActionAttribute,
            final Optional<ModifyAction> defaultModifyAction, final boolean rollback) {
        Preconditions.checkNotNull(targetDatastore);

        DataContainerChild<?, ?> editStructure = NetconfUtils.createEditConfigStructure(schemaContext, data,
                dataModifyActionAttribute, dataPath);

        Preconditions.checkNotNull(editStructure);

        return domRpcService.invokeRpc(toPath(NETCONF_EDIT_CONFIG_QNAME),
                NetconfUtils.getEditConfigContent(targetDatastore, editStructure, defaultModifyAction, rollback));
    }

    @Override
    public ListenableFuture<DOMRpcResult> copyConfig(final QName sourceDatastore, final QName targetDatastore) {
        Preconditions.checkNotNull(sourceDatastore);
        Preconditions.checkNotNull(targetDatastore);

        return domRpcService.invokeRpc(
                toPath(NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_QNAME),
                NetconfUtils.getCopyConfigContent(sourceDatastore, targetDatastore));
    }

    @Override
    public ListenableFuture<DOMRpcResult> deleteConfig(final QName targetDatastore) {
        Preconditions.checkNotNull(targetDatastore);
        Preconditions.checkArgument(!NETCONF_RUNNING_QNAME.equals(targetDatastore),
                "Running datastore cannot be deleted.");

        return domRpcService.invokeRpc(
                toPath(NetconfUtils.NETCONF_DELETE_CONFIG_QNAME),
                NetconfUtils.getDeleteConfigContent(targetDatastore));
    }

    @Override
    public ListenableFuture<DOMRpcResult> lock(final QName targetDatastore) {
        Preconditions.checkNotNull(targetDatastore);

        return domRpcService.invokeRpc(toPath(NETCONF_LOCK_QNAME), NetconfUtils.getLockContent(targetDatastore));
    }

    @Override
    public ListenableFuture<DOMRpcResult> unlock(final QName targetDatastore) {
        Preconditions.checkNotNull(targetDatastore);

        return domRpcService.invokeRpc(toPath(NETCONF_UNLOCK_QNAME), NetconfUtils.getUnLockContent(targetDatastore));
    }

    @Override
    public NodeId getDeviceId() {
        return nodeId;
    }

}
