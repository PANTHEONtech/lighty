/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.gnmi.simulatordevice.gnmi;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import gnmi.Gnmi;
import gnmi.Gnmi.Path;
import gnmi.Gnmi.PathElem;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.lighty.modules.gnmi.commons.util.DataConverter;
import io.lighty.modules.gnmi.commons.util.ElementNameWithModuleName;
import io.lighty.modules.gnmi.commons.util.JsonUtils;
import io.lighty.modules.gnmi.simulatordevice.yang.DatastoreType;
import io.lighty.modules.gnmi.simulatordevice.yang.YangDataService;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.TypeDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class GnmiCrudService {
    private static final Logger LOG = LoggerFactory.getLogger(GnmiCrudService.class);

    private final YangDataService dataService;
    private final EffectiveModelContext context;
    private final Gson gson;

    public GnmiCrudService(final YangDataService dataService, final EffectiveModelContext context, final Gson gson) {
        this.dataService = dataService;
        this.context = context;
        this.gson = gson;
    }

    // The response is always in json format, even with simple types
    Gnmi.GetResponse get(final Gnmi.GetRequest request) {
        // create YANG instance identifiers from the Gnmi.Path
        final Map<Gnmi.Path, YangInstanceIdentifier> identifierMap = pathToIdentifierMap(request.getPathList());

        // Get config data from in-memory storage and store under the map
        final Map<Gnmi.Path, String> resultMap = new HashMap<>();
        for (final Map.Entry<Gnmi.Path, YangInstanceIdentifier> entry : identifierMap.entrySet()) {

            // support only configuration and operational types for now
            final DatastoreType datastoreType;
            if (request.getType() == Gnmi.GetRequest.DataType.STATE) {
                datastoreType = DatastoreType.STATE;
            } else {
                datastoreType = DatastoreType.CONFIGURATION;
            }
            final Optional<NormalizedNode> optNode = dataService.readDataByPath(datastoreType, entry.getValue());

            optNode.ifPresent(node -> {
                Map.Entry<Gnmi.Path, String> jsonResult = getResultInJsonFormat(entry, node);
                resultMap.putIfAbsent(jsonResult.getKey(), jsonResult.getValue());
            });
        }

        // Start building response
        final Gnmi.GetResponse.Builder responseBuilder = Gnmi.GetResponse.newBuilder();

        // Map data from in-memory storage to getResponse data structure
        for (final Map.Entry<Gnmi.Path, String> entry : resultMap.entrySet()) {

            final Gnmi.TypedValue typedValue = Gnmi.TypedValue.newBuilder()
                    .setJsonIetfVal(ByteString.copyFromUtf8(entry.getValue()))
                    .build();

            final Gnmi.Update update = Gnmi.Update.newBuilder().setPath(entry.getKey()).setVal(typedValue).build();
            final long timeStampMillis = Instant.now().toEpochMilli();
            final Gnmi.Notification notification = Gnmi.Notification.newBuilder().addUpdate(update)
                    .setTimestamp(timeStampMillis).build();
            responseBuilder.addNotification(notification);
        }

        // finally return response with all notifications
        if (responseBuilder.getNotificationCount() > 0) {
            return responseBuilder.build();
        } else {
            throw new StatusRuntimeException(Status.NOT_FOUND, new Metadata());
        }
    }

    Gnmi.SetResponse set(final Gnmi.SetRequest request) {
        final Gnmi.SetResponse.Builder builder = Gnmi.SetResponse.newBuilder();

        if (request.getReplaceCount() > 0) {
            final List<Gnmi.UpdateResult> replaceResults = processUpdateList(request.getReplaceList(), true);
            builder.addAllResponse(replaceResults);
        }

        // delete section
        if (request.getDeleteCount() > 0) {
            final List<Gnmi.UpdateResult> deleteResults = processDelete(request.getDeleteList());
            builder.addAllResponse(deleteResults);
        }

        // update section
        if (request.getUpdateCount() > 0) {
            final List<Gnmi.UpdateResult> updateResults = processUpdateList(request.getUpdateList(), false);
            builder.addAllResponse(updateResults);
        }

        return builder.build();
    }

    Map.Entry<Gnmi.Path, String> getResultInJsonFormat(final Map.Entry<Gnmi.Path, YangInstanceIdentifier> entry,
                                                       final NormalizedNode node) {
        final Optional<? extends Module> module
                = DataConverter.findModuleByQName(node.name().getNodeType(), context);
        final String moduleName = module.map(Module::getName).orElse(null);

        final String jsonValue = DataConverter.jsonStringFromNormalizedNodes(entry.getValue(), node, context);
        final String jsonWithModuleNamePrefix = JsonUtils.addModuleNamePrefixToJson(jsonValue, moduleName, gson);
        return new SimpleEntry<>(entry.getKey(), jsonWithModuleNamePrefix);
    }

    private List<Gnmi.UpdateResult> processDelete(final List<Gnmi.Path> paths) {
        final List<Gnmi.UpdateResult> deleteResults = new ArrayList<>();
        final Map<Gnmi.Path, YangInstanceIdentifier> identifierMap = pathToIdentifierMap(paths);
        for (final Gnmi.Path path : paths) {
            dataService.deleteDataByPath(DatastoreType.CONFIGURATION, identifierMap.get(path));
            deleteResults.add(Gnmi.UpdateResult.newBuilder()
                    .setPath(path)
                    .setOp(Gnmi.UpdateResult.Operation.DELETE)
                    .build());
        }
        return deleteResults;
    }

    private List<Gnmi.UpdateResult> processUpdateList(final List<Gnmi.Update> updateList, final boolean isReplace) {
        final List<Gnmi.UpdateResult> results = new ArrayList<>();
        final List<Gnmi.Path> pathList = updateList.stream().map(Gnmi.Update::getPath)
                .collect(Collectors.toList());
        final Map<Gnmi.Path, YangInstanceIdentifier> identifierMap = pathToIdentifierMap(pathList);

        for (final Gnmi.Update update : updateList) {
            final YangInstanceIdentifier identifier = identifierMap.get(update.getPath());
            final Optional<? extends Module> optModule = DataConverter.findModuleByQName(identifier
                    .getLastPathArgument().getNodeType(), context);
            if (optModule.isEmpty()) {
                LOG.error("Unable to find a module for the path {}, ignored...", update.getPath());
            } else {
                Gnmi.UpdateResult updateResult;
                if (!update.getVal().getJsonIetfVal().isEmpty()) {
                    // Json sets are permitted only for non simple types
                    updateResult = processUpdateListNonSimpleValue(update, identifier, optModule.get(), isReplace);
                } else {
                    updateResult = processUpdateListSimpleValue(update, identifier);
                }
                results.add(updateResult);

            }
        }
        return results;
    }

    private Gnmi.UpdateResult processUpdateListNonSimpleValue(final Gnmi.Update update,
                                                              final YangInstanceIdentifier identifier,
                                                              final Module module, final boolean isReplace) {
        // list entries need to also be wrapped in a list
        final String json;
        final NormalizedNode node;
        YangInstanceIdentifier resultingIdentifier = identifier;
        final Gnmi.UpdateResult result;

        if (!(identifier.getLastPathArgument() instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates)) {
            json = JsonUtils.wrapJsonWithObject(update.getVal().getJsonIetfVal().toStringUtf8(),
                    String.format("%s:%s",
                            module.getName(),
                            Iterables.getLast(identifier.getPathArguments()).getNodeType().getLocalName()), gson);
            node = DataConverter.nodeFromJsonString(identifier, json, context);
        } else {
            final NodeIdentifierWithPredicates lastPathArgument
                    = (NodeIdentifierWithPredicates) identifier.getLastPathArgument();
            json = JsonUtils.wrapJsonWithArray(update.getVal().getJsonIetfVal().toStringUtf8(),
                    String.format("%s:%s",
                            module.getName(),
                            Iterables.getLast(identifier.getPathArguments()).getNodeType().getLocalName()), gson,
                    lastPathArgument, context);
            node = DataConverter.nodeFromJsonString(identifier, json, context);
            // In case of list entry, point to the list itself
            resultingIdentifier = identifier.getParent();
        }

        if (isReplace) {
            dataService.writeDataByPath(DatastoreType.CONFIGURATION, resultingIdentifier,
                    node);
            result = Gnmi.UpdateResult.newBuilder().setPath(update.getPath())
                    .setOp(Gnmi.UpdateResult.Operation.REPLACE).build();

        } else {
            dataService.mergeDataByPath(DatastoreType.CONFIGURATION, resultingIdentifier,
                    node);
            result = Gnmi.UpdateResult.newBuilder().setPath(update.getPath())
                    .setOp(Gnmi.UpdateResult.Operation.UPDATE).build();
        }
        return result;
    }

    private Gnmi.UpdateResult processUpdateListSimpleValue(final Gnmi.Update update,
                                                           final YangInstanceIdentifier identifier) {
        final Optional<NormalizedNode> updatingNode
                = dataService.readDataByPath(DatastoreType.CONFIGURATION, identifier);
        if (updatingNode.isEmpty()) {
            LOG.error("Update for non existing simple value is not permitted");
            throw Status.NOT_FOUND.withDescription("Update for non existing simple value is not permitted")
                    .asRuntimeException();
        }
        // Modify existing simple value
        final String simpleJson = getSimpleJsonValue(update.getVal(), update.getPath());
        final NormalizedNode resultNode = DataConverter.nodeFromJsonString(identifier, simpleJson, context);
        dataService.mergeDataByPath(DatastoreType.CONFIGURATION, identifier, resultNode);
        return Gnmi.UpdateResult.newBuilder()
                .setPath(update.getPath())
                .setOp(Gnmi.UpdateResult.Operation.UPDATE)
                .build();
    }

    /**
     * Create simple json with one element.
     *
     * @param value simple value
     * @param path  path related to value
     * @return simple json in format {LAST_PATH_ELEMENT:VALUE}"
     */
    private String getSimpleJsonValue(final Gnmi.TypedValue value, final Gnmi.Path path) {
        final StringBuilder jsonValue = new StringBuilder();
        jsonValue.append("{");
        final String jsonElementName = path.getElem(path.getElemCount() - 1).getName();
        jsonValue.append(jsonElementName).append(":");
        switch (value.getValueCase()) {
            case STRING_VAL:
                jsonValue.append("\"").append(value.getStringVal()).append("\"");
                break;
            case INT_VAL:
                jsonValue.append(value.getIntVal());
                break;
            case UINT_VAL:
                jsonValue.append(value.getUintVal());
                break;
            case BOOL_VAL:
                jsonValue.append(value.getBoolVal());
                break;
            case FLOAT_VAL:
                jsonValue.append(value.getFloatVal());
                break;
            case DECIMAL_VAL:
                jsonValue.append(value.getDecimalVal());
                break;
            default:
                LOG.error("ValueType {} not supported", value);
                throw Status.INVALID_ARGUMENT.withDescription("ValueType " + value + " not supported")
                        .asRuntimeException();
        }
        jsonValue.append("}");
        return jsonValue.toString();
    }

    Map<Gnmi.Path, YangInstanceIdentifier> pathToIdentifierMap(final List<Gnmi.Path> pathList) {
        // Assumption: we always have path with at least one element
        final String pathFirstElem = pathList.get(0).getElem(0).getName();

        final Optional<? extends Module> rootModule = DataConverter.findModuleByElement(pathFirstElem, context);
        if (rootModule.isEmpty()) {
            LOG.error("Unable to guess correct module for provided path (first elem: {})", pathFirstElem);
            return Collections.emptyMap();
        }
        final QNameModule qNameModule = rootModule.get().getQNameModule();

        final Map<Gnmi.Path, YangInstanceIdentifier> resultMap = new HashMap<>();
        for (final Gnmi.Path reqPath : pathList) {
            resultMap.computeIfAbsent(reqPath, v -> instanceIdentifierFromPath(qNameModule, v));
        }
        return resultMap;
    }

    private YangInstanceIdentifier instanceIdentifierFromPath(final QNameModule rootModule, final Path path) {
        var resultIdentifier = YangInstanceIdentifier.empty();
        for (final PathElem pathElem : path.getElemList()) {
            final var pathElemName = ElementNameWithModuleName.parseFromString(pathElem.getName()).getElementName();
            if (resultIdentifier.isEmpty()) {
                resultIdentifier = YangInstanceIdentifier.of(QName.create(rootModule, pathElemName));
            } else {
                resultIdentifier = getYIIDWithNewNode(resultIdentifier, pathElemName);
            }
            if (!pathElem.getKeyMap().isEmpty()) {
                resultIdentifier = getYIIDWithNewPredicateNode(resultIdentifier, pathElem);
            }
        }
        return resultIdentifier;
    }

    private YangInstanceIdentifier getYIIDWithNewNode(final YangInstanceIdentifier identifier,
            final String pathElement) {
        final QName expectedQname = QName.create(identifier.getLastPathArgument().getNodeType(), pathElement);
        final YangInstanceIdentifier expectedYIID = identifier.node(expectedQname);
        final var foundNode = DataSchemaContextTree.from(context).enterPath(expectedYIID);
        if (foundNode.isPresent()) {
            return expectedYIID;
        } else {
            // If an element by name is not found inside parent yang model, then is augmented from other YANG model.
            final var foundAug = findAugmentationFromOuterModel(pathElement, identifier);
            return identifier.node(foundAug.orElseThrow().getQName());
        }
    }

    private Optional<DataSchemaNode> findAugmentationFromOuterModel(final String element,
            final YangInstanceIdentifier parentYIID) {
        final Absolute parentPath = DataSchemaContextTree.from(context)
                .enterPath(parentYIID)
                .orElseThrow()
                .stack()
                .toSchemaNodeIdentifier();
        return context.getModules()
                .stream()
                .flatMap(module -> module.getAugmentations().stream())
                .filter(aug -> aug.getTargetPath().equals(parentPath))
                .flatMap(augmentation -> augmentation.getChildNodes().stream())
                .filter(childNode -> childNode.getQName().getLocalName().equals(element))
                .map(DataSchemaNode.class::cast)
                .findFirst();
    }

    private YangInstanceIdentifier getYIIDWithNewPredicateNode(final YangInstanceIdentifier resultIdentifier,
            final PathElem currentElement) {
        final var qname = resultIdentifier.getLastPathArgument().getNodeType();
        final Map<QName, Object> keysMap = currentElement.getKeyMap().entrySet().stream()
            .collect(Collectors.toMap(e -> QName.create(qname, e.getKey()), Map.Entry::getValue));

        for (final var entry : keysMap.entrySet()) {
            final var keyNode = resultIdentifier.node(NodeIdentifierWithPredicates.of(qname, keysMap))
                .node(entry.getKey());
            final var baseTypeDef = getBaseTypeDef(keyNode);
            entry.setValue(mapToCorrectDataType(baseTypeDef, (String) entry.getValue()));
        }
        return resultIdentifier.node(NodeIdentifierWithPredicates.of(qname, keysMap));
    }

    private TypeDefinition<?> getBaseTypeDef(final YangInstanceIdentifier identifier) {
        final var nodeAndStack = DataSchemaContextTree.from(context).enterPath(identifier).get();
        final var dataSchemaNode = nodeAndStack.node().dataSchemaNode();
        var resultDataSchemaType = ((TypedDataSchemaNode) dataSchemaNode).getType();
        if (resultDataSchemaType instanceof LeafrefTypeDefinition leafRefType) {
            final var leafRefPathOrig = leafRefType.getPathStatement().getOriginalString();
            final var leafRefPathList = Arrays.stream(leafRefPathOrig.split("/")).filter(s -> !s.isEmpty()).toList();
            final var stack = nodeAndStack.stack();
            for (final var path : leafRefPathList) {
                if ("..".equals(path)) {
                    stack.exit();
                } else {
                    stack.enterSchemaTree(QName.create(stack.currentModule().localQNameModule(), path));
                }
            }
            resultDataSchemaType = ((TypedDataSchemaNode) stack.currentStatement()).getType();
        }
        return resultDataSchemaType.getBaseType() != null ? resultDataSchemaType.getBaseType() : resultDataSchemaType;
    }

    private Object mapToCorrectDataType(final TypeDefinition<?> typeDefinition, final String value) {
        final var qname = typeDefinition.getQName();
        if (typeDefinition instanceof IdentityrefTypeDefinition identityType) {
            final var firstIdentity = identityType.getIdentities().iterator().next();
            final var identityQname = firstIdentity.getQName();
            final var values = value.split(":");
            final var identityRefName = values[values.length - 1];
            return QName.create(identityQname, identityRefName);
        } else if (qname.equals(TypeDefinitions.BOOLEAN)) {
            return Boolean.valueOf(value);
        } else if (qname.equals(TypeDefinitions.DECIMAL64)) {
            return Decimal64.valueOf(value);
        } else if (qname.equals(TypeDefinitions.INT8) || qname.equals(TypeDefinitions.INT16)
                || qname.equals(TypeDefinitions.INT32) || qname.equals(TypeDefinitions.INT64)) {
            return Integer.parseInt(value);
        } else if (qname.equals(TypeDefinitions.UINT8)) {
            return Uint8.valueOf(value);
        } else if (qname.equals(TypeDefinitions.UINT16)) {
            return Uint16.valueOf(value);
        } else if (qname.equals(TypeDefinitions.UINT32)) {
            return Uint32.valueOf(value);
        } else if (qname.equals(TypeDefinitions.UINT64)) {
            return Uint64.valueOf(value);
        } else {
            // Other types which can be sent as a String type.
            return value;
        }
    }
}
