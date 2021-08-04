/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.util;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import gnmi.Gnmi.Encoding;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.Path;
import gnmi.Gnmi.Path.Builder;
import gnmi.Gnmi.PathElem;
import gnmi.Gnmi.SetRequest;
import gnmi.Gnmi.TypedValue;
import gnmi.Gnmi.Update;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class GnmiHelper {

    public static final String REGEX_SLASH = "/";
    public static final String REGEX_SEMICOLON = ";";
    public static final Pattern KEY_VAL_PAIR = Pattern.compile("(?<key>(\\w.+))=(?<val>(\\w.+))");
    public static final String REGEX_BEGIN_KEY = "[";
    public static final String REGEX_END_KEY = "]";

    private GnmiHelper() {
        throw new UnsupportedOperationException("Instances of this class should not be created!");
    }

    public static GetRequest buildGetRequest(final String gnmiPath) {

        GetRequest request = null;
        Builder builder = pathBuilder(gnmiPath);
        if (builder != null) {
            request = GetRequest.newBuilder().setEncoding(Encoding.JSON_IETF).addPath(builder.build()).build();
        }
        return request;
    }

    public static SetRequest buildSetRequest(final String gnmiPath, final String val, final Type type) {
        final Path path = pathBuilder(gnmiPath).build();
        switch (type) {
            case REPLACE:
                return getReplaceSetRequest(val, path);
            case DELETE:
                return getDeleteSetRequest(path);
            case UPDATE:
            default:
                return getUpdateSetRequest(val, path);
        }
    }

    private static SetRequest getUpdateSetRequest(final String val, final Path path) {
        return SetRequest.newBuilder()
                .addUpdate(getBuilderForValue(val, path))
                .build();
    }

    private static SetRequest getReplaceSetRequest(final String val, final Path path) {
        return SetRequest.newBuilder()
                .addReplace(getBuilderForValue(val, path))
                .build();
    }

    private static SetRequest getDeleteSetRequest(final Path path) {
        return SetRequest.newBuilder()
                .addDelete(path)
                .build();
    }

    private static Update.Builder getBuilderForValue(final String val, final Path path) {
        return Update.newBuilder()
                .setPath(path)
                .setVal(TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(val))
                        .build());
    }

    public static Builder pathBuilder(final String xpath) {
        final Builder pathBuilder = Path.newBuilder();
        if (Strings.isNullOrEmpty(xpath)) {
            return pathBuilder;
        }
        String[] tokens = xpath.split(REGEX_SLASH);
        Arrays.stream(tokens)
            .filter(token -> !StringUtils.isBlank(token))
            .forEach(token -> processToken(token, pathBuilder));
        return pathBuilder;
    }

    private static void processToken(final String token, final Builder pathBuilder) {
        String elem;
        Map<String, String> keys = new HashMap<>();
        final int beginKey = token.indexOf(REGEX_BEGIN_KEY);
        final int endKey = token.indexOf(REGEX_END_KEY);
        if (beginKey > 0) {
            elem = token.substring(0, beginKey);
            String keyPeers = token.substring(beginKey + 1, endKey);
            for (String keyValPair : keyPeers.split(REGEX_SEMICOLON)) {
                final Matcher matcher = KEY_VAL_PAIR.matcher(keyValPair);
                if (matcher.matches()) {
                    keys.put(matcher.group("key"), matcher.group("val"));
                }
            }
        } else {
            elem = token;
        }
        gnmi.Gnmi.PathElem.Builder elemBuilder = PathElem.newBuilder().setName(elem);
        if (!keys.isEmpty()) {
            elemBuilder.putAllKey(keys);
        }
        pathBuilder.addElem(elemBuilder.build());
    }

    public enum Type { UPDATE, REPLACE, DELETE }
}