package com.agentbook.ch07;

import java.util.Map;

public record ToolResult(boolean ok, Map<String, Object> data, String error, boolean replayed) {
    public static ToolResult fail(String error) {
        return new ToolResult(false, Map.of(), error, false);
    }

    public static ToolResult ok(Map<String, Object> data) {
        return new ToolResult(true, data, null, false);
    }
}
