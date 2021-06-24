/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.gnmi.simulatordevice.gnmi;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import gnmi.Gnmi;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.lighty.modules.southbound.gnmi.commons.util.DataConverter;
import io.lighty.modules.southbound.gnmi.commons.util.JsonUtils;
import io.lighty.modules.southbound.gnmi.simulatordevice.yang.DatastoreType;
import io.lighty.modules.southbound.gnmi.simulatordevice.yang.YangDataService;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
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
            final Optional<NormalizedNode<?, ?>> optNode = dataService.readDataByPath(datastoreType, entry.getValue());

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
                                                       final NormalizedNode<?, ?> node) {
        final Optional<? extends Module> module = DataConverter.findModuleByQName(node.getNodeType(), context);
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
                LOG.error("Unable to find a module for the path {}, ignored...", update.getPath().toString());
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
        final NormalizedNode<?, ?> node;
        YangInstanceIdentifier resultingIdentifier = identifier;
        final Gnmi.UpdateResult result;

        if (!(identifier.getLastPathArgument() instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates)) {
            json = JsonUtils.wrapJsonWithObject(update.getVal().getJsonIetfVal().toStringUtf8(),
                    String.format("%s:%s",
                            module.getName(),
                            Iterables.getLast(identifier.getPathArguments()).getNodeType().getLocalName()), gson);
            node = DataConverter.nodeFromJsonString(identifier, json, context);
        } else {
            json = JsonUtils.wrapJsonWithArray(update.getVal().getJsonIetfVal().toStringUtf8(),
                    String.format("%s:%s",
                            module.getName(),
                            Iterables.getLast(identifier.getPathArguments()).getNodeType().getLocalName()), gson);
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
        final Optional<NormalizedNode<?, ?>> updatingNode
                = dataService.readDataByPath(DatastoreType.CONFIGURATION, identifier);
        if (updatingNode.isEmpty()) {
            LOG.error("Update for non existing simple value is not permitted");
            throw Status.NOT_FOUND.withDescription("Update for non existing simple value is not permitted")
                    .asRuntimeException();
        }
        // Modify existing simple value
        final String simpleJson = getSimpleJsonValue(update.getVal(), update.getPath());
        final NormalizedNode<?, ?> resultNode = DataConverter.nodeFromJsonString(identifier, simpleJson, context);
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

        final Optional<? extends Module> rootModule = DataConverter.findModule(pathFirstElem, context);
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


    private YangInstanceIdentifier instanceIdentifierFromPath(final QNameModule rootModule,
                                                              final Gnmi.Path path) {
        QName qname = null;
        YangInstanceIdentifier identifier = null;
        for (final Gnmi.PathElem pathElem : path.getElemList()) {
            if (qname == null) {
                qname = QName.create(rootModule, pathElem.getName());
                identifier = YangInstanceIdentifier.of(qname);
            } else {
                final Optional<? extends DataSchemaNode> augmentationDataNode
                        = DataConverter.findAugmentationDataNode(pathElem.getName(), context);
                if (augmentationDataNode.isPresent()) {
                    identifier = addAugmentationNodeToIdentifier(identifier, augmentationDataNode.get());
                } else {
                    qname = QName.create(identifier.getLastPathArgument().getNodeType(), pathElem.getName());
                    identifier = identifier.node(qname);
                }
            }
            if (!pathElem.getKeyMap().isEmpty()) {
                final QName finalQname = qname;
                final Map<QName, Object> keysMap = pathElem.getKeyMap().entrySet().stream().collect(
                        Collectors.toMap(e -> QName.create(finalQname, e.getKey()), Map.Entry::getValue)
                );
                identifier = identifier.node(YangInstanceIdentifier.NodeIdentifierWithPredicates.of(qname,
                        keysMap));
            }
        }
        return identifier;
    }

    private YangInstanceIdentifier addAugmentationNodeToIdentifier(final YangInstanceIdentifier identifier,
                                                                   final DataSchemaNode augmentationDataNode) {
        final HashSet<QName> augmentationQname = new HashSet<>();
        augmentationQname.add(augmentationDataNode.getQName());
        return identifier
                .node(AugmentationIdentifier.create(augmentationQname))
                .node(augmentationDataNode.getQName());
    }

}
