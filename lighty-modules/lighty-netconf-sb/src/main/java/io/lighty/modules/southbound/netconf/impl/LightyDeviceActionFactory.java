/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.southbound.netconf.impl;

import org.opendaylight.netconf.client.mdsal.api.ActionTransformer;
import org.opendaylight.netconf.client.mdsal.api.DeviceActionFactory;
import org.opendaylight.netconf.client.mdsal.api.RemoteDeviceCommunicator;
import org.opendaylight.netconf.client.mdsal.api.RemoteDeviceServices.Actions;

public class LightyDeviceActionFactory implements DeviceActionFactory {


    @Override
    public Actions createDeviceAction(ActionTransformer messageTransformer,
            RemoteDeviceCommunicator listener) {
        return new LightyDOMActionService(messageTransformer, listener);
    }
}

