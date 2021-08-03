/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfig.Store;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.DatastoreConfigBuilder;

public final class DatastoreConfigurationConfig {
    private DatastoreConfigurationConfig() {

    }

    public static DatastoreConfig getDefault() {
        return new DatastoreConfigBuilder().setStore(Store.H2DataStore).setTimeToLive(new BigInteger("36000"))
                .setTimeToWait(new BigInteger("3600")).build();
    }
}

