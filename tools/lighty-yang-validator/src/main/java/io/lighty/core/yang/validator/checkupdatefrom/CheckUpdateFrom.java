/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.checkupdatefrom;

import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.addedMustError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.checkMustWarning;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.defaultError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.illegalConfigChangeError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.illegalConfigStateError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.lengthError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.mandatoryError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.maxElementsError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.minElementsError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingBitError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingEnumError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingNodeError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingOldRevision;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.missingRevision;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.nameError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.namespaceError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.patternError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.rangeError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.referenceError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.revisionError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.statusError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.typeError;
import static io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFromErrorRFC6020.unitsError;
import static org.opendaylight.yangtools.yang.model.util.type.BaseTypes.baseTypeOf;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import io.lighty.core.yang.validator.GroupArguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ElementCountConstraint;
import org.opendaylight.yangtools.yang.model.api.ElementCountConstraintAware;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.MandatoryAware;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.MustConstraintAware;
import org.opendaylight.yangtools.yang.model.api.MustDefinition;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeAware;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.RevisionStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LengthConstraint;
import org.opendaylight.yangtools.yang.model.api.type.LengthRestrictedTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.PatternConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeRestrictedTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition;

public class CheckUpdateFrom {

    private static final String MIN_ELEMENTS = "\nmin-elements: ";
    private static final String MAX_ELEMENTS = "\nmaX-elements: ";
    private static final String MUST = "\nmust: ";
    private static final String WHEN = "\nwhen: ";
    private static final String CONFIG = "\nconfig: ";
    private static final String FALSE = "false";
    private static final String DONT_EXISTS = "does not exists";
    private static final String RANGES = "\nranges: ";
    private static final String LENGTH = "\nlength: ";
    private static final RangeSet<Integer> INTEGER_ALLOWED_RANGES =
            ImmutableRangeSet.of(Range.closed(0, Integer.MAX_VALUE));

    private final SchemaContext oldContext;
    private final SchemaContext newContext;
    private final Module oldModule;
    private final Module newModule;
    private final boolean is7950;

    private final Set<CheckUpdateFromErrorRFC6020> errors = new LinkedHashSet<>();

    public CheckUpdateFrom(final SchemaContext newContext, final String newModule,
                           final SchemaContext oldContext, final String oldModule,
                           final int rfcVersion) {
        this.newContext = newContext;
        this.oldContext = oldContext;
        final String newModuleName = extractModuleName(newModule);
        final String oldModuleName = extractModuleName(oldModule);
        this.newModule = this.newContext.findModules(newModuleName).iterator().next();
        this.oldModule = this.oldContext.findModules(oldModuleName).iterator().next();
        this.is7950 = rfcVersion == 7950;
    }

    private String extractModuleName(String module) {
        final String[] modRevArray = module.split("/");
        return modRevArray[modRevArray.length - 1].split("@")[0];
    }

    public void printErrors() {
        int order = 1;
        for (final CheckUpdateFromErrorRFC6020 err : errors) {
            err.print(order++);
        }
    }

    public void validate() {
        checkName();
        checkNamespace();
        checkRevision();
        if (is7950) {
            checkIdentities();
        }
        final Collection<? extends DataSchemaNode> childNodes = oldModule.getChildNodes();
        findNodesRecursively(childNodes);

        checkNotifications();
        checkAugmentations();
        checkRPCs();
        checkTypeDefs();

    }

    private void checkIdentities() {
        final Collection<? extends IdentitySchemaNode> oldIdentities = oldModule.getIdentities();
        final Collection<? extends IdentitySchemaNode> newIdentities = newModule.getIdentities();
        for (final IdentitySchemaNode oldIdentity : oldIdentities) {
            boolean identityFound = false;
            for (final IdentitySchemaNode newIdentity : newIdentities) {
                if (oldIdentity.getQName().getLocalName().equals(newIdentity.getQName().getLocalName())) {
                    identityFound = true;

                    final Collection<? extends IdentitySchemaNode> oldBaseIdentities = oldIdentity.getBaseIdentities();
                    final Collection<? extends IdentitySchemaNode> newBaseIdentities = newIdentity.getBaseIdentities();
                    if (oldBaseIdentities.size() > newBaseIdentities.size()) {
                        errors.add(CheckUpdateFromErrorRFC7950.baseIdentityError().updateInformation(newBaseIdentities.toString(),
                                oldBaseIdentities.toString()));
                    } else {
                        for (final IdentitySchemaNode oldBaseIdentity : oldBaseIdentities) {
                            boolean identityBaseFound = false;
                            for (final IdentitySchemaNode newBaseIdentity : newBaseIdentities) {
                                if (oldBaseIdentity.getQName().getLocalName()
                                        .equals(newBaseIdentity.getQName().getLocalName())) {
                                    identityBaseFound = true;
                                    break;
                                }
                            }
                            if (!identityBaseFound) {
                                errors.add(CheckUpdateFromErrorRFC7950.missingBaseIdentityError()
                                        .updateInformation(DONT_EXISTS, oldBaseIdentity.getPath().toString()));
                            }
                        }
                    }
                    break;
                }
            }
            if (!identityFound) {
                errors.add(CheckUpdateFromErrorRFC7950
                        .missingIdentityError().updateInformation(DONT_EXISTS, oldIdentity.getPath().toString()));
            }
        }

    }

    private void checkTypeDefs() {
        final Collection<? extends TypeDefinition<?>> oldTypeDefs = this.oldModule.getTypeDefinitions();
        final Collection<? extends TypeDefinition<?>> newTypeDefs = this.newModule.getTypeDefinitions();
        for (final TypeDefinition<?> oldTypeDef : oldTypeDefs) {
            for (final TypeDefinition<?> newTypeDef : newTypeDefs) {
                if (oldTypeDef.getQName().getLocalName().equals(newTypeDef.getQName().getLocalName())) {
                    checkTypeAware(oldTypeDef, newTypeDef);
                    checkStatus(oldTypeDef.getStatus(), newTypeDef.getStatus(), oldTypeDef.getPath(),
                            newTypeDef.getPath());
                    break;
                }
            }
        }
    }

    private void checkRPCs() {
        final Collection<? extends RpcDefinition> oldRPCs = this.oldModule.getRpcs();
        final Collection<? extends RpcDefinition> newRPCs = this.newModule.getRpcs();
        for (final RpcDefinition oldRPC : oldRPCs) {
            boolean rpcFound = false;
            for (final RpcDefinition newRPC : newRPCs) {
                if (oldRPC.equals(newRPC)) {
                    checkReference(oldRPC.getReference(), newRPC.getReference());
                    checkStatus(oldRPC.getStatus(), newRPC.getStatus(), oldRPC.getPath(), newRPC.getPath());
                    findNodesRecursively(Collections.singletonList(oldRPC.getInput()));
                    findNodesRecursively(Collections.singletonList(oldRPC.getOutput()));
                    rpcFound = true;
                    break;
                }
            }
            if (!rpcFound) {
                errors.add(missingNodeError().updateInformation("missing rpc node",
                        oldRPC.getPath().toString()));
            }
        }
    }

    private void checkAugmentations() {
        final Collection<? extends AugmentationSchemaNode> oldAugmentations = this.oldModule.getAugmentations();
        final Collection<? extends AugmentationSchemaNode> newAugmentations = this.newModule.getAugmentations();
        for (final AugmentationSchemaNode oldAug : oldAugmentations) {
            boolean augFound = false;
            for (final AugmentationSchemaNode newAug : newAugmentations) {
                if (oldAug.getTargetPath().equals(newAug.getTargetPath())) {
                    checkReference(oldAug.getReference(), newAug.getReference());
                    checkStatus(oldAug.getStatus(), newAug.getStatus(), oldAug.getTargetPath(), newAug.getTargetPath());
                    findNodesRecursively(oldAug.getChildNodes());
                    augFound = true;
                    break;
                }
            }
            if (!augFound) {
                errors.add(missingNodeError().updateInformation("missing augmentation node",
                        oldAug.getTargetPath().toString()));
            }
        }
    }

    private void checkNotifications() {
        final Collection<? extends NotificationDefinition> oldNotifications = this.oldModule.getNotifications();
        final Collection<? extends NotificationDefinition> newNotifications = this.newModule.getNotifications();
        for (final NotificationDefinition oldNotification : oldNotifications) {
            boolean notificationFound = false;
            for (final NotificationDefinition newNotification : newNotifications) {
                if (oldNotification.equals(newNotification)) {
                    checkReference(oldNotification.getReference(), newNotification.getReference());
                    checkStatus(oldNotification.getStatus(), newNotification.getStatus(),
                            oldNotification.getPath(), newNotification.getPath());
                    findNodesRecursively(oldNotification.getChildNodes());
                    notificationFound = true;
                    break;
                }
            }
            if (!notificationFound) {
                errors.add(missingNodeError().updateInformation("missing notification node",
                        oldNotification.getPath().toString()));
            }
        }
    }

    private void findNodesRecursively(final Collection<? extends DataSchemaNode> childNodes) {
        for (final DataSchemaNode oldNode : childNodes) {
            final DataSchemaNode newNode = checkNodeExists(oldNode);

            if (newNode != null) {
                checkReference(oldNode.getReference(), newNode.getReference());
                checkMust(oldNode, newNode);
                if (is7950) {
                    checkWhen(oldNode, newNode);
                }
                checkMandatory(oldNode, newNode);
                checkState(oldNode, newNode);
                checkStatus(oldNode.getStatus(), newNode.getStatus(), oldNode.getPath(), newNode.getPath());
                if (oldNode instanceof ElementCountConstraintAware) {
                    checkMinElements(oldNode, newNode);
                    checkMaxElements(oldNode, newNode);
                }
                //     oldNode.getMi

                if (oldNode instanceof TypeAware) {
                    checkTypeAware(((TypeAware) oldNode).getType(), ((TypeAware) newNode).getType());
                }

                if (oldNode instanceof DataNodeContainer) {
                    findNodesRecursively(((DataNodeContainer) oldNode).getChildNodes());
                }
            }
        }
    }

    private void checkTypeAware(TypeDefinition<? extends TypeDefinition<?>> oldType,
                                TypeDefinition<? extends TypeDefinition<?>> newType) {
        final boolean isTypeError = checkType(oldType, newType);
        checkReference(oldType.getReference(),
                newType.getReference());
        checkDefault(oldType, newType);
        checkUnits(oldType, newType);
        if (!isTypeError) {
            if (is7950 && oldType instanceof IdentityrefTypeDefinition) {
                if (((IdentityrefTypeDefinition) oldType).getIdentities().isEmpty()) {
                    errors.add(CheckUpdateFromErrorRFC7950.identityRefBaseError());
                } else if (((IdentityrefTypeDefinition) oldType).getIdentities().size()
                        < ((IdentityrefTypeDefinition) newType).getIdentities().size()) {
                    errors.add(CheckUpdateFromErrorRFC7950.identityRefBaseError()
                            .updateInformation(((IdentityrefTypeDefinition) newType).getIdentities().toString(),
                            ((IdentityrefTypeDefinition) oldType).getIdentities().toString()));
                }
            }


            if (oldType instanceof LengthRestrictedTypeDefinition) {
                checkLength(oldType, newType);
            }

            if (oldType instanceof RangeRestrictedTypeDefinition) {
                checkRange(oldType, newType);
            }

            if (oldType instanceof EnumTypeDefinition) {
                checkEnumeration(oldType, newType);
            } else if (oldType instanceof BitsTypeDefinition) {
                checkBits(oldType, newType);
            } else if (oldType instanceof StringTypeDefinition) {
                checkPattern(oldType, newType);
            }
        }
    }

    private void checkMaxElements(DataSchemaNode oldNode, DataSchemaNode newNode) {
        final Optional<ElementCountConstraint> oldElementCountConstraint =
                ((ElementCountConstraintAware) oldNode).getElementCountConstraint();
        final Optional<ElementCountConstraint> newElementCountConstraint =
                ((ElementCountConstraintAware) newNode).getElementCountConstraint();
        if (newElementCountConstraint.isPresent() && oldElementCountConstraint.isPresent()) {
            final Integer newMaxElements = newElementCountConstraint.get().getMaxElements();
            final Integer oldMaxElements = oldElementCountConstraint.get().getMaxElements();
            if (newMaxElements != null && oldMaxElements != null && newMaxElements < oldMaxElements) {
                errors.add(maxElementsError().updateInformation(newNode.getPath().toString()
                                + MAX_ELEMENTS + newMaxElements,
                        oldNode.getPath().toString() + MAX_ELEMENTS + oldMaxElements));
            }
        }
    }

    private void checkMinElements(DataSchemaNode oldNode, DataSchemaNode newNode) {
        final Optional<ElementCountConstraint> oldElementCountConstraint =
                ((ElementCountConstraintAware) oldNode).getElementCountConstraint();
        final Optional<ElementCountConstraint> newElementCountConstraint =
                ((ElementCountConstraintAware) newNode).getElementCountConstraint();
        if (newElementCountConstraint.isPresent() && oldElementCountConstraint.isEmpty()) {
            if (newElementCountConstraint.get().getMinElements() != null) {
                errors.add(minElementsError().updateInformation(newNode.getPath().toString()
                                + MIN_ELEMENTS + newElementCountConstraint.get().getMinElements(),
                        oldNode.getPath().toString() + MIN_ELEMENTS + DONT_EXISTS));
            }
        } else if (newElementCountConstraint.isPresent()) {
            final Integer newMinElements = newElementCountConstraint.get().getMinElements();
            final Integer oldMinElements = oldElementCountConstraint.get().getMinElements();
            if (newMinElements != null && oldMinElements == null) {
                errors.add(minElementsError().updateInformation(newNode.getPath().toString()
                                + MIN_ELEMENTS + newElementCountConstraint.get().getMinElements(),
                        oldNode.getPath().toString() + MIN_ELEMENTS + DONT_EXISTS));
            } else if (newMinElements != null && oldMinElements < newMinElements) {
                errors.add(minElementsError().updateInformation(newNode.getPath().toString()
                                + MIN_ELEMENTS + newElementCountConstraint.get().getMinElements(),
                        oldNode.getPath().toString() + MIN_ELEMENTS
                                + oldElementCountConstraint.get().getMinElements()));
            }

        }
    }

    private boolean checkType(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final TypeDefinition<?> oldBaseType = baseTypeOf(oldNode);
        final TypeDefinition<?> newBaseType = baseTypeOf(newNode);

        newBaseType.getPath();

        final String oldQname = oldBaseType.getQName().getLocalName();
        final String newQname = newBaseType.getQName().getLocalName();
        if (!oldQname.equals(newQname)) {
            errors.add(typeError().updateInformation(newNode.getPath().toString() + "\ntype: " + newQname,
                    oldNode.getPath().toString() + "\ntype: " + oldQname));
            return true;
        }
        return false;
    }

    private void checkStatus(final Status oldStatus, final Status newStatus, final SchemaNodeIdentifier oldPath,
                             final SchemaNodeIdentifier newPath) {
        if (oldStatus.compareTo(newStatus) > 0) {
            errors.add(statusError().updateInformation(newPath.toString() + "\nstatus: " + newStatus,
                    oldPath.toString() + "\nstatus: " + oldStatus));
        }
    }

    private void checkStatus(final Status oldStatus, final Status newStatus, final SchemaPath oldPath,
                             final SchemaPath newPath) {
        if (oldStatus.compareTo(newStatus) > 0) {
            errors.add(statusError().updateInformation(newPath.toString() + "\nstatus: " + newStatus,
                    oldPath.toString() + "\nstatus: " + oldStatus));
        }
    }

    private void checkState(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        final boolean oldIsConfig = oldNode.isConfiguration();
        final boolean newIsConfig = newNode.isConfiguration();
        if ((!oldIsConfig && newIsConfig)
                && (newNode instanceof MandatoryAware && ((MandatoryAware) newNode).isMandatory())) {
            errors.add(illegalConfigStateError().updateInformation(newNode.getPath().toString()
                            + CONFIG + "true mandatory true",
                    oldNode.getQName().toString() + CONFIG + FALSE));
        } else if (oldIsConfig && !newIsConfig) {
            errors.add(illegalConfigChangeError().updateInformation(newNode.getPath().toString()
                            + CONFIG + FALSE,
                    oldNode.getQName().toString() + CONFIG + "true"));
        }
    }

    private void checkMandatory(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        if (oldNode instanceof MandatoryAware && newNode instanceof MandatoryAware) {
            final boolean oldMandatory = ((MandatoryAware) oldNode).isMandatory();
            final boolean newMandatory = ((MandatoryAware) newNode).isMandatory();
            if (!oldMandatory && newMandatory) {
                errors.add(mandatoryError().updateInformation(newNode.getPath().toString() + "\nmandatory: true",
                        oldNode.getPath().toString() + "\nmandatroy: false"));
            }
        }
    }

    private void checkMust(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        if (newNode instanceof MustConstraintAware) {
            final Collection<? extends MustDefinition> newMust = ((MustConstraintAware) newNode).getMustConstraints();
            if (oldNode instanceof MustConstraintAware) {
                final Collection<? extends MustDefinition> oldMust =
                        ((MustConstraintAware) oldNode).getMustConstraints();
                if (oldMust.size() < newMust.size()) {
                    errors.add(addedMustError().updateInformation(
                            newNode.getPath().toString() + MUST + new ArrayList<>(newMust).toString(),
                            oldNode.getPath().toString() + MUST + new ArrayList<>(oldMust).toString()));
                } else {
                    for (MustDefinition newMustDefinition : newMust) {
                        if (!oldMust.contains(newMustDefinition)) {
                            errors.add(checkMustWarning().updateInformation(
                                    newNode.getPath().toString() + MUST + newMustDefinition.toString(),
                                    oldNode.getPath().toString() + MUST + new ArrayList<>(oldMust).toString()));
                        }
                    }
                }
            } else {
                errors.add(addedMustError().updateInformation(
                        newNode.getPath().toString() + MUST + new ArrayList<>(newMust).toString(),
                        DONT_EXISTS));
            }
        }
    }

    private void checkWhen(final DataSchemaNode oldNode, final DataSchemaNode newNode) {
        final Optional<RevisionAwareXPath> newWhen = newNode.getWhenCondition();
        final Optional<RevisionAwareXPath> oldWhen = oldNode.getWhenCondition();
        if (oldWhen.isEmpty() && newWhen.isPresent()) {
            errors.add(CheckUpdateFromErrorRFC7950.addedWhenError().updateInformation(
                    newNode.getPath().toString() + WHEN + newWhen.get().getOriginalString(),
                    oldNode.getPath().toString() + WHEN + DONT_EXISTS));
        } else if (oldWhen.isPresent() && newWhen.isPresent()) {
            if (!oldWhen.get().getOriginalString().equals(newWhen.get().getOriginalString())) {
                errors.add(CheckUpdateFromErrorRFC7950.checkWhenWarning().updateInformation(
                        newNode.getPath().toString() + WHEN + newWhen.get().getOriginalString(),
                        oldNode.getPath().toString() + WHEN + oldWhen.get().getOriginalString()));

            }
        }
    }

    private void checkReference(final Optional<String> oldReference, final Optional<String> newReference) {
        if (oldReference.isPresent() && newReference.isEmpty()) {
            errors.add(referenceError().updateInformation(DONT_EXISTS, oldReference.get()));
        }
    }

    private void checkUnits(final TypeDefinition<?> oldNode, final TypeDefinition<?> newNode) {
        final Optional<String> oldUnit = oldNode.getUnits();
        final Optional<String> newUnit = newNode.getUnits();
        if (oldUnit.isPresent() && newUnit.isEmpty()) {
            errors.add(unitsError().updateInformation(DONT_EXISTS,
                    oldNode.getPath().toString() + "\nunits: " + oldUnit.get()));
        }
    }

    private void checkDefault(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final Optional<?> oldDefault = oldNode.getDefaultValue();
        final Optional<?> newDefault = newNode.getDefaultValue();
        if (oldDefault.isPresent() && (newDefault.isEmpty() || !oldDefault.get().equals(newDefault.get()))) {
            errors.add(defaultError().updateInformation(DONT_EXISTS,
                    oldNode.getPath().toString() + "\ndefault: " + oldDefault.get().toString()));
        }
    }

    private void checkRange(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final Optional<RangeConstraint> oldRange =
                ((RangeRestrictedTypeDefinition) oldNode).getRangeConstraint();
        final Optional<RangeConstraint> newRange =
                ((RangeRestrictedTypeDefinition) newNode).getRangeConstraint();
        if (oldRange.isPresent()) {
            if (newRange.isEmpty()) {
                errors.add(rangeError().updateInformation(DONT_EXISTS,
                        oldNode.getPath().toString() + RANGES + oldRange.get().toString()));
            } else {
                final RangeConstraint oldRangeConstraint = oldRange.get();
                final RangeConstraint newRangeConstraint = newRange.get();
                final Set newRangeSet = newRangeConstraint.getAllowedRanges().asRanges();
                final Set oldRangeSet = oldRangeConstraint.getAllowedRanges().asRanges();
                if (newRangeSet.containsAll(oldRangeSet)) {
                    checkReference(oldRangeConstraint.getReference(), newRangeConstraint.getReference());
                } else {
                    errors.add(rangeError().updateInformation(
                            newNode.getPath().toString() + RANGES + newRangeSet.toString(),
                            oldNode.getPath().toString() + RANGES + oldRangeSet.toString()));
                }
            }
        }
    }

    private void checkLength(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final Optional<LengthConstraint> oldLengths = ((LengthRestrictedTypeDefinition) oldNode).getLengthConstraint();
        final Optional<LengthConstraint> newLengths = ((LengthRestrictedTypeDefinition) newNode).getLengthConstraint();

        if (oldLengths.isPresent()) {
            if (newLengths.isEmpty()) {
                if (!oldLengths.get().getAllowedRanges().equals(INTEGER_ALLOWED_RANGES)) {
                    errors.add(lengthError().updateInformation(DONT_EXISTS,
                            oldNode.getPath().toString() + LENGTH
                                    + oldLengths.get().getAllowedRanges().toString()));
                }
            } else {
                final LengthConstraint oldLengthConstraint = oldLengths.get();
                final LengthConstraint newLengthConstraint = newLengths.get();
                final Set<Range<Integer>> newRangeSet = newLengthConstraint.getAllowedRanges().asRanges();
                final Set<Range<Integer>> oldRangeSet = oldLengthConstraint.getAllowedRanges().asRanges();
                if (newRangeSet.containsAll(oldRangeSet)) {
                    checkReference(oldLengthConstraint.getReference(), newLengthConstraint.getReference());
                } else {
                    errors.add(lengthError().updateInformation(
                            newNode.getPath().toString() + LENGTH + newRangeSet.toString(),
                            oldNode.getPath().toString() + LENGTH + oldRangeSet.toString()));
                }
            }
        }
    }

    private void checkPattern(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final List<PatternConstraint> oldPatterns = ((StringTypeDefinition) oldNode).getPatternConstraints();
        final List<PatternConstraint> newPatterns = ((StringTypeDefinition) newNode).getPatternConstraints();
        if (newPatterns.containsAll(oldPatterns)) {
            for (PatternConstraint oldPattern : oldPatterns) {
                checkReference(oldPattern.getReference(),
                        newPatterns.get(newPatterns.indexOf(oldPattern)).getReference());
            }
        } else {
            errors.add(patternError().updateInformation(newPatterns.toString(), oldPatterns.toString()));
        }
    }

    private void checkBits(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final Collection<? extends BitsTypeDefinition.Bit> oldBits = ((BitsTypeDefinition) oldNode).getBits();
        final Collection<? extends BitsTypeDefinition.Bit> newBits = ((BitsTypeDefinition) newNode).getBits();
        for (final BitsTypeDefinition.Bit oldBit : oldBits) {
            boolean sameBit = false;
            for (final BitsTypeDefinition.Bit newBit : newBits) {
                if (oldBit.getName().equals(newBit.getName()) && oldBit.getPosition().equals(newBit.getPosition())) {
                    checkReference(oldBit.getReference(), newBit.getReference());
                    sameBit = true;
                }
            }
            if (!sameBit) {
                errors.add(missingBitError().updateInformation(newBits.toString(), oldBits.toString()));
            }
        }
    }

    private void checkEnumeration(final TypeDefinition oldNode, final TypeDefinition newNode) {
        final List<EnumTypeDefinition.EnumPair> oldValues = ((EnumTypeDefinition) oldNode).getValues();
        final List<EnumTypeDefinition.EnumPair> newValues = ((EnumTypeDefinition) newNode).getValues();
        if (newValues.containsAll(oldValues)) {
            for (final EnumTypeDefinition.EnumPair oldEnum : oldValues) {
                checkReference(oldEnum.getReference(), newValues.get(newValues.indexOf(oldEnum)).getReference());
            }
        } else {
            errors.add(missingEnumError().updateInformation(newValues.toString(), oldValues.toString()));
        }
    }

    private DataSchemaNode checkNodeExists(final DataSchemaNode node) {
        final List<QName> finalList = new LinkedList<>();
        for (final QName qName : node.getPath().getPathFromRoot()) {
            finalList.add(QName.create(newModule.getNamespace(), newModule.getRevision(), qName.getLocalName()));
        }

        final Optional<DataSchemaNode> dataChildByName = newModule.findDataTreeChild(finalList);
        if (dataChildByName.isPresent()) {
            return dataChildByName.get();
        } else {
            errors.add(missingNodeError().updateInformation("missing node", node.getPath().toString()));
            return null;
        }
    }

    private void checkRevision() {
        final Optional<Revision> newOptionalRevision = this.newModule.getRevision();
        final Optional<Revision> oldOptionalRevision = this.oldModule.getRevision();
        if (newOptionalRevision.isEmpty()) {
            errors.add(missingRevision());
        } else {
            final Revision revision = newOptionalRevision.get();
            if (oldOptionalRevision.isPresent() && revision.compareTo(oldOptionalRevision.get()) < 1) {
                errors.add(revisionError().updateInformation(revision.toString(),
                        oldOptionalRevision.get().toString()));
            }

            final Collection<? extends RevisionStatement> revisionsNew =
                    ((ModuleEffectiveStatement) newModule).getDeclared().getRevisions();
            final Collection<? extends RevisionStatement> revisionsOld =
                    ((ModuleEffectiveStatement) oldModule).getDeclared().getRevisions();

            final List<Revision> newDates = revisionsNew
                    .stream()
                    .map(RevisionStatement::getDate)
                    .collect(Collectors.toList());
            for (RevisionStatement oldRev : revisionsOld) {
                if (!newDates.contains(oldRev.getDate())) {
                    errors.add(missingOldRevision().updateInformation(DONT_EXISTS, oldRev.getDate().toString()));
                }
            }
        }
    }

    private void checkName() {
        if (!this.newModule.getName().equals(this.oldModule.getName())) {
            errors.add(nameError().updateInformation(this.newModule.getName(), this.oldModule.getName()));
        }
    }

    private void checkNamespace() {
        if (!this.newModule.getNamespace().equals(this.oldModule.getNamespace())) {
            errors.add(namespaceError().updateInformation(this.newModule.getNamespace().toString(),
                    this.oldModule.getNamespace().toString()));
        }
    }

    public static GroupArguments getGroupArguments() {
        final GroupArguments groupArguments = new GroupArguments("check-update-from",
                "Check-update-from based arguments: ");
        groupArguments.addOption("check update from path is a colon (:)"
                        + " separated list of directories to search"
                        + " for yang modules used on \"old module\".",
                Arrays.asList("-P", "--check-update-from-path"), false, "*", Collections.emptyList(),
                new CollectionArgumentChoice<>(Collections.emptyList()), List.class);
        groupArguments.addOption("Choose the RFC (7950 or 6020)"
                        + " according to which check will be processed.",
                Collections.singletonList("--rfc-version"), false, "?", 6020,
                new CollectionArgumentChoice<>(Arrays.asList(6020, 7950)), Integer.TYPE);
        return groupArguments;
    }

}
