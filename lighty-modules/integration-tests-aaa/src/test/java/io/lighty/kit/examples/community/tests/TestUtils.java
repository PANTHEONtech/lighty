/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.kit.examples.community.tests;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

final class TestUtils {

    static String readResource(final String classPath) throws IOException {
        String result;
        try (InputStream inputStream = TestUtils.class.getResourceAsStream(classPath)) {
            result = CharStreams.toString(new InputStreamReader(
                    inputStream, Charsets.UTF_8));
        }
        return result;
    }

}
