/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.commons.util;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for sanitizing YANG model body.
 */
public final class SanitizeYangModel {

    private SanitizeYangModel() {
        // Utility class
    }

    /*
     *  pattern '^REGEX$';
     *  oc-ext:posix-pattern '^REGEX$';
     */
    private static final Pattern YANG_REGEX_PATTERN = Pattern.compile("(.*|\n*)pattern '\\^(?<regex>.+)\\$'(.*|\n*)");

    /**
     * Sanitize regex posix ['^','$'] from YANG model.
     * E.g.: from '^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){7}$' to: '[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){7}'
     * This method is workaround to YANGTOOLS issue: https://jira.opendaylight.org/browse/YANGTOOLS-1005
     * TODO: Remove when https://jira.opendaylight.org/browse/YANGTOOLS-1005 issue will be resolved.
     *
     * @param data {@link ByteSource} containing yang model body.
     * @return Sanitized regex posix inside YANG model regex patterns.
     * @throws IOException Throw when is execute {@link ByteSource#read()}
     */
    public static ByteSource removeRegexpPosix(final ByteSource data) throws IOException {
        final String textModel = new String(data.read(), StandardCharsets.UTF_8);
        final String sanitizedModel = removeRegexpPosix(textModel);
        return ByteSource.wrap(sanitizedModel.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sanitize regex posix ['^','$'] from YANG model.
     * E.g.: from: '^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){7}$' to: '[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){7}'
     * This method is workaround to YANGTOOLS issue: https://jira.opendaylight.org/browse/YANGTOOLS-1005
     * TODO: Remove when https://jira.opendaylight.org/browse/YANGTOOLS-1005 issue will be resolved.
     *
     * @param yangModel {@link String} yang model body.
     * @return Sanitized regex posix inside YANG model regex patterns.
     */
    public static String removeRegexpPosix(final String yangModel) {
        String result = yangModel;
        final Matcher matcher = YANG_REGEX_PATTERN.matcher(yangModel);
        while (matcher.find()) {
            String regex = matcher.group("regex");
            result = result.replace("^" + regex + "$", regex);
        }
        return result;
    }
}
