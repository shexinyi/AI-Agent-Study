package com.agentbook.ch18;

import java.util.List;
import java.util.Map;

public class ChainNeedsLoop {
    public static void main(String[] args) {
        System.out.println("线性链没有回边，第一次不是 JSON 就结束。");
        String raw = Kit.modelOnce(0);
        System.out.println("链: 链断了: 不是 JSON → " + raw);

        MiniGraph g = new MiniGraph()
                .addNode("draft", (s, r) -> {
                    int steps = ((Number) s.getOrDefault("steps", 0)).intValue();
                    return Map.of("draft", Kit.modelOnce(steps), "steps", steps + 1, "trace", List.of("draft#" + steps));
                })
                .addNode("parse", (s, r) -> {
                    String draft = String.valueOf(s.get("draft"));
                    if (draft.startsWith("{")) {
                        return Map.of("ok", true, "answer", Kit.getOrder("ORDER-88").toString(), "trace", List.of("parse_ok"));
                    }
                    return Map.of("ok", false, "answer", "【不足】解析失败", "trace", List.of("parse_fail"));
                })
                .addEdge("start", "draft")
                .addEdge("draft", "parse")
                .addCond("parse", s -> {
                    if (Boolean.TRUE.equals(s.get("ok"))) {
                        return "pass";
                    }
                    return ((Number) s.getOrDefault("steps", 0)).intValue() < 2 ? "retry" : "fail";
                }, Map.of("pass", "end", "retry", "draft", "fail", "end"));

        MiniGraph.Result out = g.invoke(Map.of("question", "ORDER-88 能不能退", "steps", 0, "ok", false, "trace", List.of()), "loop");
        System.out.println("图: " + out.state.get("answer"));
        System.out.println("轨迹: " + out.state.get("trace"));
        System.out.println("\n读法：Agent 要循环。Chain 是一条管子，Graph 才能长出回边。");
    }
}
