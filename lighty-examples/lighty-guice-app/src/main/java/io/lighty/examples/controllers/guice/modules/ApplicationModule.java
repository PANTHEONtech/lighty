/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.guice.modules;

import com.google.inject.AbstractModule;
import io.lighty.examples.controllers.guice.service.DataStoreService;
import io.lighty.examples.controllers.guice.service.DataStoreServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module providing Application bindings.
 */
public class ApplicationModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationModule.class);

    public ApplicationModule() {
        LOG.info("ApplicationModule init ...");
    }

    @Override
    protected void configure() {
        LOG.info("initializing Application bindings ...");
        bind(DataStoreService.class)
                .to(DataStoreServiceImpl.class);
        LOG.info("Application bindings initialized.");
    }

}
