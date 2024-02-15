/*
 * Copyright (c) 2019 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.netconf.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotBuilder;
import org.opendaylight.netconf.client.mdsal.impl.BaseSchema;
import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemas;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MountPointContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
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
        final Set<YangModuleInfo> yangModuleInfos = Set.of(
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.datastores.rev180214
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.yang.metadata.rev160805
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.origin.rev180214
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.with.defaults.rev110601
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.nmda.rev190107
                        .YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220
                        .YangModuleInfoImpl.getInstance()
        );
        effectiveModelContext = getEffectiveModelContext(new ArrayList<>(yangModuleInfos));
        mountContext = MountPointContext.of(effectiveModelContext);
        baseSchema = new DefaultBaseNetconfSchemas(new DefaultYangParserFactory()).getBaseSchema();
    }

    boolean hasSpecificChild(final Collection<DataContainerChild> children,
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

    private static EffectiveModelContext getEffectiveModelContext(final List<YangModuleInfo> moduleInfos)
            throws YangParserException {
        final DefaultYangParserFactory yangParserFactory = new DefaultYangParserFactory(new AntlrXPathParserFactory());
        ModuleInfoSnapshotBuilder moduleInfoSnapshotBuilder = new ModuleInfoSnapshotBuilder(yangParserFactory);
        moduleInfoSnapshotBuilder.add(moduleInfos);
        return moduleInfoSnapshotBuilder.build().getEffectiveModelContext();
    }
}
