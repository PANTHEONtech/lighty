/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.lightymodule.util;

import java.util.Set;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

public final class GnmiConfigUtils {
    public static final Set<YangModuleInfo> YANG_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.topology.rev210316
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.yang.storage.rev210331
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.force.capabilities.rev210702
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.lighty.gnmi.certificate.storage.rev210504
                    .YangModuleInfoImpl.getInstance()
    );

    public static final Set<YangModuleInfo> OPENCONFIG_YANG_MODELS = Set.of(
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.aaa.rev200730.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.aaa.types.rev181121.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.alarms.rev190709.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.alarms.types.rev181121.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.interfaces.aggregate.rev200501.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.interfaces.ethernet.rev210609.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.interfaces.rev210406.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.license.rev200422.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.messages.rev180813.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openconfig._if.types.rev181121.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openconfig.ext.rev200616.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openconfig.types.rev190416.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openflow.rev181121.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openflow.types.rev200630.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.platform.rev210118.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.platform.types.rev210118.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.system.logging.rev181121.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.system.procmon.rev190315.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.system.rev200413.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.system.terminal.rev181121.YangModuleInfoImpl
            .getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.types.inet.rev210107.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.types.yang.rev210302.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.vlan.rev190416.YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.svc.v1.http.openconfig.net.yang.vlan.types.rev200630.YangModuleInfoImpl.getInstance()
    );

    private GnmiConfigUtils() {
        //Utility class
    }

}
