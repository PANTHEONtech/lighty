/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.commons.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class JsonUtils {

    private JsonUtils() {
        // Utility class
    }

    public static String jsonFromSimpleType(String key, Object value) {
        return "{\"" + key + "\":" + value + "}";
    }

    public static String wrapJsonWithArray(final String jsonString, final String wrapper, final Gson gson) {
        final JsonObject innerJson = new JsonParser().parse(jsonString).getAsJsonObject();
        final JsonObject result = new JsonObject();
        final JsonArray array = new JsonArray();
        array.add(innerJson);
        result.add(wrapper, array);
        return gson.toJson(result);
    }

    public static String wrapJsonWithObject(final String jsonString, final String wrapper, final Gson gson) {
        final JsonElement innerJson = new JsonParser().parse(jsonString);
        final JsonObject result = new JsonObject();
        result.add(wrapper, innerJson);
        return gson.toJson(result);
    }

    public static String addModuleNamePrefixToJson(final String jsonString, final String moduleName, final Gson gson) {
        final JsonObject outerJson = new JsonParser().parse(jsonString).getAsJsonObject();
        final Set<Map.Entry<String, JsonElement>> outerMap = outerJson.entrySet();

        if (outerMap.isEmpty()) {
            return jsonString;
        }
        // Append moduleName to each outer json
        final Map<String, JsonElement> resultMapWithPrefix = new HashMap<>();
        for (Map.Entry<String, JsonElement> elem : outerMap) {
            final JsonElement jsonElement = elem.getValue();
            // apply moduleName only to top level objects
            if (moduleName != null && jsonElement.isJsonObject()) {
                String topLevelModuleName = elem.getKey();
                // Apply moduleName, if it is not already applied
                if (!elem.getKey().contains(":")) {
                    topLevelModuleName = String.format("%s:%s", moduleName, elem.getKey());
                }
                resultMapWithPrefix.put(topLevelModuleName, elem.getValue());
            } else {
                resultMapWithPrefix.put(elem.getKey(), elem.getValue());
            }

        }
        return gson.toJson(resultMapWithPrefix);
    }

    public static String wrapPrimitive(final String identifier, final String toWrap, final Gson gson) {
        final JsonObject result = new JsonObject();
        result.add(identifier, new JsonPrimitive(toWrap));
        return gson.toJson(result);
    }

    public static String wrapPrimitive(final String identifier, final Boolean toWrap, final Gson gson) {
        final JsonObject result = new JsonObject();
        result.add(identifier, new JsonPrimitive(toWrap));
        return gson.toJson(result);
    }

    public static String wrapPrimitive(final String identifier, final Number toWrap, final Gson gson) {
        final JsonObject result = new JsonObject();
        result.add(identifier, new JsonPrimitive(toWrap));
        return gson.toJson(result);
    }

}
