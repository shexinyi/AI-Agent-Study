package com.agentbook.ch03;

import java.util.List;
import java.util.Map;

public record Goal(String description, String userId, List<String> successWhen, Map<String, Object> constraints) {
    public boolean isSatisfied(Map<String, Object> slots) {
        return successWhen.stream().allMatch(flag -> Boolean.TRUE.equals(slots.get(flag)));
    }
}
