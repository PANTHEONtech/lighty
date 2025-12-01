/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.gnmi.southbound.lightymodule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;

@ExtendWith(MockitoExtension.class)
public class GnmiSouthBoundModuleTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private DOMMountPointService mountPointService;
    @Mock
    private AAAEncryptionService aaaEncryptionService;
    @Mock
    private YangParserFactory parserFactory;
    @Mock
    private YangXPathParserFactory xpathParserFactory;

    @Test
    @SuppressWarnings("unchecked")
    public void gnmiModuleSmokeTest() {
        when(parserFactory.createParser()).thenReturn(mock(YangParser.class));

        WriteTransaction writeTx = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTx);
        FluentFuture<CommitInfo> successfulCommit = FluentFuture.from(Futures.immediateFuture(CommitInfo.empty()));
        doReturn(successfulCommit).when(writeTx).commit();

        ObjectRegistration registration = mock(ObjectRegistration.class);
        lenient().doReturn(registration).when(rpcProviderService)
            .registerRpcImplementations((ClassToInstanceMap) any());
        lenient().doReturn(registration).when(rpcProviderService)
            .registerRpcImplementations((Collection) any());
        lenient().doReturn(registration).when(rpcProviderService)
            .registerRpcImplementations((Rpc<?, ?>[]) any());

        Registration listenerRegistration = mock(Registration.class);
        lenient().doReturn(listenerRegistration).when(dataBroker)
            .registerDataTreeChangeListener(any(), (DataTreeChangeListener) any());

        final GnmiSouthboundModule gnmiModule = new GnmiSouthboundModule(
            dataBroker,
            rpcProviderService,
            mountPointService,
            aaaEncryptionService,
            parserFactory,
            xpathParserFactory
        );

        gnmiModule.init();
        gnmiModule.close();
    }
}