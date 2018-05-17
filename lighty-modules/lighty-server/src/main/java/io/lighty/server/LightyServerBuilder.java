/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Allows user to build jetty server.
 *
 */
public class LightyServerBuilder {

    private final InetSocketAddress inetSocketAddress;
    private final Map<FilterHolder, String> filters;
    private final Map<String, String> parameters;
    private final List<Handler> contexts;
    private final List<EventListener> listeners;
    private Server server;

    /**
     * Init new jetty server on specifc port and address wrapped into {@link InetSocketAddress}
     * @param inetSocketAddress - port and address of server
     */
    public LightyServerBuilder(final InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
        this.filters = new HashMap<>();
        this.parameters = new HashMap<>();
        this.listeners = new ArrayList<>();
        this.contexts = new ArrayList<>();
    }

    /**
     * Init jetty server with existing ones.
     *
     * @param server - jetty servevr
     */
    public LightyServerBuilder(final Server server) {
        this(new InetSocketAddress(0));
        this.server = server;
    }

    /**
     * Add filter for handlers.
     *
     * @param filterHolder - filter holder
     * @param path - path
     * @return instance of {@link LightyServerBuilder}
     */
    public LightyServerBuilder addCommonFilter(final FilterHolder filterHolder, final String path) {
        this.filters.put(filterHolder, path);
        return this;
    }

    /**
     * Add listener for handlers.
     *
     * @param eventListener - event listener
     * @return instance of {@link LightyServerBuilder}
     */
    public LightyServerBuilder addCommonEventListener(final EventListener eventListener) {
        this.listeners.add(eventListener);
        return this;
    }

    /**
     * Add init parameters for handlers.
     *
     * @param key - key of init parameters
     * @param value - value of init parameters
     * @return instance of {@link LightyServerBuilder}
     */
    public LightyServerBuilder addCommonInitParameter(final String key, final String value) {
        this.parameters.put(key, value);
        return this;
    }

    /**
     * Add specific handler for server to handle incoming HTTP requests.
     *
     * @param handler - specific handler
     * @return instance of {@link LightyServerBuilder}
     */
    public LightyServerBuilder addContextHandler(final Handler handler) {
        this.contexts.add(handler);
        return this;
    }

    /**
     * Build jetty server with specific settings (filters, init params, event listeners, handlers).
     *
     * @return instance of jetty servevr
     */
    public Server build() {
        if (this.server == null) {
            this.server = new Server(this.inetSocketAddress);
        }
        final ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        this.contexts.forEach((contextHandler) -> {
            if (true) {
                addFilters(contextHandler);
            }
            contextHandlerCollection.addHandler(contextHandler);
        });
        this.server.setHandler(contextHandlerCollection);
        return this.server;
    }

    private void addFilters(final Handler contextHandler) {
        if (contextHandler instanceof ContextHandlerCollection) {
            final ContextHandlerCollection sch = (ContextHandlerCollection) contextHandler;
            for (final Handler handler : sch.getChildHandlers()) {
                if (handler instanceof ServletContextHandler) {
                    additionalComponents(handler);
                }
            }
        } else if (contextHandler instanceof ServletContextHandler) {
            additionalComponents(contextHandler);
        }
    }

    private void additionalComponents(final Handler contextHandler) {
        final ServletContextHandler ch = (ServletContextHandler) contextHandler;
        this.filters.forEach((filterHolder, path) -> {
            ch.addFilter(filterHolder, path, EnumSet.of(DispatcherType.REQUEST));
        });
        EventListener[] array = new EventListener[this.listeners.size()];
        array = this.listeners.toArray(array);
        ch.setEventListeners(array);
        this.parameters.forEach((key, value) -> {
            ch.setInitParameter(key, value);
        });
    }
}