/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.commons.util;

import com.google.errorprone.annotations.Var;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public final class JsonUtils {

    private JsonUtils() {
        // Utility class
    }

    public static String wrapJsonWithArray(String jsonString, String wrapper, Gson gson,
            NodeIdentifierWithPredicates predicates) {
        JsonObject innerJson = JsonParser.parseString(jsonString).getAsJsonObject();
        for (Entry<QName, Object> key : predicates.entrySet()) {
            innerJson.add(key.getKey().getLocalName(), gson.toJsonTree(key.getValue()));
        }

        var result = new JsonObject();
        var array = new JsonArray();
        array.add(innerJson);
        result.add(wrapper, array);
        return gson.toJson(result);
    }

    public static String wrapJsonWithObject(String jsonString, String wrapper, Gson gson) {
        JsonElement innerJson = JsonParser.parseString(jsonString);
        var result = new JsonObject();
        result.add(wrapper, innerJson);
        return gson.toJson(result);
    }

    public static String addModuleNamePrefixToJson(String jsonString, String moduleName, Gson gson) {
        JsonObject outerJson = JsonParser.parseString(jsonString).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> outerMap = outerJson.entrySet();

        if (outerMap.isEmpty()) {
            return jsonString;
        }
        // Append moduleName to each outer json
        Map<String, JsonElement> resultMapWithPrefix = new HashMap<>();
        for (Map.Entry<String, JsonElement> elem : outerMap) {
            JsonElement jsonElement = elem.getValue();
            // apply moduleName only to top level objects
            if (moduleName != null && jsonElement.isJsonObject()) {
                @Var String topLevelModuleName = elem.getKey();
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

    public static String wrapPrimitive(String identifier, String toWrap, Gson gson) {
        var result = new JsonObject();
        result.add(identifier, new JsonPrimitive(toWrap));
        return gson.toJson(result);
    }

    public static String wrapPrimitive(String identifier, Boolean toWrap, Gson gson) {
        var result = new JsonObject();
        result.add(identifier, new JsonPrimitive(toWrap));
        return gson.toJson(result);
    }

    public static String wrapPrimitive(String identifier, Number toWrap, Gson gson) {
        var result = new JsonObject();
        result.add(identifier, new JsonPrimitive(toWrap));
        return gson.toJson(result);
    }

}
