package com.agentbook.ch04;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SchemaValidator {
    private SchemaValidator() {
    }

    public static String validate(JsonNode schema, Map<String, Object> args) {
        List<String> missing = new ArrayList<>();
        for (JsonNode key : schema.path("required")) {
            if (!args.containsKey(key.asText())) {
                missing.add(key.asText());
            }
        }
        if (!missing.isEmpty()) {
            return "缺少参数: " + missing;
        }
        JsonNode properties = schema.path("properties");
        for (String key : args.keySet()) {
            if (!properties.has(key)) {
                return "未声明参数: " + key;
            }
            String type = properties.path(key).path("type").asText();
            Object value = args.get(key);
            if ("string".equals(type) && !(value instanceof String)) {
                return key + " 应为 string";
            }
            if ("integer".equals(type) && !(value instanceof Integer)) {
                return key + " 应为 integer";
            }
            if ("boolean".equals(type) && !(value instanceof Boolean)) {
                return key + " 应为 boolean";
            }
        }
        return null;
    }
}
