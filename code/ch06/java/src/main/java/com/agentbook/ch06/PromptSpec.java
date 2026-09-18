package com.agentbook.ch06;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record PromptSpec(
        String name,
        String version,
        String system,
        String userTemplate,
        List<String> constraints,
        String outputContract,
        List<Example> examples
) {
    public String renderSystem() {
        StringBuilder sb = new StringBuilder(system);
        if (!constraints.isEmpty()) {
            sb.append("\n\n约束：\n- ").append(String.join("\n- ", constraints));
        }
        if (outputContract != null && !outputContract.isBlank()) {
            sb.append("\n\n输出契约：\n").append(outputContract);
        }
        return sb.toString();
    }

    public String renderUser(Map<String, String> variables) {
        String text = userTemplate;
        for (var entry : variables.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (text.contains("{")) {
            throw new IllegalArgumentException("模板仍有未替换变量: " + text);
        }
        return text;
    }

    public List<Message> messages(Map<String, String> variables) {
        List<Message> items = new ArrayList<>();
        items.add(new Message("system", renderSystem()));
        for (Example ex : examples) {
            items.add(new Message("user", ex.user()));
            items.add(new Message("assistant", ex.assistant()));
        }
        items.add(new Message("user", renderUser(variables)));
        return items;
    }
}
