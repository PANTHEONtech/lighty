/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.tests;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class TestUtils {

    private TestUtils() {

    }

    static String readResource(final String classPath) throws IOException {
        String result;
        try (InputStream inputStream = TestUtils.class.getResourceAsStream(classPath)) {
            result = CharStreams.toString(new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8));
        }
        return result;
    }

}
