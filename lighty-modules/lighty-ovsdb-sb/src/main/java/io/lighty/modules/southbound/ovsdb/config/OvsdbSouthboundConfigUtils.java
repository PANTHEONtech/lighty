/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the LIGHTY.IO LICENSE,
 * version 1.1. If a copy of the license was not distributed with this file,
 * You can obtain one at https://lighty.io/license/1.1/
 */
package io.lighty.modules.southbound.ovsdb.config;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public final class OvsdbSouthboundConfigUtils {

    public static final Set<YangModuleInfo> YANG_MODELS = ImmutableSet.of(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.$YangModuleInfoImpl
            .getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsal.core.general.entity.rev150930.$YangModuleInfoImpl
            .getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.$YangModuleInfoImpl
            .getInstance()
            );

    private OvsdbSouthboundConfigUtils() {
        throw new UnsupportedOperationException("Do not instantiate utility class !");
    }
}
