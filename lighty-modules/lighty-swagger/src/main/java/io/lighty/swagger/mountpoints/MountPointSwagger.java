/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.swagger.mountpoints;

import com.google.common.base.Optional;
import io.lighty.swagger.impl.BaseYangSwaggerGenerator;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.netconf.sal.rest.doc.swagger.Api;
import org.opendaylight.netconf.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.netconf.sal.rest.doc.swagger.Operation;
import org.opendaylight.netconf.sal.rest.doc.swagger.Resource;
import org.opendaylight.netconf.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MountPointSwagger extends BaseYangSwaggerGenerator implements MountProvisionListener {

    private static final String DATASTORES_REVISION = "-";
    private static final String DATASTORES_LABEL = "Datastores";
    private static final String RESTCONF_DRAFT = "18";
    private static final AtomicReference<MountPointSwagger> SELF_REF = new AtomicReference<>();

    private DOMMountPointService mountService;
    private final Map<YangInstanceIdentifier, Long> instanceIdToLongId =
            new TreeMap<>((o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
    private final Map<Long, YangInstanceIdentifier> longIdToInstanceId = new HashMap<>();

    private final Object lock = new Object();

    private final AtomicLong idKey = new AtomicLong(0);
    private SchemaService globalSchema;
    private static boolean newDraft;

    public Map<String, Long> getInstanceIdentifiers() {
        final Map<String, Long> urlToId = new HashMap<>();
        synchronized (this.lock) {
            final SchemaContext context = this.globalSchema.getGlobalContext();
            for (final Entry<YangInstanceIdentifier, Long> entry : this.instanceIdToLongId.entrySet()) {
                final String modName = findModuleName(entry.getKey(), context);
                urlToId.put(generateUrlPrefixFromInstanceID(entry.getKey(), modName), entry.getValue());
            }
        }
        return urlToId;
    }

    public void setGlobalSchema(final SchemaService globalSchema) {
        this.globalSchema = globalSchema;
    }

    private String findModuleName(final YangInstanceIdentifier id, final SchemaContext context) {
        final PathArgument rootQName = id.getPathArguments().iterator().next();
        for (final Module mod : context.getModules()) {
            if (mod.getDataChildByName(rootQName.getNodeType()) != null) {
                return mod.getName();
            }
        }
        return null;
    }

    private String generateUrlPrefixFromInstanceID(final YangInstanceIdentifier key, final String moduleName) {
        final StringBuilder builder = new StringBuilder();
        builder.append("/");
        if (moduleName != null) {
            builder.append(moduleName).append(':');
        }
        for (final PathArgument arg : key.getPathArguments()) {
            final String name = arg.getNodeType().getLocalName();
            if (arg instanceof NodeIdentifierWithPredicates) {
                final NodeIdentifierWithPredicates nodeId = (NodeIdentifierWithPredicates) arg;
                for (final Entry<QName, Object> entry : nodeId.getKeyValues().entrySet()) {
                    if (newDraft) {
                        builder.deleteCharAt(builder.length() - 1).append("=").append(entry.getValue()).append('/');
                    } else {
                        builder.append(entry.getValue()).append('/');
                    }
                }
            } else {
                builder.append(name).append('/');
            }
        }
        return builder.toString();
    }

    private String getYangMountUrl(final YangInstanceIdentifier key) {
        final String modName = findModuleName(key, this.globalSchema.getGlobalContext());
        return generateUrlPrefixFromInstanceID(key, modName) + "yang-ext:mount";
    }

    public ResourceList getResourceList(final UriInfo uriInfo, final Long id) {
        final YangInstanceIdentifier iid = getInstanceId(id);
        if (iid == null) {
            return null; // indicating not found.
        }
        final SchemaContext context = getSchemaContext(iid);
        if (context == null) {
            return createResourceList();
        }
        final List<Resource> resources = new LinkedList<>();
        final Resource dataStores = new Resource();
        dataStores.setDescription("Provides methods for accessing the data stores.");
        dataStores.setPath(generatePath(uriInfo, DATASTORES_LABEL, DATASTORES_REVISION));
        resources.add(dataStores);
        final String urlPrefix = getYangMountUrl(iid);
        final ResourceList list = super.getResourceListing(uriInfo, context, urlPrefix);
        resources.addAll(list.getApis());
        list.setApis(resources);
        return list;
    }

    private YangInstanceIdentifier getInstanceId(final Long id) {
        final YangInstanceIdentifier instanceId;
        synchronized (this.lock) {
            instanceId = this.longIdToInstanceId.get(id);
        }
        return instanceId;
    }

    private SchemaContext getSchemaContext(final YangInstanceIdentifier id) {

        if (id == null) {
            return null;
        }

        final Optional<DOMMountPoint> mountPoint = this.mountService.getMountPoint(id);
        if (!mountPoint.isPresent()) {
            return null;
        }

        final SchemaContext context = mountPoint.get().getSchemaContext();
        if (context == null) {
            return null;
        }
        return context;
    }

    public ApiDeclaration getMountPointApi(final UriInfo uriInfo, final Long id, final String module,
                                           final String revision) {
        final YangInstanceIdentifier iid = getInstanceId(id);
        final SchemaContext context = getSchemaContext(iid);
        final String urlPrefix = getYangMountUrl(iid);
        if (context == null) {
            return null;
        }

        if (DATASTORES_LABEL.equals(module) && DATASTORES_REVISION.equals(revision)) {
            return generateDataStoreApiDoc(uriInfo, urlPrefix);
        }
        return super.getApiDeclaration(module, revision, uriInfo, context, urlPrefix);
    }

    private ApiDeclaration generateDataStoreApiDoc(final UriInfo uriInfo, final String context) {
        final List<Api> apis = new LinkedList<>();
        apis.add(createGetApi("config", "Queries the config (startup) datastore on the mounted hosted.", context));
        apis.add(createGetApi("operational", "Queries the operational (running) datastore on the mounted hosted.",
                context));
        apis.add(createGetApi("operations", "Queries the available operations (RPC calls) on the mounted hosted.",
                context));

        final ApiDeclaration declaration = super.createApiDeclaration(createBasePathFromUriInfo(uriInfo));
        declaration.setApis(apis);
        return declaration;

    }

    private Api createGetApi(final String datastore, final String note, final String context) {
        final Operation getConfig = new Operation();
        getConfig.setMethod("GET");
        getConfig.setNickname("GET " + datastore);
        getConfig.setNotes(note);

        final Api api = new Api();
        api.setPath(getDataStorePath(datastore, context).concat(getContent(datastore)));
        api.setOperations(Collections.singletonList(getConfig));

        return api;
    }

    public void setMountService(final DOMMountPointService mountService) {
        this.mountService = mountService;
    }

    @Override
    public void onMountPointCreated(final YangInstanceIdentifier path) {
        synchronized (this.lock) {
            final Long idLong = this.idKey.incrementAndGet();
            this.instanceIdToLongId.put(path, idLong);
            this.longIdToInstanceId.put(idLong, path);
        }
    }

    @Override
    public void onMountPointRemoved(final YangInstanceIdentifier path) {
        synchronized (this.lock) {
            final Long id = this.instanceIdToLongId.remove(path);
            this.longIdToInstanceId.remove(id);
        }
    }

    public static MountPointSwagger getInstance() {
        MountPointSwagger swagger = SELF_REF.get();
        if (swagger == null) {
            SELF_REF.compareAndSet(null, new MountPointSwagger());
            swagger = SELF_REF.get();
        }
        newDraft = false;
        return swagger;
    }

    public static MountPointSwagger getInstanceDraft18() {
        MountPointSwagger swagger = SELF_REF.get();
        if (swagger == null) {
            SELF_REF.compareAndSet(null, new MountPointSwagger());
            swagger = SELF_REF.get();
        }
        newDraft = true;
        return swagger;
    }
}
