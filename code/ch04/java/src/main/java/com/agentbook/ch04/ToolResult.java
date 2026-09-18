package com.agentbook.ch04;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public record ToolResult(String toolCallId, String name, boolean ok, Map<String, Object> data, String error) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ObjectNode asOpenaiMessage() {
        try {
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("ok", ok);
            payload.set("data", MAPPER.valueToTree(data));
            if (error != null) {
                payload.put("error", error);
            }
            ObjectNode message = MAPPER.createObjectNode();
            message.put("role", "tool");
            message.put("tool_call_id", toolCallId);
            message.put("content", MAPPER.writeValueAsString(payload));
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
