/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.formats.yang.printer;

import io.lighty.core.yang.validator.simplify.SchemaTree;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DerivableSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.DescriptionStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ReferenceStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RevisionStatement;
import org.slf4j.Logger;

public class ModulePrinter {

    private final Set<TypeDefinition> usedTypes;
    private final Set<String> usedImports;
    private final StatementPrinter printer;
    private final Set<SchemaTree> schemaTree;
    private final QNameModule moduleName;
    private final SmartTypePrintingStrategy typePrinter;
    private final Module module;
    private final Map<QNameModule, String> moduleToPrefix;
    private final Set<String> usedGroupingNames = new HashSet<>();

    private final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTreesMap = new HashMap<>();

    public ModulePrinter(final Set<SchemaTree> schemaTree, final SchemaContext schemaContext,
                         final QNameModule moduleName, final OutputStream out,
                         final Set<TypeDefinition> usedTypes, final Set<String> usedImports) {
        this(schemaTree, schemaContext, moduleName, new IndentingPrinter(new PrintStream(out)), usedTypes, usedImports);
    }

    public ModulePrinter(final Set<SchemaTree> schemaTree, final SchemaContext schemaContext,
                         final QNameModule moduleName, final Logger out,
                         final Set<TypeDefinition> usedTypes, final Set<String> usedImports) {
        this(schemaTree, schemaContext, moduleName, new IndentingLogger(out), usedTypes, usedImports);
    }

    private ModulePrinter(final Set<SchemaTree> schemaTree, final SchemaContext schemaContext,
                          final QNameModule moduleName, final Indenting printer,
                          final Set<TypeDefinition> usedTypes, final Set<String> usedImports) {
        this.usedImports = usedImports;
        this.usedTypes = usedTypes;
        this.schemaTree = schemaTree;
        this.moduleName = moduleName;
        this.printer = new StatementPrinter(printer);
        module = schemaContext.findModule(moduleName).get();
        moduleToPrefix = module.getImports().stream()
                .collect(Collectors.toMap(i -> schemaContext
                                .findModules(i.getModuleName()).iterator().next().getQNameModule(),
                        ModuleImport::getPrefix));
        typePrinter = new SmartTypePrintingStrategy(module, moduleToPrefix);
    }

    public void printYang() {
        printHeader();
        typePrinter.printTypedefs(printer, usedTypes);
        printAugmentations();
        for (SchemaTree st : schemaTree) {
            if (!st.isAugmenting()) {
                printSchema(st);
            }
        }
        printGroupings(groupingTreesMap);
        printer.closeStatement();
    }

    private void printAugmentations() {
        for (final AugmentationSchemaNode augmentation : module.getAugmentations()) {
            boolean printOpeningStatement = true;
            final Set<String> uses = new HashSet<>();
            for (SchemaTree st : schemaTree) {
                if (st.isAugmenting()
                        && st.getSchemaNode().getPath().getParent()
                        .equals(augmentation.getTargetPath().asSchemaPath())) {
                    if (printOpeningStatement) {
                        final StringBuilder target = new StringBuilder();
                        for (final QName name : augmentation.getTargetPath().getNodeIdentifiers()) {
                            target.append('/');
                            target.append(moduleToPrefix.get(name.getModule()));
                            target.append(':');
                            target.append(name.getLocalName());
                        }
                        printer.openStatement(Statement.AUGMENT, target.toString());
                        printOpeningStatement = false;
                    }
                    doPrintSchema(true, st, null, groupingTreesMap);
                }
            }
            if (!printOpeningStatement) {
                printer.closeStatement();
            }
        }
    }

    private void printSchema(final SchemaTree tree) {
        if (module.findDataChildByName(tree.getSchemaNode().getQName()).isPresent()) {
            doPrintSchema(true, tree, null, groupingTreesMap);
        }
    }

    private void printGroupings(final HashMap<GroupingDefinition, Set<SchemaTree>> trees) {
        for (final Map.Entry<GroupingDefinition, Set<SchemaTree>> tree : trees.entrySet()) {
            printer.openStatement(Statement.GROUPING, tree.getKey().getQName().getLocalName());
            for (SchemaTree entry : tree.getValue()) {
                doPrintSchema(true, entry, tree.getKey().getPath().getLastComponent().getLocalName(),
                        new HashMap<>());
            }
            printer.closeStatement();
        }
    }

    private boolean doPrintUses(final DataSchemaNode schemaNode, boolean isPrintingAllowed, final String groupingName,
                                final SchemaTree tree, HashMap<GroupingDefinition, Set<SchemaTree>> groupingTrees) {
        String uses;
        if (schemaNode.isAddedByUses()) {
            final List<GroupingDefinition> groupingDefinitions = module.getGroupings().stream()
                    .filter(g -> g.findDataChildByName(schemaNode.getQName()).isPresent())
                    .collect(Collectors.toList());
            Optional<GroupingDefinition> match = Optional.empty();
            for (GroupingDefinition grouping : groupingDefinitions) {
                final Optional<DataSchemaNode> dataChildByName = grouping.findDataChildByName(schemaNode.getQName());
                if (dataChildByName.isPresent()) {
                    if (((DerivableSchemaNode) schemaNode).getOriginal().isPresent()) {
                        if (!((DerivableSchemaNode) schemaNode).getOriginal().get().getPath()
                                .equals(dataChildByName.get().getPath())) {
                            continue;
                        }
                    }
                    final Collection collection = ((EffectiveStatement) dataChildByName.get()).effectiveSubstatements();
                    if (((EffectiveStatement) schemaNode).effectiveSubstatements().size() == collection.size()) {
                        boolean allSubstatementFound = true;
                        for (Object compare : ((EffectiveStatement) schemaNode).effectiveSubstatements()) {
                            boolean substatementFound = false;
                            for (Object substatement : collection) {
                                if (((EffectiveStatement) compare).getDeclared() == null) {
                                    if (compare.equals(substatement)) {
                                        substatementFound = true;
                                        break;
                                    }
                                } else {
                                    if (((EffectiveStatement) compare).getDeclared()
                                            .equals(((EffectiveStatement) substatement).getDeclared())) {
                                        substatementFound = true;
                                        break;
                                    }
                                }
                            }
                            if (!substatementFound) {
                                allSubstatementFound = false;
                                break;
                            }
                        }
                        if (allSubstatementFound) {
                            match = Optional.of(grouping);
                        }
                    }
                }
            }
            if (match.isPresent()) {
                final GroupingDefinition groupingDefinition = match.get();
                if (!groupingDefinition.getPath().getLastComponent().getLocalName().equals(groupingName)) {
                    uses = groupingDefinition.getPath().getLastComponent().getLocalName();
                    if (!groupingTrees.containsKey(groupingDefinition)) {
                        groupingTrees.put(groupingDefinition, new HashSet<>());
                    }
                    final Set<SchemaTree> schemaTrees = groupingTrees.get(groupingDefinition);
                    if (tree.getSchemaNode() instanceof ChoiceSchemaNode) {
                        boolean extendedTree = false;
                        for (final SchemaTree st : schemaTrees) {
                            if (st.getSchemaNode() instanceof ChoiceSchemaNode) {
                                if (st.getSchemaNode().getPath().getLastComponent()
                                        .equals(tree.getSchemaNode().getPath().getLastComponent())) {
                                    extendedTree = true;
                                    for (Map.Entry<SchemaPath, SchemaTree> entry : tree.getChildren().entrySet()) {
                                        if (!st.getChildren().containsKey(entry.getKey())) {
                                            st.addChild(entry.getValue());
                                        }
                                    }
                                }
                            }
                        }
                        if (!extendedTree) {
                            schemaTrees.add(tree);
                        }
                    } else {
                        boolean containsKey = false;
                        for (SchemaTree st : schemaTrees) {
                            if (st.getSchemaNode().getQName().equals(tree.getSchemaNode().getQName())) {
                                containsKey = true;
                                break;
                            }
                        }
                        if (!containsKey) {
                            schemaTrees.add(tree);
                        }
                    }
                    if (!usedGroupingNames.contains(uses) && isPrintingAllowed) {
                        printer.printSimple("uses", uses);
                        usedGroupingNames.add(uses);
                    }
                    isPrintingAllowed = false;
                }
            }
        }
        return isPrintingAllowed;
    }

    private void doPrintSchema(boolean isPrintingAllowed, final SchemaTree tree, final String groupingName,
                               final HashMap<GroupingDefinition, Set<SchemaTree>> groupingTrees) {
        final DataSchemaNode schemaNode = tree.getSchemaNode();
        if (moduleName.equals(schemaNode.getQName().getModule())) {
            isPrintingAllowed = doPrintUses(schemaNode, isPrintingAllowed, groupingName, tree, groupingTrees);
            if (isPrintingAllowed) {
                if (schemaNode instanceof ContainerSchemaNode) {
                    printer.openStatement(Statement.CONTAINER, schemaNode.getQName().getLocalName());
                    printer.printConfig(schemaNode.isConfiguration());
                } else if (schemaNode instanceof ListSchemaNode) {
                    final ListSchemaNode listSchemaNode = (ListSchemaNode) schemaNode;
                    printer.openStatement(Statement.LIST, schemaNode.getQName().getLocalName());
                    final StringJoiner keyJoiner = new StringJoiner(" ", "key \"", "\"");
                    listSchemaNode.getKeyDefinition().stream()
                            .map(QName::getLocalName)
                            .forEach(keyJoiner::add);
                    printer.printSimple("", keyJoiner.toString());
                } else if (schemaNode instanceof LeafSchemaNode) {
                    final LeafSchemaNode leafSchemaNode = (LeafSchemaNode) schemaNode;
                    printer.openStatement(Statement.LEAF, schemaNode.getQName().getLocalName());
                    typePrinter.printType(printer, leafSchemaNode);
                } else if (schemaNode instanceof ChoiceSchemaNode) {
                    printer.openStatement(Statement.CHOICE, schemaNode.getQName().getLocalName());
                } else if (schemaNode instanceof CaseSchemaNode) {
                    printer.openStatement(Statement.CASE, schemaNode.getQName().getLocalName());
                } else if (schemaNode instanceof LeafListSchemaNode) {
                    printer.openStatement(Statement.LEAF_LIST, schemaNode.getQName().getLocalName());
                    typePrinter.printType(printer, (TypedDataSchemaNode) schemaNode);
                } else {
                    throw new IllegalStateException("Unknown node " + schemaNode);
                }
                doPrintDescription(schemaNode);
                doPrintMandatory(schemaNode);
                doPrintReference(schemaNode);
                doPrintWhen(schemaNode);
            }
            for (final SchemaTree child : tree.getChildren().values()) {
                doPrintSchema(isPrintingAllowed, child, groupingName, groupingTrees);
            }
            if (isPrintingAllowed) {
                printer.closeStatement();
            }
        }
    }

    private void doPrintWhen(final DataSchemaNode schemaNode) {
        final Optional<RevisionAwareXPath> whenCondition = schemaNode.getWhenCondition();
        if (whenCondition.isPresent()) {
            printer.printSimple("when",
                    "\"" + whenCondition.get().getOriginalString() + "\"");
            printer.printEmptyLine();
        }
    }

    private void doPrintReference(final DataSchemaNode schemaNode) {
        final Optional<String> reference = schemaNode.getReference();
        if (reference.isPresent()) {
            printer.printSimple("reference", "\"" + reference.get() + "\"");
            printer.printEmptyLine();
        }
    }

    private void doPrintDescription(final DataSchemaNode schemaNode) {
        final Optional<String> description = schemaNode.getDescription();
        if (description.isPresent()) {
            printer.printSimple("description", "\"" + description.get() + "\"");
            printer.printEmptyLine();
        }
    }

    private void doPrintMandatory(final DataSchemaNode schemaNode) {
        if (schemaNode instanceof MandatoryAware) {
            final boolean mandatory = ((MandatoryAware) schemaNode).isMandatory();
            if (mandatory) {
                printer.printSimple("mandatory", "true");
                printer.printEmptyLine();
            }
        }
    }

    private void printHeader() {
        printer.openStatement(Statement.MODULE, module.getName());
        printer.printSimple("yang-version", module.getYangVersion().toString());
        printer.printEmptyLine();
        printer.printSimple("namespace", "\"" + module.getNamespace().toString() + "\"");
        printer.printEmptyLine();
        printer.printSimple("prefix", module.getPrefix());
        printer.printEmptyLine();
        printImports();
        final Optional<String> organization = module.getOrganization();
        if (organization.isPresent()) {
            printer.printSimple("organization", "\"" + organization.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<String> contact = module.getContact();
        if (contact.isPresent()) {
            printer.printSimple("contact", "\"" + contact.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<String> description = module.getDescription();
        if (description.isPresent()) {
            printer.printSimple("description", "\"" + description.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<String> reference = module.getReference();
        if (reference.isPresent()) {
            printer.printSimple("reference", "\"" + reference.get() + "\"");
            printer.printEmptyLine();
        }
        final Optional<Revision> revision = module.getRevision();
        if (revision.isPresent()) {
            if (module instanceof ModuleEffectiveStatement) {
                final Collection<? extends RevisionStatement> revisions =
                        ((ModuleEffectiveStatement) module).getDeclared().getRevisions();
                for (RevisionStatement rev : revisions) {
                    if (rev.getDescription().isPresent() || rev.getReference().isPresent()) {
                        printer.openStatement(Statement.REVISION, rev.getDate().toString());
                        final Optional<ReferenceStatement> optReference = rev.getReference();
                        if (optReference.isPresent()) {
                            printer.printSimpleSeparately("reference", "\""
                                    + optReference.get().getText() + "\"");
                        }
                        final Optional<DescriptionStatement> optDescription = rev.getDescription();
                        if (optDescription.isPresent()) {
                            printer.printSimpleSeparately("description", "\""
                                    + optDescription.get().getText() + "\"");

                        }
                        printer.closeStatement();
                        printer.printEmptyLine();
                    } else {
                        doPrintSimpleRevision(rev.getDate());
                    }
                }
            } else {
                doPrintSimpleRevision(revision.get());
            }
        }
    }

    private void doPrintSimpleRevision(final Revision revision) {
        printer.printSimple("revision", revision.toString());
        printer.printEmptyLine();
    }

    private void printImports() {
        for (final ModuleImport anImport : module.getImports()) {
            if (this.usedImports.contains(anImport.getModuleName())) {
                printer.openStatement(Statement.IMPORT, anImport.getModuleName());
                printer.printSimple("prefix", anImport.getPrefix());
                printer.closeStatement();
                printer.printEmptyLine();
            }
        }
    }
}

