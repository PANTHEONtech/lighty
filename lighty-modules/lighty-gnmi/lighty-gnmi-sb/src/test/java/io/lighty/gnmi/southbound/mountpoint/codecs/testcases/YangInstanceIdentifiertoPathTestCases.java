/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs.testcases;

import com.google.common.collect.Maps;
import gnmi.Gnmi;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class YangInstanceIdentifiertoPathTestCases extends CodecTestCasesBase {

    public YangInstanceIdentifiertoPathTestCases() throws YangLoadException, SchemaException, ConfigurationException {
        super();
    }

    public Map.Entry<YangInstanceIdentifier, Gnmi.Path> leafTestCase(final boolean addPrefixToTopElement) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(addPrefixToTopElement
                                ? makePrefixString(OC_INTERFACES_ID, "interfaces")
                                : "interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("mtu"))
                .build();
        return Maps.immutableEntry(super.leafNumberCase().left, path);
    }

    public Map.Entry<YangInstanceIdentifier, Gnmi.Path> rootElementCase() {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .build();
        return Maps.immutableEntry(super.rootElementTestCase().left, path);
    }

    public Map.Entry<YangInstanceIdentifier, Gnmi.Path> topElementTestCase(
            final boolean addPrefixToTopElement) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(addPrefixToTopElement
                                ? makePrefixString(OC_INTERFACES_ID, "interfaces")
                                : "interfaces")
                        .build())
                .build();
        return Maps.immutableEntry(super.topElementCase().left, path);
    }

    public Map.Entry<YangInstanceIdentifier, Gnmi.Path> listEntryTestCase(
            final boolean addPrefixToTopElement) {
        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(addPrefixToTopElement
                                ? makePrefixString(OC_INTERFACES_ID, "interfaces")
                                : "interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "eth3"))
                .build();
        return Maps.immutableEntry(super.listEntryCase(false).left, path);
    }

    public Map.Entry<YangInstanceIdentifier, Gnmi.Path> augmentedTestCase(
            final boolean addPrefixToTopElement) {

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName(addPrefixToTopElement
                                ? makePrefixString(OC_INTERFACES_ID, "interfaces")
                                : "interfaces"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("interface")
                        .putKey("name", "br0"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("ethernet"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("config"))
                .addElem(Gnmi.PathElem.newBuilder()
                        .setName("aggregate-id"))
                .build();
        return Maps.immutableEntry(super.leafAgumentedCase().left, path);
    }
}
