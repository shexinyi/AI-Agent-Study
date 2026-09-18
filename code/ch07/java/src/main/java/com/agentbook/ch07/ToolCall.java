package com.agentbook.ch07;

import java.util.Map;

public record ToolCall(String id, String name, Map<String, Object> arguments, String idempotencyKey) {
}
