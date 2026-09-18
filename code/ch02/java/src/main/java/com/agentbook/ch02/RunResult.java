package com.agentbook.ch02;

import java.util.List;
import java.util.Map;

public record RunResult(
        Status status,
        String reason,
        Map<String, Object> slots,
        List<String> events,
        int steps
) {
    public enum Status {
        SUCCESS, FAILED, TIMEOUT, MAX_STEPS, NEED_HUMAN, REPEATED_TOOL
    }
}
