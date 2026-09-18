package com.agentbook.ch03;

import java.util.Map;

public record Decision(Kind kind, String reason, String toolName, Map<String, Object> toolArgs) {
    public enum Kind { CALL_TOOL, FINISH, FAIL, ASK_HUMAN }

    public static Decision finish(String reason) {
        return new Decision(Kind.FINISH, reason, null, Map.of());
    }

    public static Decision askHuman(String reason) {
        return new Decision(Kind.ASK_HUMAN, reason, null, Map.of());
    }

    public static Decision callTool(String reason, String toolName, Map<String, Object> args) {
        return new Decision(Kind.CALL_TOOL, reason, toolName, args);
    }
}
