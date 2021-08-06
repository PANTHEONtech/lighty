/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.request;

import com.google.protobuf.ByteString;
import gnmi.Gnmi;

public final class RequestBuilder {

    private RequestBuilder() {

    }

    public static Gnmi.GetRequest buildGetRequest(final Gnmi.Path path) {
        return Gnmi.GetRequest.newBuilder().setEncoding(Gnmi.Encoding.JSON_IETF).addPath(path).build();
    }

    public static Gnmi.SetRequest buildSetRequest(final Gnmi.Path path, final String val, final Type type) {
        switch (type) {
            case REPLACE:
                return buildReplaceSetRequest(val, path);
            case DELETE:
                return buildDeleteSetRequest(path);
            case UPDATE:
            default:
                return buildUpdateSetRequest(val, path);
        }
    }

    private static Gnmi.SetRequest buildUpdateSetRequest(final String val, final Gnmi.Path path) {
        return Gnmi.SetRequest.newBuilder()
                .addUpdate(getBuilderForValue(val, path))
                .build();
    }

    private static Gnmi.SetRequest buildReplaceSetRequest(final String val, final Gnmi.Path path) {
        return Gnmi.SetRequest.newBuilder()
                .addReplace(getBuilderForValue(val, path))
                .build();
    }

    private static Gnmi.SetRequest buildDeleteSetRequest(final Gnmi.Path path) {
        return Gnmi.SetRequest.newBuilder()
                .addDelete(path)
                .build();
    }

    private static Gnmi.Update.Builder getBuilderForValue(final String val, final Gnmi.Path path) {
        return Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(Gnmi.TypedValue.newBuilder()
                        .setJsonIetfVal(ByteString.copyFromUtf8(val))
                        .build());
    }

    public enum Type { UPDATE, REPLACE, DELETE }
}
