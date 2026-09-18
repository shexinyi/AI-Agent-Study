package com.agentbook.ch04;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public record ToolCall(String id, String name, Map<String, Object> arguments) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ToolCall fromOpenai(JsonNode raw) {
        try {
            JsonNode fn = raw.path("function");
            String argsText = fn.path("arguments").asText("{}");
            JsonNode parsed = MAPPER.readTree(argsText);
            Map<String, Object> args = new LinkedHashMap<>();
            parsed.fields().forEachRemaining(entry -> args.put(entry.getKey(), toJava(entry.getValue())));
            return new ToolCall(raw.path("id").asText(), fn.path("name").asText(), args);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析 tool_call", e);
        }
    }

    private static Object toJava(JsonNode node) {
        if (node.isInt()) {
            return node.asInt();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        return node.asText();
    }
}
