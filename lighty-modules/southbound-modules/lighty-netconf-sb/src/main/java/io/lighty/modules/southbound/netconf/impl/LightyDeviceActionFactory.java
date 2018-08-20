/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.southbound.netconf.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
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

