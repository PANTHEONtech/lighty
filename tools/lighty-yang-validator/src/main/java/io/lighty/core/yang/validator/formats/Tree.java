/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats;

import static java.lang.Math.min;

import io.lighty.core.yang.validator.GroupArguments;
import io.lighty.core.yang.validator.simplify.SchemaTree;
import io.lighty.core.yang.validator.config.Configuration;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.ActionNodeContainer;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;

public class Tree extends FormatPlugin {

    private static final String HELP_NAME = "tree";
    private static final String HELP_DESCRIPTION = "Prints out tree of the modules";
    private static final String MODULE = "module: ";
    private static final String AUGMENT = "augment ";
    private static final String SLASH = "/";
    private static final String COLON = ":";
    private static final String RPCS = "RPCs:";
    private static final String NOTIFICATION = "notifications:";

    private final Map<URI, String> namespacePrefix = new HashMap<>();
    private Module usedModule = null;
    private int treeDepth;
    private int lineLength;

    public Tree() {
        super(Tree.class);
    }

    @Override
    void init(final SchemaContext context, final List<RevisionSourceIdentifier> testFilesSchemaSources,
              final SchemaTree schemaTree, final Configuration config) {
        super.init(context, testFilesSchemaSources, schemaTree, config);
        treeDepth = this.configuration.getTreeConfiguration().getTreeDepth();
        final int len = this.configuration.getTreeConfiguration().getLineLength();
        this.lineLength = len == 0 ? 10000 : len;
    }

    @Override
    public void emitFormat() {
        if (this.configuration.getTreeConfiguration().isHelp()) {
            printHelp();
        }
        for (final RevisionSourceIdentifier source : this.sources) {
            List<Line> lines = new ArrayList<>();
            usedModule = this.schemaContext.findModule(source.getName(), source.getRevision()).get();
            for (Module m : this.schemaContext.getModules()) {
                if (!m.getPrefix().equals(usedModule.getPrefix())
                        || this.configuration.getTreeConfiguration().isPrefixMainModule()) {
                    if (this.configuration.getTreeConfiguration().isModulePrefix()) {
                        namespacePrefix.put(m.getNamespace(), m.getName());
                    } else {
                        namespacePrefix.put(m.getNamespace(), m.getPrefix());
                    }
                }
            }
            final String firstLine = MODULE + usedModule.getName();
            log.info(firstLine.substring(0, min(firstLine.length(), lineLength)));
            final List<Integer> removeChoiceQnames = new ArrayList<>();
            int rootNodes = 0;
            for (Map.Entry<SchemaPath, SchemaTree> st : this.schemaTree.getChildren().entrySet()) {
                if (st.getKey().getLastComponent().getModule().equals(usedModule.getQNameModule())
                        && !st.getValue().isAugmenting()) {
                    rootNodes++;
                }
            }
            for (Map.Entry<SchemaPath, SchemaTree> st : this.schemaTree.getChildren().entrySet()) {
                if (st.getKey().getLastComponent().getModule().equals(usedModule.getQNameModule())
                        && !st.getValue().isAugmenting()) {
                    DataSchemaNode node = st.getValue().getSchemaNode();
                    ConsoleLine consoleLine =
                            new ConsoleLine(Collections.emptyList(), node, RpcInputOutput.OTHER, this.schemaContext,
                            removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    List<QName> keyDefinitions = Collections.emptyList();
                    if (node instanceof ListSchemaNode) {
                        keyDefinitions = ((ListSchemaNode) node).getKeyDefinition();
                    }
                    resolveChildNodes(lines, new ArrayList<>(), st.getValue(), --rootNodes > 0,
                            RpcInputOutput.OTHER, removeChoiceQnames, keyDefinitions);
                    this.treeDepth++;
                }
            }
            printLines(lines);

            // augmentations
            final Map<List<QName>, Set<SchemaTree>> augments = new LinkedHashMap<>();
            for (Map.Entry<SchemaPath, SchemaTree> st : this.schemaTree.getChildren().entrySet()) {
                if (st.getKey().getLastComponent().getModule().equals(usedModule.getQNameModule())
                        && st.getValue().isAugmenting()) {
                    final Iterator<QName> iterator = st.getKey().getPathFromRoot().iterator();
                    List<QName> qnames = new ArrayList<>();
                    while (iterator.hasNext()) {
                        final QName next = iterator.next();
                        if (iterator.hasNext()) {
                            qnames.add(next);
                        }
                    }
                    if (augments.get(qnames) == null || augments.get(qnames).isEmpty()) {
                        augments.put(qnames, new LinkedHashSet<>());
                    }
                    augments.get(qnames).add(st.getValue());
                }
            }

            lines = new ArrayList<>();
            for (Map.Entry<List<QName>, Set<SchemaTree>> st : augments.entrySet()) {
                final StringBuilder pathBuilder = new StringBuilder();
                for (QName qname : st.getKey()) {
                    pathBuilder.append(SLASH);
                    if (this.configuration.getTreeConfiguration().isPrefixMainModule()
                            || namespacePrefix.containsKey(qname.getNamespace())) {
                        pathBuilder.append(namespacePrefix.get(qname.getNamespace()))
                                .append(COLON);
                    }
                    pathBuilder.append(qname.getLocalName());
                }
                final String augmentText = AUGMENT + pathBuilder.append(COLON).toString();
                log.info(augmentText.substring(0, min(augmentText.length(), lineLength)));
                int augmentationNodes = st.getValue().size();
                for (final SchemaTree value : st.getValue()) {
                    DataSchemaNode node = value.getSchemaNode();
                    ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), node, RpcInputOutput.OTHER,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, new ArrayList<>(), value, --augmentationNodes > 0,
                            RpcInputOutput.OTHER, removeChoiceQnames, Collections.emptyList());
                    this.treeDepth++;
                }

                printLines(lines);
                lines = new ArrayList<>();
            }
            // rpcs
            final Iterator<? extends RpcDefinition> rpcs = usedModule.getRpcs().iterator();
            if (rpcs.hasNext()) {
                log.info(RPCS.substring(0, min(RPCS.length(), lineLength)));
            }
            while (rpcs.hasNext()) {
                final RpcDefinition node = rpcs.next();
                ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), node, RpcInputOutput.OTHER,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                lines.add(consoleLine);
                final boolean inputExists = !node.getInput().getChildNodes().isEmpty();
                final boolean outputExists = !node.getOutput().getChildNodes().isEmpty();
                if (inputExists) {
                    consoleLine = new ConsoleLine(Collections.singletonList(rpcs.hasNext()), node.getInput(),
                            RpcInputOutput.INPUT, this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    final ArrayList<Boolean> isNextRpc = new ArrayList<>(Collections.singleton(rpcs.hasNext()));
                    resolveChildNodes(lines, isNextRpc, node.getInput(), outputExists, RpcInputOutput.INPUT,
                            removeChoiceQnames, Collections.emptyList());
                    this.treeDepth++;
                }
                if (outputExists) {
                    consoleLine = new ConsoleLine(Collections.singletonList(rpcs.hasNext()), node.getOutput(),
                            RpcInputOutput.OUTPUT, this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    final ArrayList<Boolean> isNextRpc = new ArrayList<>(Collections.singleton(rpcs.hasNext()));
                    resolveChildNodes(lines, isNextRpc, node.getOutput(), false, RpcInputOutput.OUTPUT,
                            removeChoiceQnames, Collections.emptyList());
                    this.treeDepth++;
                }
            }

            printLines(lines);
            lines = new ArrayList<>();
            // Notifications
            final Iterator<? extends NotificationDefinition> notifications = usedModule.getNotifications().iterator();
            if (notifications.hasNext()) {
                log.info(NOTIFICATION.substring(0, min(NOTIFICATION.length(), lineLength)));
            }
            while (notifications.hasNext()) {
                final NotificationDefinition node = notifications.next();
                ConsoleLine consoleLine = new ConsoleLine(Collections.emptyList(), node, RpcInputOutput.OTHER,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                lines.add(consoleLine);
                resolveChildNodes(lines, new ArrayList<>(), node, false, RpcInputOutput.OTHER,
                        removeChoiceQnames, Collections.emptyList());
                this.treeDepth++;
            }
            printLines(lines);
        }
    }

    private void resolveChildNodes(List<Line> lines, List<Boolean> isConnected, SchemaTree st, boolean hasNext,
                                   RpcInputOutput inputOutput, List<Integer> removeChoiceQnames, List<QName> keys) {
        if (--this.treeDepth == 0) {
            return;
        }
        boolean actionExists = false;
        final DataSchemaNode node = st.getSchemaNode();
        if (node instanceof ActionNodeContainer) {
            actionExists = !((ActionNodeContainer) node).getActions().isEmpty();
        }
        if (node instanceof DataNodeContainer) {
            isConnected.add(hasNext);
            final Iterator<Map.Entry<SchemaPath, SchemaTree>> childNodes =
                    st.getDataSchemaNodeChildren().entrySet().iterator();
            while (childNodes.hasNext()) {
                final Map.Entry<SchemaPath, SchemaTree> nextST = childNodes.next();
                if (nextST.getKey().getLastComponent().getModule().equals(usedModule.getQNameModule())) {
                    final SchemaTree childSchemaTree = nextST.getValue();
                    final DataSchemaNode child = childSchemaTree.getSchemaNode();
                    ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), child, inputOutput,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, keys.contains(child.getQName()));
                    lines.add(consoleLine);
                    List<QName> keyDefinitions = Collections.emptyList();
                    if (child instanceof ListSchemaNode) {
                        keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
                    }
                    resolveChildNodes(lines, isConnected, childSchemaTree, childNodes.hasNext()
                            || actionExists, inputOutput, removeChoiceQnames, keyDefinitions);
                    this.treeDepth++;
                }
            }
            isConnected.remove(isConnected.size() - 1);
        } else if (node instanceof ChoiceSchemaNode) {
            isConnected.add(hasNext);
            final Iterator<Map.Entry<SchemaPath, SchemaTree>> caseNodes =
                    st.getDataSchemaNodeChildren().entrySet().iterator();
            removeChoiceQnames.add(((List) node.getPath().getPathFromRoot()).size() - 1);
            while (caseNodes.hasNext()) {
                final Map.Entry<SchemaPath, SchemaTree> nextST = caseNodes.next();
                if (nextST.getKey().getLastComponent().getModule().equals(usedModule.getQNameModule())) {
                    final SchemaTree caseValue = nextST.getValue();
                    final DataSchemaNode child = caseValue.getSchemaNode();
                    removeChoiceQnames.add(((List) child.getPath().getPathFromRoot()).size() - 1);
                    ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), child, inputOutput,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, caseValue, caseNodes.hasNext()
                                    || actionExists, inputOutput, removeChoiceQnames, Collections.emptyList());
                    this.treeDepth++;
                    removeChoiceQnames.remove(Integer.valueOf(((List) child.getPath().getPathFromRoot()).size() - 1));
                }
            }
            removeChoiceQnames.remove(Integer.valueOf(((List) node.getPath().getPathFromRoot()).size() - 1));
            isConnected.remove(isConnected.size() - 1);
        }
        // If action is in container or list
        if (!st.getActionDefinitionChildren().isEmpty()) {
            isConnected.add(hasNext);
            final Iterator<Map.Entry<SchemaPath, SchemaTree>> actions =
                    st.getActionDefinitionChildren().entrySet().iterator();
            while (actions.hasNext()) {
                final Map.Entry<SchemaPath, SchemaTree> nextST = actions.next();
                if (nextST.getKey().getLastComponent().getModule().equals(usedModule.getQNameModule())) {
                    final SchemaTree actionSchemaTree = nextST.getValue();
                    final ActionDefinition action = actionSchemaTree.getActionNode();
                    ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), action,
                            RpcInputOutput.OTHER, this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    boolean inputExists = false;
                    boolean outputExists = false;
                    SchemaTree inValue = null;
                    SchemaTree outValue = null;
                    for (Map.Entry<SchemaPath, SchemaTree> inOut : actionSchemaTree.getChildren().entrySet()) {
                        if ("input".equals(inOut.getKey().getLastComponent().getLocalName())
                                && !inOut.getValue().getChildren().isEmpty()) {
                            inputExists = true;
                            inValue = inOut.getValue();
                        } else if ("output".equals(inOut.getKey().getLastComponent().getLocalName())
                                && !inOut.getValue().getChildren().isEmpty()) {
                            outputExists = true;
                            outValue = inOut.getValue();
                        }
                    }
                    if (inputExists) {
                        isConnected.add(actions.hasNext() || hasNext);
                        consoleLine = new ConsoleLine(new ArrayList<>(isConnected), action.getInput(),
                                RpcInputOutput.INPUT, this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                        lines.add(consoleLine);
                        resolveChildNodes(lines, isConnected, inValue, outputExists, RpcInputOutput.INPUT,
                                removeChoiceQnames, Collections.emptyList());
                        this.treeDepth++;
                        isConnected.remove(isConnected.size() - 1);
                    }
                    if (outputExists) {
                        isConnected.add(actions.hasNext() || hasNext);
                        consoleLine = new ConsoleLine(new ArrayList<>(isConnected), action.getOutput(),
                                RpcInputOutput.OUTPUT, this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                        lines.add(consoleLine);
                        resolveChildNodes(lines, isConnected, outValue, false, RpcInputOutput.OUTPUT,
                                removeChoiceQnames, Collections.emptyList());
                        this.treeDepth++;
                        isConnected.remove(isConnected.size() - 1);
                    }
                }
                isConnected.remove(isConnected.size() - 1);
            }
        }
    }

    private void resolveChildNodes(List<Line> lines, List<Boolean> isConnected, SchemaNode node, boolean hasNext,
                                   RpcInputOutput inputOutput, List<Integer> removeChoiceQnames, List<QName> keys) {
        if (--this.treeDepth == 0) {
            return;
        }

        boolean actionExists = false;
        if (node instanceof ActionNodeContainer) {
            actionExists = !((ActionNodeContainer) node).getActions().isEmpty();
        }
        if (node instanceof DataNodeContainer) {
            isConnected.add(hasNext);
            final Iterator<? extends DataSchemaNode> childNodes = ((DataNodeContainer) node).getChildNodes().iterator();
            while (childNodes.hasNext()) {
                final DataSchemaNode child = childNodes.next();
                ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), child, inputOutput,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, keys.contains(child.getQName()));
                lines.add(consoleLine);
                List<QName> keyDefinitions = Collections.emptyList();
                if (child instanceof ListSchemaNode) {
                    keyDefinitions = ((ListSchemaNode) child).getKeyDefinition();
                }
                resolveChildNodes(lines, isConnected, child, childNodes.hasNext() || actionExists, inputOutput,
                        removeChoiceQnames, keyDefinitions);
                this.treeDepth++;
            }
            // remove last
            isConnected.remove(isConnected.size() - 1);
        } else if (node instanceof ChoiceSchemaNode) {
            isConnected.add(hasNext);
            final Collection<? extends CaseSchemaNode> cases = ((ChoiceSchemaNode) node).getCases();
            final Iterator<? extends CaseSchemaNode> iterator = cases.iterator();
            removeChoiceQnames.add(((List) node.getPath().getPathFromRoot()).size() - 1);
            while (iterator.hasNext()) {
                final DataSchemaNode child = iterator.next();
                removeChoiceQnames.add(((List) child.getPath().getPathFromRoot()).size() - 1);
                ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), child, inputOutput,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                lines.add(consoleLine);
                resolveChildNodes(lines, isConnected, child, iterator.hasNext() || actionExists, inputOutput,
                        removeChoiceQnames, Collections.emptyList());
                this.treeDepth++;
                removeChoiceQnames.remove(Integer.valueOf(((List) child.getPath().getPathFromRoot()).size() - 1));
            }
            removeChoiceQnames.remove(Integer.valueOf(((List) node.getPath().getPathFromRoot()).size() - 1));
            // remove last
            isConnected.remove(isConnected.size() - 1);
        }
        // If action is in container or list
        if (node instanceof ActionNodeContainer) {
            final Iterator<? extends ActionDefinition> actions = ((ActionNodeContainer) node).getActions().iterator();
            while (actions.hasNext()) {
                final ActionDefinition action = actions.next();
                isConnected.add(actions.hasNext());
                ConsoleLine consoleLine = new ConsoleLine(new ArrayList<>(isConnected), action, RpcInputOutput.OTHER,
                        this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                lines.add(consoleLine);
                final boolean inputExists = !action.getInput().getChildNodes().isEmpty();
                final boolean outputExists = !action.getOutput().getChildNodes().isEmpty();
                if (inputExists) {
                    isConnected.add(outputExists);
                    consoleLine = new ConsoleLine(new ArrayList<>(isConnected), action.getInput(), RpcInputOutput.INPUT,
                            this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, action.getInput(), outputExists, RpcInputOutput.INPUT,
                            removeChoiceQnames, Collections.emptyList());
                    this.treeDepth++;
                    isConnected.remove(isConnected.size() - 1);
                }
                if (outputExists) {
                    isConnected.add(false);
                    consoleLine = new ConsoleLine(new ArrayList<>(isConnected), action.getOutput(),
                            RpcInputOutput.OUTPUT, this.schemaContext, removeChoiceQnames, namespacePrefix, false);
                    lines.add(consoleLine);
                    resolveChildNodes(lines, isConnected, action.getOutput(), false, RpcInputOutput.OUTPUT,
                            removeChoiceQnames, Collections.emptyList());
                    this.treeDepth++;
                    isConnected.remove(isConnected.size() - 1);
                }
                isConnected.remove(isConnected.size() - 1);
            }
        }
    }

    private void printLines(final List<Line> lines) {
        for (Line l : lines) {
            final String linesText = l.toString();
            log.info(linesText.substring(0, min(linesText.length(), lineLength)));
        }
    }

    private void printHelp() {
        log.info(
                "tree - tree is printed in following format <status>--<flags> <name><opts> <type> <if-features>\n"
                        + "\n"
                        + " <status> is one of:\n"
                        + "\n"
                        + "    +  for current\n"
                        + "    x  for deprecated\n"
                        + "    o  for obsolete\n"
                        + "\n"
                        + " <flags> is one of:\n"
                        + "\n"
                        + "    rw  for configuration data\n"
                        + "    ro  for non-configuration data, output parameters to rpcs\n"
                        + "       and actions, and notification parameters\n"
                        + "    -w  for input parameters to rpcs and actions\n"
                        + "    -x  for rpcs and actions\n"
                        + "    -n  for notifications\n"
                        + "\n"
                        + " <name> is the name of the node:\n"
                        + "\n"
                        + "    (<name>) means that the node is a choice node\n"
                        + "    :(<name>) means that the node is a case node\n"
                        + "\n"
                        + " <opts> is one of:\n"
                        + "\n"
                        + "    ?  for an optional leaf, choice\n"
                        + "    *  for a leaf-list or list\n"
                        + "    [<keys>] for a list's keys\n"
                        + "\n"
                        + " <type> is the name of the type for leafs and leaf-lists.\n"
                        + "  If the type is a leafref, the type is printed as \"-> TARGET\",\n"
                        + "  whereTARGET is the leafref path, with prefixes removed if possible.\n"
                        + "\n"
                        + " <if-features> is the list of features this node depends on, printed\n"
                        + "     within curly brackets and a question mark \"{...}?\"\n");
    }

    @Override
    public Help getHelp() {
        return new Help(HELP_NAME, HELP_DESCRIPTION);
    }

    @Override
    public Optional<GroupArguments> getGroupArguments() {
        final GroupArguments groupArguments = new GroupArguments(HELP_NAME,
                "Tree format based arguments: ");
        groupArguments.addOption("Number of children to print (0 = all the child nodes).",
                Collections.singletonList("--tree-depth"), false, "?", 0,
                new CollectionArgumentChoice<>(Collections.emptyList()), Integer.TYPE);
        groupArguments.addOption("Number of characters to print for each line (print the whole line).",
                Collections.singletonList("--tree-line-length"), false, "?", 0,
                new CollectionArgumentChoice<>(Collections.emptyList()), Integer.TYPE);
        groupArguments.addOption("Print help information for symbols used in tree format.",
                Collections.singletonList("--tree-help"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("Use the whole module name instead of prefix.",
                Collections.singletonList("--tree-prefix-module"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        groupArguments.addOption("Use prefix with used module.",
                Collections.singletonList("--tree-prefix-main-module"), true, null, null,
                new CollectionArgumentChoice<>(Collections.emptyList()), Boolean.TYPE);
        return Optional.of(groupArguments);
    }
}
