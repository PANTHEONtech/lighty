/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.modules.northbound.restconf.community.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.InetAddress;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

public class RestConfConfiguration {

    @JsonIgnore
    private DOMDataBroker domDataBroker;
    @JsonIgnore
    private DOMSchemaService schemaService;
    @JsonIgnore
    private DOMRpcService domRpcService;
    @JsonIgnore
    private DOMNotificationService domNotificationService;
    @JsonIgnore
    private DOMMountPointService domMountPointService;
    @JsonIgnore
    private DOMSchemaService domSchemaService;

    private InetAddress inetAddress = InetAddress.getLoopbackAddress();
    private int webSocketPort = 8185;
    private JsonRestConfServiceType jsonRestconfServiceType = JsonRestConfServiceType.DRAFT_18;
    private int httpPort = 8888;
    private String restconfServletContextPath = "/restconf";

    public RestConfConfiguration() {
    }

    public RestConfConfiguration(final RestConfConfiguration restConfConfiguration) {
        this.inetAddress = restConfConfiguration.getInetAddress();
        this.webSocketPort = restConfConfiguration.getWebSocketPort();
        this.httpPort = restConfConfiguration.getHttpPort();
        this.restconfServletContextPath = restConfConfiguration.getRestconfServletContextPath();
        this.domDataBroker = restConfConfiguration.getDomDataBroker();
        this.schemaService = restConfConfiguration.getSchemaService();
        this.domRpcService = restConfConfiguration.getDomRpcService();
        this.domNotificationService = restConfConfiguration.getDomNotificationService();
        this.domMountPointService = restConfConfiguration.getDomMountPointService();
        this.domSchemaService = restConfConfiguration.getDomSchemaService();
        this.jsonRestconfServiceType = restConfConfiguration.getJsonRestconfServiceType();
    }

    public RestConfConfiguration(final DOMDataBroker domDataBroker, final DOMSchemaService schemaService,
            final DOMRpcService domRpcService, final DOMNotificationService domNotificationService,
            final DOMMountPointService domMountPointService, final DOMSchemaService domSchemaService) {
        this.domDataBroker = domDataBroker;
        this.schemaService = schemaService;
        this.domRpcService = domRpcService;
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

    public int getWebSocketPort() {
        return this.webSocketPort;
    }

    public void setWebSocketPort(final int webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    public JsonRestConfServiceType getJsonRestconfServiceType() {
        return this.jsonRestconfServiceType;
    }

    public void setJsonRestconfServiceType(final JsonRestConfServiceType jsonRestconfServiceType) {
        this.jsonRestconfServiceType = jsonRestconfServiceType;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public void setHttpPort(final int httpPort) {
        this.httpPort = httpPort;
    }

    public String getRestconfServletContextPath() {
        return this.restconfServletContextPath;
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final RestConfConfiguration that = (RestConfConfiguration) o;

        if (this.webSocketPort != that.webSocketPort) {
            return false;
        }
        if (this.httpPort != that.httpPort) {
            return false;
        }
        if (!this.domDataBroker.equals(that.domDataBroker)) {
            return false;
        }
        if (!this.schemaService.equals(that.schemaService)) {
            return false;
        }
        if (!this.domRpcService.equals(that.domRpcService)) {
            return false;
        }
        if (!this.domNotificationService.equals(that.domNotificationService)) {
            return false;
        }
        if (!this.domMountPointService.equals(that.domMountPointService)) {
            return false;
        }
        if (this.jsonRestconfServiceType != that.jsonRestconfServiceType) {
            return false;
        }
        if (this.domSchemaService != that.domSchemaService) {
            return false;
        }
        return this.restconfServletContextPath.equals(that.restconfServletContextPath);
    }

    @Override
    public int hashCode() {
        int result = this.domDataBroker.hashCode();
        result = (31 * result) + this.schemaService.hashCode();
        result = (31 * result) + this.domRpcService.hashCode();
        result = (31 * result) + this.domNotificationService.hashCode();
        result = (31 * result) + this.domMountPointService.hashCode();
        result = (31 * result) + this.webSocketPort;
        result = (31 * result) + this.jsonRestconfServiceType.hashCode();
        result = (31 * result) + this.httpPort;
        result = (31 * result) + this.restconfServletContextPath.hashCode();
        result = (31 * result) + this.domSchemaService.hashCode();
        return result;
    }
}
