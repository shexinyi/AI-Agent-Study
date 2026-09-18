package com.agentbook.ch04;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.function.Function;

public record ToolSpec(
        String name,
        String description,
        JsonNode parameters,
        Risk risk,
        Function<Map<String, Object>, Map<String, Object>> handler
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ObjectNode asOpenaiTool() {
        ObjectNode tool = MAPPER.createObjectNode();
        tool.put("type", "function");
        ObjectNode function = tool.putObject("function");
        function.put("name", name);
        function.put("description", description);
        function.set("parameters", parameters);
        return tool;
    }
}
