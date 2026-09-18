package com.agentbook.ch07;

public record AuditEvent(
        String tool,
        boolean ok,
        String risk,
        long latencyMs,
        String idempotencyKey,
        String error,
        boolean replayed
) {
}
