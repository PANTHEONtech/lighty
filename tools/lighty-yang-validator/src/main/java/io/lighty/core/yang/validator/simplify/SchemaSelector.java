/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.simplify;

import io.lighty.core.yang.validator.simplify.stream.TrackingXmlParserStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

public class SchemaSelector {

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();
    private final EffectiveModelContext effectiveModelContext;
    private final SchemaTree tree;
    @SuppressWarnings("UnstableApiUsage")
    private final XmlCodecFactory codecs;

    @SuppressWarnings("UnstableApiUsage")
    public SchemaSelector(final EffectiveModelContext effectiveModelContext) {
        this.effectiveModelContext = effectiveModelContext;
        this.codecs = XmlCodecFactory.create(effectiveModelContext);
        tree = new SchemaTree(QName.create("root", "root"), null,
                false, false, null);
    }

    public void addXml(final InputStream xml) throws XMLStreamException, IOException, URISyntaxException {
        fillUsedSchema(xml, tree);
    }

    public SchemaTree getSchemaTree() {
        return tree;
    }

    private void fillUsedSchema(final InputStream input, final SchemaTree st)
            throws XMLStreamException, IOException, URISyntaxException {
        final XMLStreamReader reader = FACTORY.createXMLStreamReader(input);
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
        final TrackingXmlParserStream xmlParser =
                new TrackingXmlParserStream(streamWriter, codecs, effectiveModelContext, true, st);
        xmlParser.parse(reader);
        xmlParser.flush();
        xmlParser.close();
    }

    public void noXml() {
        for (Module module : effectiveModelContext.getModules()) {
            for (DataSchemaNode node : module.getChildNodes()) {
                resolveChildNodes(tree, node, true, false);
            }

            for (AugmentationSchemaNode aug : module.getAugmentations()) {
                for (DataSchemaNode node : aug.getChildNodes()) {
                    resolveChildNodes(tree, node, true, true);
                }
            }
        }
    }

    private void resolveChildNodes(final SchemaTree schemaTree, DataSchemaNode node, boolean rootNode,
                                   boolean augNode) {
        SchemaTree childSchemaTree = schemaTree.addChild(node, rootNode, augNode);
        if (node instanceof DataNodeContainer) {
            for (DataSchemaNode schemaNode : ((DataNodeContainer) node).getChildNodes()) {
                resolveChildNodes(childSchemaTree, schemaNode, false, false);
            }
        } else if (node instanceof ChoiceSchemaNode) {
            final Collection<? extends CaseSchemaNode> cases = ((ChoiceSchemaNode) node).getCases();
            for (DataSchemaNode singelCase : cases) {
                resolveChildNodes(childSchemaTree, singelCase, false, false);
            }
        }

        if (node instanceof ActionNodeContainer) {
            final Collection<? extends ActionDefinition> actions = ((ActionNodeContainer) node).getActions();
            for (ActionDefinition action : actions) {
                childSchemaTree = childSchemaTree.addChild(action, false, false);
                if (action.getInput() != null) {
                    resolveChildNodes(childSchemaTree, action.getInput(), false, false);
                }
                if (action.getOutput() != null) {
                    resolveChildNodes(childSchemaTree, action.getOutput(), false, false);
                }
            }
        }
    }
}

