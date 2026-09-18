package com.agentbook.ch07;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record ToolSpec(
        String name,
        String description,
        String whenToUse,
        String whenNot,
        List<String> required,
        String risk,
        double timeoutSeconds,
        boolean idempotent,
        Function<Map<String, Object>, Map<String, Object>> handler,
        List<String> keywords
) {
    public String menuText() {
        return name + ": " + description + " 使用时机: " + whenToUse + " 不要用于: " + whenNot;
    }
}
