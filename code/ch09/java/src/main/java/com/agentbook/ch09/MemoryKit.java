package com.agentbook.ch09;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MemoryKit {
    public record Turn(String role, String content) {
    }

    public record Episode(String at, String kind, String text) {
    }

    public record Fact(String key, String value, String source, double confidence, String updatedAt) {
    }

    public record RememberDecision(boolean write, String reason, Fact fact) {
    }

    private MemoryKit() {
    }

    public static int tokenish(String text) {
        return Math.max(1, text.length());
    }

    public static class WorkingMemory {
        private final int maxTurns;
        public final List<Turn> turns = new ArrayList<>();
        public String summary = "";
        public final Map<String, Object> slots = new LinkedHashMap<>();

        public WorkingMemory(int maxTurns) {
            this.maxTurns = maxTurns;
        }

        public void add(String role, String content) {
            turns.add(new Turn(role, content));
            extractSlots(content);
            while (turns.size() > maxTurns) {
                Turn evicted = turns.remove(0);
                summary = summary.isBlank()
                        ? "已折叠 " + evicted.role() + ":" + evicted.content().substring(0, Math.min(20, evicted.content().length()))
                        : summary + "；+" + evicted.role() + ":" + evicted.content().substring(0, Math.min(20, evicted.content().length()));
            }
        }

        public String packForPrompt() {
            List<String> parts = new ArrayList<>();
            if (!summary.isBlank()) {
                parts.add("[摘要] " + summary);
            }
            if (!slots.isEmpty()) {
                parts.add("[槽位] " + slots.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
            }
            for (Turn turn : turns) {
                parts.add("[" + turn.role() + "] " + turn.content());
            }
            return String.join("\n", parts);
        }

        private void extractSlots(String content) {
            if (content.contains("周一") && content.contains("周三")) {
                slots.put("start_date", "2026-09-14");
                slots.put("end_date", "2026-09-16");
                slots.put("days", 3);
            }
            Matcher name = Pattern.compile("我是([\\u4e00-\\u9fff]{2,4})").matcher(content);
            if (name.find()) {
                slots.put("employee", name.group(1));
            }
            if (content.contains("上海")) {
                slots.put("city", "上海");
            }
        }
    }

    public static class SemanticMemory {
        public final Map<String, Fact> facts = new LinkedHashMap<>();

        public String upsert(Fact fact, String policy) {
            Fact old = facts.get(fact.key());
            if (old == null) {
                facts.put(fact.key(), fact);
                return "inserted";
            }
            if (old.value().equals(fact.value())) {
                facts.put(fact.key(), fact);
                return "refreshed";
            }
            if ("keep_old".equals(policy) || ("higher_confidence".equals(policy) && fact.confidence() < old.confidence())) {
                return "conflict_kept_old";
            }
            facts.put(fact.key(), fact);
            return "conflict_replaced";
        }

        public List<Fact> search(String query) {
            return facts.values().stream()
                    .filter(f -> f.key().contains(query) || f.value().contains(query))
                    .toList();
        }

        public boolean forget(String key) {
            return facts.remove(key) != null;
        }
    }

    public static class EpisodicMemory {
        public final List<Episode> events = new ArrayList<>();

        public void record(String kind, String text) {
            events.add(new Episode(Instant.now().toString(), kind, text));
        }
    }

    public static RememberDecision shouldRemember(String text) {
        if (text.matches("(?s).*(密码|口令|验证码|银行卡).*")) {
            return new RememberDecision(false, "敏感信息，禁止写入长期记忆", null);
        }
        if (text.length() < 8 && text.matches("(?s).*(哈哈|嗯嗯|好的|在吗).*")) {
            return new RememberDecision(false, "寒暄，不值得长期占用", null);
        }
        if (text.contains("喜欢") || text.contains("偏好")) {
            return new RememberDecision(true, "稳定偏好", new Fact("preference", text, "user", 0.8, Instant.now().toString()));
        }
        Matcher name = Pattern.compile("我是([\\u4e00-\\u9fff]{2,4})").matcher(text);
        if (name.find()) {
            boolean correction = text.contains("其实");
            return new RememberDecision(
                    true,
                    correction ? "身份更正" : "身份槽位",
                    new Fact("employee", name.group(1), correction ? "user_correction" : "user",
                            correction ? 0.95 : 0.9, Instant.now().toString())
            );
        }
        return new RememberDecision(false, "一次性任务细节，留给工作记忆即可", null);
    }
}
