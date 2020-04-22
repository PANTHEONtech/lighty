/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;
import org.opendaylight.aaa.shiro.web.env.ThreadLocals;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.HttpAuthorization;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.http.authorization.Policies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.http.permission.Permissions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.http.permission.Permissions.Actions;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a dynamic authorization mechanism for restful web services with permission grain
 * scope.  <code>aaa.yang</code> defines the model for this filtering mechanism.
 * This model exposes the ability to manipulate policy information for specific paths
 * based on a tuple of (role, http_permission_list).
 *
 * <p>This mechanism will only work when put behind <code>authcBasic</code>.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LightyMDSALDynamicAuthorizationFilter extends AuthorizationFilter
        implements ClusteredDataTreeChangeListener<HttpAuthorization> {

    private static final Logger LOG = LoggerFactory.getLogger(LightyMDSALDynamicAuthorizationFilter.class);

    private static final DataTreeIdentifier<HttpAuthorization> AUTHZ_CONTAINER = DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(HttpAuthorization.class));

    private final DataBroker dataBroker;

    private ListenerRegistration<?> reg;
    private volatile ListenableFuture<Optional<HttpAuthorization>> authContainer;

    public LightyMDSALDynamicAuthorizationFilter() {
        dataBroker = requireNonNull(ThreadLocals.DATABROKER_TL.get());
    }

    @Override
    public Filter processPathConfig(String path, String config) {
        try (ReadTransaction tx = dataBroker.newReadOnlyTransaction()) {
            authContainer = tx.read(AUTHZ_CONTAINER.getDatastoreType(), AUTHZ_CONTAINER.getRootIdentifier());
        }
        this.reg = dataBroker.registerDataTreeChangeListener(AUTHZ_CONTAINER, this);
        return super.processPathConfig(path, config);
    }

    @Override
    public void destroy() {
        if (reg != null) {
            reg.close();
            reg = null;
        }
        super.destroy();
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<HttpAuthorization>> changes) {
        final HttpAuthorization newVal = Iterables.getLast(changes).getRootNode().getDataAfter();
        LOG.debug("Updating authorization information to {}", newVal);
        authContainer = Futures.immediateFuture(Optional.ofNullable(newVal));
    }

    @Override
    public boolean isAccessAllowed(final ServletRequest request, final ServletResponse response,
                                   final Object mappedValue) {
        checkArgument(request instanceof HttpServletRequest, "Expected HttpServletRequest, received {}", request);

        final Subject subject = getSubject(request, response);
        final HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        final String requestURI = httpServletRequest.getRequestURI();
        LOG.debug("isAccessAllowed for user={} to requestURI={}", subject, requestURI);

        final Optional<HttpAuthorization> authorizationOptional;
        try {
            authorizationOptional = authContainer.get();
        } catch (ExecutionException | InterruptedException e) {
            // Something went completely wrong trying to read the authz container.  Deny access.
            LOG.warn("MDSAL attempt to read Http Authz Container failed, disallowing access", e);
            return false;
        }

        if (!authorizationOptional.isPresent()) {
            // The authorization container does not exist-- hence no authz rules are present
            // Allow access.
            LOG.debug("Authorization Container does not exist");
            return true;
        }

        final HttpAuthorization httpAuthorization = authorizationOptional.get();
        final Policies policies = httpAuthorization.getPolicies();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.http.authorization
                .policies.Policies>
                policiesList = policies.getPolicies();
        if (policiesList.isEmpty()) {
            // The authorization container exists, but no rules are present.  Allow access.
            LOG.debug("Exiting successfully early since no authorization rules exist");
            return true;
        }

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.aaa.rev161214.http.authorization
                .policies.Policies policy : policiesList) {
            final String resource = policy.getResource();
            final boolean pathsMatch = pathsMatch(resource, requestURI);
            if (pathsMatch) {
                LOG.debug("paths match for pattern={} and requestURI={}", resource, requestURI);
                final String method = httpServletRequest.getMethod();
                LOG.trace("method={}", method);
                final List<Permissions> permissions = policy.getPermissions();
                for (Permissions permission : permissions) {
                    final String role = permission.getRole();
                    LOG.trace("role={}", role);
                    final List<Actions> actions = permission.getActions();
                    for (Permissions.Actions action : actions) {
                        LOG.trace("action={}", action.getName());
                        if (action.getName().equalsIgnoreCase(method)) {
                            final boolean hasRole = subject.hasRole(role);
                            LOG.trace("hasRole({})={}", role, hasRole);
                            if (hasRole) {
                                return true;
                            }
                        }
                    }
                }
                LOG.debug("couldn't authorize the user for access");
                return false;
            }
        }
        LOG.debug("successfully authorized the user for access");
        return true;
    }
}
