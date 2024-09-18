/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.bgp.config;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

public final class BgpConfigUtils {

    private BgpConfigUtils() {
        //Util class
    }

    private static final Set<YangModuleInfo> BASE_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.rpc.rev180329
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.bgp.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.bgp.operational.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.bgp.policy.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.bgp.types.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.interfaces.rev160412
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.local.routing.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.network.instance.rev151018
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.network.instance.types.rev151018
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openconfig.ext.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.openconfig.types.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.policy.types.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.openconfig.net.yang.routing.policy.rev151009
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120
                    .YangModuleInfoImpl.getInstance()
    );

    private static final Set<YangModuleInfo> TOPOLOGY_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev180329
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.sr.rev130819
                    .YangModuleInfoImpl.getInstance()
    );

    private static final Set<YangModuleInfo> EXTENSIONS_MODELS = Set.of(
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv6.rev180417
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720
                    .YangModuleInfoImpl.getInstance()
    );

    public static final Set<YangModuleInfo> ALL_BGP_MODELS = Set.copyOf(
            Stream.of(BASE_MODELS, EXTENSIONS_MODELS, TOPOLOGY_MODELS)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet()));
}
