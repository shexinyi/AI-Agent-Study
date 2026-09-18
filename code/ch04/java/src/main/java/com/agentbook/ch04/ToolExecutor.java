package com.agentbook.ch04;

import java.util.Map;
import java.util.Set;

public class ToolExecutor {
    private final Map<String, ToolSpec> specs;
    private final Set<String> grantedWrite;
    private final int maxRetries;

    public ToolExecutor(Map<String, ToolSpec> specs, Set<String> grantedWrite, int maxRetries) {
        this.specs = specs;
        this.grantedWrite = grantedWrite;
        this.maxRetries = maxRetries;
    }

    public ToolResult execute(ToolCall call) {
        ToolSpec spec = specs.get(call.name());
        if (spec == null) {
            return new ToolResult(call.id(), call.name(), false, Map.of(), "UNKNOWN_TOOL");
        }
        if ((spec.risk() == Risk.WRITE || spec.risk() == Risk.MONEY) && !grantedWrite.contains(call.name())) {
            return new ToolResult(call.id(), call.name(), false, Map.of(),
                    "PERMISSION_DENIED: " + call.name() + " 风险=" + spec.risk() + "，未在授权名单");
        }
        String schemaError = SchemaValidator.validate(spec.parameters(), call.arguments());
        if (schemaError != null) {
            return new ToolResult(call.id(), call.name(), false, Map.of(), "SCHEMA_ERROR: " + schemaError);
        }
        String lastError = "UNKNOWN";
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> data = spec.handler().apply(call.arguments());
                return new ToolResult(call.id(), call.name(), true, data, null);
            } catch (TransientError e) {
                lastError = e.getMessage();
                if (attempt == maxRetries) {
                    return new ToolResult(call.id(), call.name(), false, Map.of("attempts", attempt), "TRANSIENT: " + lastError);
                }
            } catch (Exception e) {
                return new ToolResult(call.id(), call.name(), false, Map.of(), "HANDLER_ERROR: " + e.getMessage());
            }
        }
        return new ToolResult(call.id(), call.name(), false, Map.of(), "TRANSIENT: " + lastError);
    }
}
