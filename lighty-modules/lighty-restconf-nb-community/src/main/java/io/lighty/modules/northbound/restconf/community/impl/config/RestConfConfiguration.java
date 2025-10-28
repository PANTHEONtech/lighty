/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.InetAddress;
import java.util.Objects;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

public class RestConfConfiguration {

    @JsonIgnore
    private DOMDataBroker domDataBroker;
    @JsonIgnore
    private DOMSchemaService schemaService;
    @JsonIgnore
    private DOMRpcService domRpcService;
    @JsonIgnore
    private DOMActionService domActionService;
    @JsonIgnore
    private DOMNotificationService domNotificationService;
    @JsonIgnore
    private DOMMountPointService domMountPointService;
    @JsonIgnore
    private DOMSchemaService domSchemaService;

    private InetAddress inetAddress = InetAddress.getLoopbackAddress();
    private int httpPort = 8888;
    private String restconfServletContextPath = "/restconf";

    public RestConfConfiguration() {
    }

    public RestConfConfiguration(final RestConfConfiguration restConfConfiguration) {
        this.inetAddress = restConfConfiguration.getInetAddress();
        this.httpPort = restConfConfiguration.getHttpPort();
        this.restconfServletContextPath = restConfConfiguration.getRestconfServletContextPath();
        this.domDataBroker = restConfConfiguration.getDomDataBroker();
        this.schemaService = restConfConfiguration.getSchemaService();
        this.domRpcService = restConfConfiguration.getDomRpcService();
        this.domActionService = restConfConfiguration.getDomActionService();
        this.domNotificationService = restConfConfiguration.getDomNotificationService();
        this.domMountPointService = restConfConfiguration.getDomMountPointService();
        this.domSchemaService = restConfConfiguration.getDomSchemaService();
    }

    public RestConfConfiguration(final DOMDataBroker domDataBroker, final DOMSchemaService schemaService,
            final DOMRpcService domRpcService, final DOMActionService domActionService,
            final DOMNotificationService domNotificationService, final DOMMountPointService domMountPointService,
            final DOMSchemaService domSchemaService) {
        this.domDataBroker = domDataBroker;
        this.schemaService = schemaService;
        this.domRpcService = domRpcService;
        this.domActionService = domActionService;
        this.domNotificationService = domNotificationService;
        this.domMountPointService = domMountPointService;
        this.domSchemaService = domSchemaService;
    }

    public InetAddress getInetAddress() {
        return this.inetAddress;
    }

    public void setInetAddress(final InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public DOMDataBroker getDomDataBroker() {
        return this.domDataBroker;
    }

    public void setDomDataBroker(final DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    public DOMSchemaService getSchemaService() {
        return this.schemaService;
    }

    public void setSchemaService(final DOMSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public DOMRpcService getDomRpcService() {
        return this.domRpcService;
    }

    public void setDomRpcService(final DOMRpcService domRpcService) {
        this.domRpcService = domRpcService;
    }

    public DOMActionService getDomActionService() {
        return this.domActionService;
    }

    public void setDomActionService(final DOMActionService domActionService) {
        this.domActionService = domActionService;
    }

    public DOMNotificationService getDomNotificationService() {
        return this.domNotificationService;
    }

    public void setDomNotificationService(final DOMNotificationService domNotificationService) {
        this.domNotificationService = domNotificationService;
    }

    public DOMMountPointService getDomMountPointService() {
        return this.domMountPointService;
    }

    public void setDomMountPointService(final DOMMountPointService domMountPointService) {
        this.domMountPointService = domMountPointService;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public void setHttpPort(final int httpPort) {
        this.httpPort = httpPort;
    }

    public String getRestconfServletContextPath() {
        return restconfServletContextPath.startsWith("/")
            ? this.restconfServletContextPath.substring(1) : this.restconfServletContextPath;
    }

    public void setRestconfServletContextPath(final String restconfServletContextPath) {
        this.restconfServletContextPath = restconfServletContextPath;
    }

    public DOMSchemaService getDomSchemaService() {
        return this.domSchemaService;
    }

    public void setDomSchemaService(final DOMSchemaService domSchemaService) {
        this.domSchemaService = domSchemaService;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RestConfConfiguration that = (RestConfConfiguration) obj;
        return httpPort == that.httpPort
                && Objects.equals(domDataBroker, that.domDataBroker)
                && Objects.equals(schemaService, that.schemaService)
                && Objects.equals(domRpcService, that.domRpcService)
                && Objects.equals(domNotificationService, that.domNotificationService)
                && Objects.equals(domMountPointService, that.domMountPointService)
                && Objects.equals(domSchemaService, that.domSchemaService)
                && Objects.equals(inetAddress, that.inetAddress)
                && Objects.equals(restconfServletContextPath, that.restconfServletContextPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domDataBroker, schemaService, domRpcService, domNotificationService,
                domMountPointService, domSchemaService, inetAddress, httpPort, restconfServletContextPath);
    }

}
