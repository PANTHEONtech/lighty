/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.lighty.core.yang.validator.GroupArguments;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;

public class JsTree extends FormatPlugin {

    private static final String HELP_NAME = "jstree";
    private static final String HELP_DESCRIPTION = "Prints out html, javascript tree of the modules";
    private static final String INPUT = "input";

    private final Map<URI, String> namespacePrefix = new HashMap<>();

    public JsTree() {
        super(Tree.class);
    }

    @Override
    public void emitFormat() {
        for (final RevisionSourceIdentifier source : this.sources) {
            List<Line> lines = new ArrayList<>();
            final Module module = this.schemaContext.findModule(source.getName(), source.getRevision()).get();
            final String headerText = prepareHeader(module);
            log.info(headerText);
            for (Module m : this.schemaContext.getModules()) {
                if (!m.getPrefix().equals(module.getPrefix())) {
                    namespacePrefix.put(m.getNamespace(), m.getPrefix());
                }
            }
            final List<Integer> removeChoiceQnames = new ArrayList<>();
            int id = 1;
            for (DataSchemaNode node : module.getChildNodes()) {
                final ArrayList<Integer> ids = new ArrayList<>(Collections.singletonList(id++));
                HtmlLine htmlLine = new HtmlLine(ids, node, RpcInputOutput.OTHER, this.schemaContext,
                        removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(ids), node, RpcInputOutput.OTHER, removeChoiceQnames,
                        Collections.emptyList());
            }
            for (Line l : lines) {
                final String linesText = l.toString();
                log.info(linesText);
            }
            // augmentations
            lines = new ArrayList<>();
            for (AugmentationSchemaNode augNode : module.getAugmentations()) {
                final ArrayList<Integer> ids = new ArrayList<>(Collections.singletonList(id++));
                HtmlLine htmlLine = new HtmlLine(new ArrayList<>(ids), augNode.getChildNodes().iterator().next(),
                        RpcInputOutput.OTHER, this.schemaContext, removeChoiceQnames, namespacePrefix,
                        Optional.of(augNode), false);
                lines.add(htmlLine);
                final Iterator<? extends DataSchemaNode> nodes = augNode.getChildNodes().iterator();
                int modelAugmentationNumber = 1;
                while (nodes.hasNext()) {
                    removeChoiceQnames.clear();
                    final DataSchemaNode node = nodes.next();
                    final ArrayList<QName> qNames = Lists.newArrayList(node.getPath().getPathFromRoot().iterator());
                    Collection<? extends ActionDefinition> actions = new HashSet<>();
                    RpcInputOutput inputOutputOther = RpcInputOutput.OTHER;
                    for (int i = 1; i <= qNames.size(); i++) {
                        List<QName> qNamesCopy = new ArrayList<>(qNames);
                        qNamesCopy = qNamesCopy.subList(0, i);
                        if (!actions.isEmpty()) {
                            for (ActionDefinition action : actions) {
                                if (action.getQName().getLocalName()
                                        .equals(qNamesCopy.get(qNamesCopy.size() - 1).getLocalName())) {
                                    if (INPUT.equals(qNames.get(i).getLocalName())) {
                                        inputOutputOther = RpcInputOutput.INPUT;
                                    } else {
                                        inputOutputOther = RpcInputOutput.OUTPUT;
                                    }
                                }
                            }
                        }
                        final ListIterator<Integer> integerListIterator =
                                removeChoiceQnames.listIterator(removeChoiceQnames.size());
                        while (integerListIterator.hasPrevious()) {
                            qNamesCopy.remove(integerListIterator.previous().intValue());
                        }
                        if (!this.schemaContext.findDataTreeChild(qNamesCopy).isPresent()) {
                            removeChoiceQnames.add(i - 1);
                        } else if (this.schemaContext.findDataTreeChild(qNamesCopy).get()
                                instanceof ActionNodeContainer) {
                            final ActionNodeContainer actionSchemaNode =
                                    (ActionNodeContainer) this.schemaContext.findDataTreeChild(qNamesCopy).get();
                            actions = actionSchemaNode.getActions();
                        }
                    }
                    ids.add(modelAugmentationNumber++);
                    HtmlLine line = new HtmlLine(new ArrayList<>(ids), node, inputOutputOther, this.schemaContext,
                            removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                    lines.add(line);
                    resolveChildNodes(lines, new ArrayList<>(ids), node, RpcInputOutput.OTHER, removeChoiceQnames,
                            Collections.emptyList());
                    ids.remove(ids.size() - 1);
                }
                for (Line line : lines) {
                    final String linesText = line.toString();
                    log.info(linesText);
                }
                lines = new ArrayList<>();
            }
            // rpcs
            for (RpcDefinition node : module.getRpcs()) {
                final ArrayList<Integer> rpcId = new ArrayList<>(Collections.singletonList(id++));
                HtmlLine htmlLine = new HtmlLine(rpcId, node, RpcInputOutput.OTHER, this.schemaContext,
                        removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                final boolean inputExists = !node.getInput().getChildNodes().isEmpty();
                final boolean outputExists = !node.getOutput().getChildNodes().isEmpty();
                ArrayList<Integer> ids = new ArrayList<>(rpcId);
                if (inputExists) {
                    ids.add(1);
                    htmlLine = new HtmlLine(new ArrayList<>(ids), node.getInput(), RpcInputOutput.INPUT,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(),
                            false);
                    lines.add(htmlLine);
                    resolveChildNodes(lines, new ArrayList<>(ids), node.getInput(), RpcInputOutput.INPUT,
                            removeChoiceQnames, Collections.emptyList());
                }
                ids = new ArrayList<>(rpcId);
                if (outputExists) {
                    if (!inputExists) {
                        ids.add(1);
                    } else {
                        ids.add(2);
                    }
                    htmlLine = new HtmlLine(new ArrayList<>(ids), node.getOutput(), RpcInputOutput.OUTPUT,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(),
                            false);
                    lines.add(htmlLine);
                    resolveChildNodes(lines, new ArrayList<>(ids), node.getOutput(), RpcInputOutput.OUTPUT,
                            removeChoiceQnames, Collections.emptyList());
                }
            }
            for (Line line : lines) {
                final String linesText = line.toString();
                log.info(linesText);
            }
            lines = new ArrayList<>();
            // Notifications
            for (NotificationDefinition node : module.getNotifications()) {
                final ArrayList<Integer> ids = new ArrayList<>(Collections.singletonList(id++));
                HtmlLine htmlLine = new HtmlLine(new ArrayList<>(ids), node, RpcInputOutput.OTHER, this.schemaContext,
                        removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(ids), node, RpcInputOutput.OTHER, removeChoiceQnames,
                        Collections.emptyList());
            }
            for (Line line : lines) {
                final String linesText = line.toString();
                log.info(linesText);
            }
        }
        log.info("</table>");
        log.info("</div>");
        final String jsText = loadJS();
        log.info(jsText);
        log.info("</body>");
        log.info("</html>");
    }

    private String loadJS() {
        URL url = Resources.getResource("js");
        String text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Can not load text from js file");
        }

        return text;
    }

    private String prepareHeader(final Module module) {
        final StringBuilder nameRevision = new StringBuilder(module.getName());
        if (module.getRevision().isPresent()) {
            nameRevision.append("@")
                    .append(module.getRevision().get());
        }
        URL url = Resources.getResource("header");
        String text = "";
        try {
            text = Resources.toString(url, StandardCharsets.UTF_8);
            text = text.replace("<NAME_REVISION>", nameRevision);
            text = text.replace("<NAMESPACE>", module.getNamespace().toString());
            text = text.replace("<PREFIX>", module.getPrefix());
        } catch (IOException e) {
            log.error("Can not load text from header file");
        }

        return text;
    }

    private void resolveChildNodes(List<Line> lines, List<Integer> connections, SchemaNode node,
                                   RpcInputOutput inputOutput, List<Integer> removeChoiceQnames,
                                   List<QName> keys) {
        if (node instanceof DataNodeContainer) {
            int id = 1;
            final Iterator<? extends DataSchemaNode> childNodes = ((DataNodeContainer) node).getChildNodes().iterator();
            connections.add(0);
            while (childNodes.hasNext()) {
                final DataSchemaNode child = childNodes.next();
                connections.set(connections.size() - 1, id++);
                HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), child, inputOutput, this.schemaContext,
                        removeChoiceQnames, namespacePrefix, Optional.empty(), keys.contains(child.getQName()));
                lines.add(htmlLine);
                List<QName> keyDefinitions = Collections.emptyList();
                if (child instanceof ListSchemaNode) {
                    keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
                }
                resolveChildNodes(lines, new ArrayList<>(connections), child, inputOutput, removeChoiceQnames,
                        keyDefinitions);
            }
            // remove last only if the conatiner is not root container
            if (connections.size() > 1) {
                connections.remove(connections.size() - 1);
            }
        } else if (node instanceof ChoiceSchemaNode) {
            int id = 1;
            connections.add(0);
            final Collection<? extends CaseSchemaNode> cases = ((ChoiceSchemaNode) node).getCases();
            final Iterator<? extends CaseSchemaNode> iterator = cases.iterator();
            removeChoiceQnames.add(((List) node.getPath().getPathFromRoot()).size() - 1);
            while (iterator.hasNext()) {
                final DataSchemaNode child = iterator.next();
                removeChoiceQnames.add(((List) child.getPath().getPathFromRoot()).size() - 1);
                connections.set(connections.size() - 1, id++);
                HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), child, inputOutput, this.schemaContext,
                        removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                resolveChildNodes(lines, new ArrayList<>(connections), child, inputOutput, removeChoiceQnames,
                        Collections.emptyList());
                removeChoiceQnames.remove(Integer.valueOf(((List) child.getPath().getPathFromRoot()).size() - 1));
            }
            removeChoiceQnames.remove(Integer.valueOf(((List) node.getPath().getPathFromRoot()).size() - 1));
            // remove last
            connections.remove(connections.size() - 1);
        }
        // If action is in container or list
        if (node instanceof ActionNodeContainer) {

            for (ActionDefinition action : ((ActionNodeContainer) node).getActions()) {
                int id = 1;
                connections.add(0);
                connections.set(connections.size() - 1, id);
                HtmlLine htmlLine = new HtmlLine(new ArrayList<>(connections), action, RpcInputOutput.OTHER,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                lines.add(htmlLine);
                final boolean inputExists = !action.getInput().getChildNodes().isEmpty();
                final boolean outputExists = !action.getOutput().getChildNodes().isEmpty();
                if (inputExists) {
                    connections.add(1);
                    htmlLine = new HtmlLine(new ArrayList<>(connections), action.getInput(), RpcInputOutput.INPUT,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                    lines.add(htmlLine);
                    resolveChildNodes(lines, new ArrayList<>(connections), action.getInput(), RpcInputOutput.INPUT,
                            removeChoiceQnames, Collections.emptyList());
                    connections.remove(connections.size() - 1);
                }
                if (outputExists) {
                    connections.add(1);
                    htmlLine = new HtmlLine(new ArrayList<>(connections), action.getOutput(), RpcInputOutput.OUTPUT,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, Optional.empty(), false);
                    lines.add(htmlLine);
                    resolveChildNodes(lines, new ArrayList<>(connections), action.getOutput(), RpcInputOutput.OUTPUT,
                            removeChoiceQnames, Collections.emptyList());
                    connections.remove(connections.size() - 1);
                }
                connections.remove(connections.size() - 1);
            }
        }
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        return Optional.empty();
    }
}
