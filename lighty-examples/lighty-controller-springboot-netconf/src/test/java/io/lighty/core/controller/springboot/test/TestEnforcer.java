/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lighty.core.controller.springboot.config.EnforcerProducer;
import java.io.IOException;
import java.util.stream.Stream;
import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestEnforcer {

    public static Stream<Arguments> data() throws IOException {
        final EnforcerProducer producer2 =
                new EnforcerProducer("/data/security/authz_model.conf", "/data/security/authz_policy.csv");
        return Stream.of(
                Arguments.of( "alice", "/services/data/netconf/list", "GET", true, producer2),
                Arguments.of( "bob", "/services/data/netconf/list", "GET", true, producer2),
                Arguments.of( "bob", "/services/data/netconf/id/xxx", "PUT", true, producer2),
                Arguments.of( "alice", "/services/data/netconf/id/xxx", "PUT", false, producer2),
                Arguments.of( "bob", "/services/data/netconf/id/xxx", "DELETE", true, producer2),
                Arguments.of( "alice", "/services/data/netconf/id/xxx", "DELETE", false, producer2),

                Arguments.of( "alice", "/services/data/topology/list", "GET", true, producer2),
                Arguments.of( "bob", "/services/data/topology/list", "GET", true, producer2),
                Arguments.of( "bob", "/services/data/topology/id/xxx", "PUT", true, producer2),
                Arguments.of( "alice", "/services/data/topology/id/xxx", "PUT", false, producer2),
                Arguments.of( "bob", "/services/data/topology/id/xxx", "DELETE", true, producer2),
                Arguments.of( "alice", "/services/data/topology/id/xxx", "DELETE", false, producer2)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testEnforcer(final String user, final String path, final String method, final boolean expectedResult,
            final EnforcerProducer producer) {
        final Enforcer enforcer = producer.getEnforcer();
        assertEquals(expectedResult, enforcer.enforce(user, path, method));
    }

}
