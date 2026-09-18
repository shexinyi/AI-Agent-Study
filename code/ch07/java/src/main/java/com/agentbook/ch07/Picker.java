package com.agentbook.ch07;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class Picker {
    private Picker() {
    }

    public static List<Map.Entry<String, Integer>> pick(String userText, Map<String, ToolSpec> specs, int topK) {
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();
        for (ToolSpec spec : specs.values()) {
            int score = 0;
            for (String word : spec.keywords()) {
                if (userText.contains(word)) {
                    score += 3;
                }
            }
            if (score > 0) {
                scored.add(Map.entry(spec.name(), score));
            }
        }
        scored.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    public static List<ToolCall> toCalls(String userText, Map<String, ToolSpec> specs) {
        List<ToolCall> calls = new ArrayList<>();
        int index = 1;
        for (var item : pick(userText, specs, 2)) {
            ToolSpec spec = specs.get(item.getKey());
            Map<String, Object> args = new java.util.LinkedHashMap<>();
            if (spec.required().contains("order_id")) {
                args.put("order_id", userText.contains("99") ? "ORDER-99" : "ORDER-88");
            }
            if (spec.required().contains("topic")) {
                args.put("topic", "退款");
            }
            String key = "user-demo-" + spec.name() + "-" + args.getOrDefault("order_id", args.getOrDefault("topic", "x"));
            calls.add(new ToolCall("call_" + index++, spec.name(), args, key));
        }
        return calls;
    }
}
