/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.northbound.restconf.community.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.InetAddress;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

public class RestConfConfiguration {

    @JsonIgnore
    private DOMDataBroker domDataBroker;
    @JsonIgnore
    private SchemaService schemaService;
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

    public RestConfConfiguration(RestConfConfiguration restConfConfiguration) {
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

    public RestConfConfiguration(DOMDataBroker domDataBroker, SchemaService schemaService, DOMRpcService domRpcService,
                                 DOMNotificationService domNotificationService, DOMMountPointService domMountPointService,
                                 DOMSchemaService domSchemaService) {
        this.domDataBroker = domDataBroker;
        this.schemaService = schemaService;
        this.domRpcService = domRpcService;
        this.domNotificationService = domNotificationService;
        this.domMountPointService = domMountPointService;
        this.domSchemaService = domSchemaService;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public DOMDataBroker getDomDataBroker() {
        return domDataBroker;
    }

    public void setDomDataBroker(DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    public SchemaService getSchemaService() {
        return schemaService;
    }

    public void setSchemaService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public DOMRpcService getDomRpcService() {
        return domRpcService;
    }

    public void setDomRpcService(DOMRpcService domRpcService) {
        this.domRpcService = domRpcService;
    }

    public DOMNotificationService getDomNotificationService() {
        return domNotificationService;
    }

    public void setDomNotificationService(DOMNotificationService domNotificationService) {
        this.domNotificationService = domNotificationService;
    }

    public DOMMountPointService getDomMountPointService() {
        return domMountPointService;
    }

    public void setDomMountPointService(DOMMountPointService domMountPointService) {
        this.domMountPointService = domMountPointService;
    }

    public int getWebSocketPort() {
        return webSocketPort;
    }

    public void setWebSocketPort(int webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    public JsonRestConfServiceType getJsonRestconfServiceType() {
        return jsonRestconfServiceType;
    }

    public void setJsonRestconfServiceType(JsonRestConfServiceType jsonRestconfServiceType) {
        this.jsonRestconfServiceType = jsonRestconfServiceType;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getRestconfServletContextPath() {
        return restconfServletContextPath;
    }

    public void setRestconfServletContextPath(String restconfServletContextPath) {
        this.restconfServletContextPath = restconfServletContextPath;
    }

    public DOMSchemaService getDomSchemaService() {
        return domSchemaService;
    }

    public void setDomSchemaService(DOMSchemaService domSchemaService) {
        this.domSchemaService = domSchemaService;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RestConfConfiguration that = (RestConfConfiguration) o;

        if (webSocketPort != that.webSocketPort)
            return false;
        if (httpPort != that.httpPort)
            return false;
        if (!domDataBroker.equals(that.domDataBroker))
            return false;
        if (!schemaService.equals(that.schemaService))
            return false;
        if (!domRpcService.equals(that.domRpcService))
            return false;
        if (!domNotificationService.equals(that.domNotificationService))
            return false;
        if (!domMountPointService.equals(that.domMountPointService))
            return false;
        if (jsonRestconfServiceType != that.jsonRestconfServiceType)
            return false;
        if (domSchemaService != that.domSchemaService) {
            return false;
        }
        return restconfServletContextPath.equals(that.restconfServletContextPath);
    }

    @Override
    public int hashCode() {
        int result = domDataBroker.hashCode();
        result = 31 * result + schemaService.hashCode();
        result = 31 * result + domRpcService.hashCode();
        result = 31 * result + domNotificationService.hashCode();
        result = 31 * result + domMountPointService.hashCode();
        result = 31 * result + webSocketPort;
        result = 31 * result + jsonRestconfServiceType.hashCode();
        result = 31 * result + httpPort;
        result = 31 * result + restconfServletContextPath.hashCode();
        result = 31 * result + domSchemaService.hashCode();
        return result;
    }
}
