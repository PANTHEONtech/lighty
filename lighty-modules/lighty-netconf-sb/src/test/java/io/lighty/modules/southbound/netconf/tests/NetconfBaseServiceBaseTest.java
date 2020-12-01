/*
 * Copyright (c) 2019 Pantheon.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.netconf.tests;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotBuilder;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.BaseSchema;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.DefaultBaseNetconfSchemas;
import org.opendaylight.yangtools.rcf8528.data.util.EmptyMountPointContext;
import org.opendaylight.yangtools.rfc8528.data.api.MountPointContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.impl.YangParserFactoryImpl;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.AntlrXPathParserFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

@Test
public abstract class NetconfBaseServiceBaseTest {

    protected EffectiveModelContext effectiveModelContext;
    protected MountPointContext mountContext;
    protected BaseSchema baseSchema;

    @BeforeClass
    public void beforeTest() throws YangParserException {
        @SuppressWarnings("checkstyle:LineLength")  /* Lines kept long for brevity. */
        final ImmutableSet<YangModuleInfo> yangModuleInfos = ImmutableSet.of(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastores.rev180214.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.metadata.rev160805.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.origin.rev180214.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.with.defaults.rev110601.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.nmda.rev190107.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.$YangModuleInfoImpl.getInstance()
        );
        effectiveModelContext = getEffectiveModelContext(new ArrayList<>(yangModuleInfos));
        mountContext = new EmptyMountPointContext(effectiveModelContext);
        baseSchema = new DefaultBaseNetconfSchemas(new YangParserFactoryImpl()).getBaseSchema();
    }

    static EffectiveModelContext getEffectiveModelContext(final List<YangModuleInfo> moduleInfos) throws YangParserException {
        final YangXPathParserFactory xpathFactory = new AntlrXPathParserFactory();
        final YangParserFactoryImpl yangParserFactory = new YangParserFactoryImpl(xpathFactory);
        ModuleInfoSnapshotBuilder moduleInfoBackedCntxt = new ModuleInfoSnapshotBuilder(yangParserFactory);
        moduleInfoBackedCntxt.add(moduleInfos);
        return moduleInfoBackedCntxt.build().getEffectiveModelContext();
    }

    boolean hasSpecificChild(final Collection<DataContainerChild<? extends PathArgument, ?>> children,
                                     final String localName) {
        return children.stream()
                .anyMatch(child -> child.getIdentifier().getNodeType().getLocalName().equals(localName));
    }

    Element getSpecificElementSubtree(final Element doc, final String namespace, final String localName) {
        return getSpecificElementSubtree(doc, namespace, localName, 0);
    }

    Element getSpecificElementSubtree(final Element doc, final String namespace, final String localName,
                                      final Integer itemNumber) {
        return (Element) doc.getElementsByTagNameNS(namespace, localName).item(itemNumber);
    }

    Element getSpecificElementSubtree(final Element doc, final QName qname, final String localName,
                                      final Integer itemNumber) {
        return getSpecificElementSubtree(doc, qname.getNamespace().toString(), localName, itemNumber);
    }

    Element getSpecificElementSubtree(final Element doc, final QName qname, final String localName) {
        return getSpecificElementSubtree(doc, qname.getNamespace().toString(), localName);
    }

}
