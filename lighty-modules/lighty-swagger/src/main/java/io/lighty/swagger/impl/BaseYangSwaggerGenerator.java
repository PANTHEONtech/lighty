/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.opendaylight.netconf.sal.rest.doc.impl.ModelGenerator;
import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder.Delete;
import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder.Get;
import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder.Post;
import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder.Put;
import org.opendaylight.netconf.sal.rest.doc.swagger.Api;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.Operation;
import org.opendaylight.netconf.sal.rest.doc.swagger.Parameter;
import org.opendaylight.netconf.sal.rest.doc.swagger.Resource;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.opendaylight.netconf.sal.rest.doc.util.RestDocgenUtil.resolvePathArgumentsName;

public class BaseYangSwaggerGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(org.opendaylight.netconf.sal.rest.doc.impl.BaseYangSwaggerGenerator.class);

    protected static final String API_VERSION = "1.0.0";
    protected static final String SWAGGER_VERSION = "1.2";
    protected static final String RESTCONF_CONTEXT_ROOT = "restconf";
    private static final String RESTCONF_DRAFT = "18";

    static final String MODULE_NAME_SUFFIX = "_module";
    private final ModelGenerator jsonConverter = new ModelGenerator();

    // private Map<String, ApiDeclaration> MODULE_DOC_CACHE = new HashMap<>()
    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean newDraft;

    protected BaseYangSwaggerGenerator() {
        this.mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * Return list of modules converted to swagger compliant resource list.
     */
    public ResourceList getResourceListing(final UriInfo uriInfo, final SchemaContext schemaContext,
            final String context) {

        final ResourceList resourceList = createResourceList();

        final Set<Module> modules = getSortedModules(schemaContext);

        final List<Resource> resources = new ArrayList<>(modules.size());

        LOG.info("Modules found [{}]", modules.size());

        for (final Module module : modules) {
            final String revisionString = module.getQNameModule().getRevision().map(Revision::toString).orElse(null);
            final Resource resource = new Resource();
            LOG.debug("Working on [{},{}]...", module.getName(), revisionString);
            final ApiDeclaration doc =
                    getApiDeclaration(module.getName(), revisionString, uriInfo, schemaContext, context);

            if (doc != null) {
                resource.setPath(generatePath(uriInfo, module.getName(), revisionString));
                resources.add(resource);
            } else {
                LOG.warn("Could not generate doc for {},{}", module.getName(), revisionString);
            }
        }

        resourceList.setApis(resources);

        return resourceList;
    }

    protected ResourceList createResourceList() {
        final ResourceList resourceList = new ResourceList();
        resourceList.setApiVersion(API_VERSION);
        resourceList.setSwaggerVersion(SWAGGER_VERSION);
        return resourceList;
    }

    protected String generatePath(final UriInfo uriInfo, final String name, final String revision) {
        final URI uri = uriInfo.getRequestUriBuilder().path(generateCacheKey(name, revision)).build();
        return uri.toASCIIString();
    }

    public ApiDeclaration getApiDeclaration(final String moduleName, final String revision, final UriInfo uriInfo,
                                            final SchemaContext schemaContext, final String context) {
        final Optional<Revision> rev;

        try {
            rev = Revision.ofNullable(revision);
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }

        final Module module = schemaContext.findModule(moduleName, rev).orElse(null);
        Preconditions.checkArgument(module != null,
                "Could not find module by name,revision: " + moduleName + "," + revision);

        return getApiDeclaration(module, uriInfo, context, schemaContext);
    }

    public ApiDeclaration getApiDeclaration(final Module module, final UriInfo uriInfo,
                                            final String context, final SchemaContext schemaContext) {
        final String basePath = createBasePathFromUriInfo(uriInfo);

        final ApiDeclaration doc = getSwaggerDocSpec(module, basePath, context, schemaContext);
        if (doc != null) {
            return doc;
        }
        return null;
    }

    protected String createBasePathFromUriInfo(final UriInfo uriInfo) {
        String portPart = "";
        final int port = uriInfo.getBaseUri().getPort();
        if (port != -1) {
            portPart = ":" + port;
        }
        final String basePath =
                new StringBuilder(uriInfo.getBaseUri().getScheme()).append("://").append(uriInfo.getBaseUri().getHost())
                        .append(portPart).append("/").append(RESTCONF_CONTEXT_ROOT).toString();
        return basePath;
    }

    public ApiDeclaration getSwaggerDocSpec(final Module module, final String basePath, final String context,
                                            final SchemaContext schemaContext) {
        final ApiDeclaration doc = createApiDeclaration(basePath);

        final List<Api> apis = new ArrayList<>();
        boolean hasAddRootPostLink = false;

        final Collection<DataSchemaNode> dataSchemaNodes = module.getChildNodes();
        LOG.debug("child nodes size [{}]", dataSchemaNodes.size());
        for (final DataSchemaNode node : dataSchemaNodes) {
            if (node instanceof ListSchemaNode || node instanceof ContainerSchemaNode) {
                LOG.debug("Is Configuration node [{}] [{}]", node.isConfiguration(), node.getQName().getLocalName());

                List<Parameter> pathParams = new ArrayList<>();
                String resourcePath;

                /*
                 * Only when the node's config statement is true, such apis as
                 * GET/PUT/POST/DELETE config are added for this node.
                 */
                if (node.isConfiguration()) { // This node's config statement is
                                              // true.
                    resourcePath = getDataStorePath("config", context);

                    /*
                     * When there are two or more top container or list nodes
                     * whose config statement is true in module, make sure that
                     * only one root post link is added for this module.
                     */
                    if (!hasAddRootPostLink) {
                        LOG.debug("Has added root post link for module {}", module.getName());
                        addRootPostLink(module, (DataNodeContainer) node, pathParams, resourcePath, "config", apis);

                        hasAddRootPostLink = true;
                    }

                    addApis(node, apis, resourcePath, pathParams, schemaContext, true, module.getName(), "config");
                }
                pathParams = new ArrayList<>();
                resourcePath = getDataStorePath("operational", context);

                addApis(node, apis, resourcePath, pathParams, schemaContext, false, module.getName(), "operational");
            }
        }

        final Set<RpcDefinition> rpcs = module.getRpcs();
        for (final RpcDefinition rpcDefinition : rpcs) {
            final String resourcePath;
            resourcePath = getDataStorePath("operations", context);

            addRpcs(rpcDefinition, apis, resourcePath, schemaContext);
        }

        LOG.debug("Number of APIs found [{}]", apis.size());

        if (!apis.isEmpty()) {
            doc.setApis(apis);
            ObjectNode models = null;

            try {
                models = this.jsonConverter.convertToJsonSchema(module, schemaContext);
                doc.setModels(models);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this.mapper.writeValueAsString(doc));
                }
            } catch (IOException e) {
                LOG.error("Exception occured in ModelGenerator", e);
            }

            return doc;
        }
        return null;
    }

    private static void addRootPostLink(final Module module, final DataNodeContainer node,
                                        final List<Parameter> pathParams, final String resourcePath, final String dataStore, final List<Api> apis) {
        if (containsListOrContainer(module.getChildNodes())) {
            final Api apiForRootPostUri = new Api();
            apiForRootPostUri.setPath(resourcePath.concat(getContent(dataStore)));
            apiForRootPostUri.setOperations(operationPost(module.getName() + MODULE_NAME_SUFFIX,
                    module.getDescription().orElse(null), module, pathParams, true, ""));
            apis.add(apiForRootPostUri);
        }
    }

    protected ApiDeclaration createApiDeclaration(final String basePath) {
        final ApiDeclaration doc = new ApiDeclaration();
        doc.setApiVersion(API_VERSION);
        doc.setSwaggerVersion(SWAGGER_VERSION);
        doc.setBasePath(basePath);
        doc.setProduces(Arrays.asList("application/json", "application/xml"));
        return doc;
    }

    protected String getDataStorePath(final String dataStore, final String context) {
        if (newDraft) {
//            if ("config".contains(dataStore) || "operational".contains(dataStore)) {
//                return "/" + RESTCONF_DRAFT + "/data" + context;
//            }
//            return "/" + RESTCONF_DRAFT + "/operations" + context;

            // LIGHTY-CHANGE-START
            if ("config".contains(dataStore) || "operational".contains(dataStore)) {
                return "/data" + context;
            }
            return "/operations" + context;
            // LIGHTY-CHANGE-END
        }

        return "/" + dataStore + context;
    }

    private static String generateCacheKey(final String module, final String revision) {
        return module + "(" + revision + ")";
    }

    private void addApis(final DataSchemaNode node, final List<Api> apis, final String parentPath,
                         final List<Parameter> parentPathParams, final SchemaContext schemaContext, final boolean addConfigApi,
                         final String parentName, final String dataStore) {
        final Api api = new Api();
        final List<Parameter> pathParams = new ArrayList<>(parentPathParams);

        final String resourcePath = parentPath + "/" + createPath(node, pathParams, schemaContext);
        LOG.debug("Adding path: [{}]", resourcePath);
        api.setPath(resourcePath.concat(getContent(dataStore)));

        Iterable<DataSchemaNode> childSchemaNodes = Collections.<DataSchemaNode>emptySet();
        if (node instanceof ListSchemaNode || node instanceof ContainerSchemaNode) {
            final DataNodeContainer dataNodeContainer = (DataNodeContainer) node;
            childSchemaNodes = dataNodeContainer.getChildNodes();
        }
        api.setOperations(operation(node, pathParams, addConfigApi, childSchemaNodes, parentName));
        apis.add(api);

        for (final DataSchemaNode childNode : childSchemaNodes) {
            if (childNode instanceof ListSchemaNode || childNode instanceof ContainerSchemaNode) {
                // keep config and operation attributes separate.
                if (childNode.isConfiguration() == addConfigApi) {
                    final String newParent = parentName + "/" + node.getQName().getLocalName();
                    addApis(childNode, apis, resourcePath, pathParams, schemaContext, addConfigApi, newParent,
                            dataStore);
                }
            }
        }
    }

    protected static String getContent(final String dataStore) {
        if (newDraft) {
            if ("operational".contains(dataStore)) {
                return "?content=nonconfig";
            } else if ("config".contains(dataStore)) {
                return "?content=config";
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private static boolean containsListOrContainer(final Iterable<DataSchemaNode> nodes) {
        for (final DataSchemaNode child : nodes) {
            if (child instanceof ListSchemaNode || child instanceof ContainerSchemaNode) {
                return true;
            }
        }
        return false;
    }

    private static List<Operation> operation(final DataSchemaNode node, final List<Parameter> pathParams,
                                             final boolean isConfig, final Iterable<DataSchemaNode> childSchemaNodes, final String parentName) {
        final List<Operation> operations = new ArrayList<>();

        final Get getBuilder = new Get(node, isConfig);
        operations.add(getBuilder.pathParams(pathParams).build());

        if (isConfig) {
            final Put putBuilder = new Put(node.getQName().getLocalName(), node.getDescription().orElse(null),
                parentName);
            operations.add(putBuilder.pathParams(pathParams).build());

            final Delete deleteBuilder = new Delete(node);
            operations.add(deleteBuilder.pathParams(pathParams).build());

            if (containsListOrContainer(childSchemaNodes)) {
                operations.addAll(operationPost(node.getQName().getLocalName(), node.getDescription().orElse(null),
                        (DataNodeContainer) node, pathParams, isConfig, parentName + "/"));
            }
        }
        return operations;
    }

    private static List<Operation> operationPost(final String name, final String description,
                                                 final DataNodeContainer dataNodeContainer, final List<Parameter> pathParams, final boolean isConfig,
                                                 final String parentName) {
        final List<Operation> operations = new ArrayList<>();
        if (isConfig) {
            final Post postBuilder = new Post(name, parentName + name, description, dataNodeContainer);
            operations.add(postBuilder.pathParams(pathParams).build());
        }
        return operations;
    }

    private static String createPath(final DataSchemaNode schemaNode, final List<Parameter> pathParams,
                                     final SchemaContext schemaContext) {
        final ArrayList<LeafSchemaNode> pathListParams = new ArrayList<>();
        final StringBuilder path = new StringBuilder();
        final String localName = resolvePathArgumentsName(schemaNode, schemaContext);
        path.append(localName);

        if (schemaNode instanceof ListSchemaNode) {
            final List<QName> listKeys = ((ListSchemaNode) schemaNode).getKeyDefinition();
            StringBuilder keyBuilder = null;
            if (newDraft) {
                keyBuilder = new StringBuilder("=");
            }

            for (final QName listKey : listKeys) {
                final DataSchemaNode dataChildByName = ((DataNodeContainer) schemaNode).getDataChildByName(listKey);
                pathListParams.add((LeafSchemaNode) dataChildByName);
                final String pathParamIdentifier;
                if (newDraft) {
                    pathParamIdentifier = keyBuilder.append("{").append(listKey.getLocalName()).append("}").toString();
                } else {
                    pathParamIdentifier = "/{" + listKey.getLocalName() + "}";
                }
                path.append(pathParamIdentifier);

                final Parameter pathParam = new Parameter();
                pathParam.setName(listKey.getLocalName());
                pathParam.setDescription(dataChildByName.getDescription().orElse(null));
                pathParam.setType("string");
                pathParam.setParamType("path");

                pathParams.add(pathParam);
                if (newDraft) {
                    keyBuilder = new StringBuilder(",");
                }
            }
        }
        return path.toString();
    }

    protected void addRpcs(final RpcDefinition rpcDefn, final List<Api> apis, final String parentPath,
                           final SchemaContext schemaContext) {
        final Api rpc = new Api();
        final String resourcePath = parentPath + "/" + resolvePathArgumentsName(rpcDefn, schemaContext);
        rpc.setPath(resourcePath);

        final Operation operationSpec = new Operation();
        operationSpec.setMethod("POST");
        operationSpec.setNotes(rpcDefn.getDescription().orElse(null));
        operationSpec.setNickname(rpcDefn.getQName().getLocalName());
        if (!rpcDefn.getOutput().getChildNodes().isEmpty()) {
            operationSpec.setType("(" + rpcDefn.getQName().getLocalName() + ")output" + OperationBuilder.TOP);
        }
        if (!rpcDefn.getInput().getChildNodes().isEmpty()) {
            final Parameter payload = new Parameter();
            payload.setParamType("body");
            payload.setType("(" + rpcDefn.getQName().getLocalName() + ")input" + OperationBuilder.TOP);
            operationSpec.setParameters(Collections.singletonList(payload));
            operationSpec.setConsumes(OperationBuilder.CONSUMES_PUT_POST);
        }

        rpc.setOperations(Arrays.asList(operationSpec));

        apis.add(rpc);
    }

    protected SortedSet<Module> getSortedModules(final SchemaContext schemaContext) {
        if (schemaContext == null) {
            return new TreeSet<>();
        }

        final Set<Module> modules = schemaContext.getModules();

        final SortedSet<Module> sortedModules = new TreeSet<>((module1, module2) -> {
            int result = module1.getName().compareTo(module2.getName());
            if (result == 0) {
                result = Revision.compare(module1.getRevision(), module2.getRevision());
            }
            if (result == 0) {
                result = module1.getNamespace().compareTo(module2.getNamespace());
            }
            return result;
        });
        for (final Module m : modules) {
            if (m != null) {
                sortedModules.add(m);
            }
        }
        return sortedModules;
    }

    public void setDraft(final boolean draft) {
        BaseYangSwaggerGenerator.newDraft = draft;
    }
}
