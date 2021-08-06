/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.sal.connect.api.DeviceActionFactory;
import org.opendaylight.netconf.sal.connect.api.MessageTransformer;
import org.opendaylight.netconf.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class LightyDeviceActionFactory implements DeviceActionFactory {

    @Override
    public DOMActionService createDeviceAction(final MessageTransformer<NetconfMessage> messageTransformer,
            final RemoteDeviceCommunicator<NetconfMessage> listener, final SchemaContext schemaContext) {
        return new LightyDOMActionService(messageTransformer, listener, schemaContext);
    }
}

