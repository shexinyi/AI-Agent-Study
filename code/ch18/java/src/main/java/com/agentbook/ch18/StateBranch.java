package com.agentbook.ch18;

import java.util.List;
import java.util.Map;

public class StateBranch {
    public static void main(String[] args) {
        MiniGraph g = new MiniGraph()
                .addNode("route", (s, r) -> {
                    String route = Kit.routeOf(String.valueOf(s.get("question")));
                    return Map.of("route", route, "trace", List.of("route:" + route));
                })
                .addNode("leave", (s, r) -> Map.of("facts", Kit.leaveFact(), "trace", List.of("node:leave")))
                .addNode("order", (s, r) -> {
                    var fact = Kit.getOrder(Kit.orderIdOf(String.valueOf(s.get("question"))));
                    return Map.of("facts", fact.toString(), "trace", List.of("node:order:" + fact.get("order_id")));
                })
                .addNode("refuse", (s, r) -> Map.of("answer", "【不足】不是请假也不是订单。", "trace", List.of("node:refuse")))
                .addNode("draft", (s, r) -> {
                    if (s.get("answer") != null && !String.valueOf(s.get("answer")).isBlank()) {
                        return Map.of("trace", List.of("draft:skip"));
                    }
                    return Map.of("answer", Kit.draftFromFacts(String.valueOf(s.getOrDefault("facts", ""))),
                            "trace", List.of("node:draft"));
                })
                .addEdge("start", "route")
                .addCond("route", s -> String.valueOf(s.get("route")),
                        Map.of("leave", "leave", "order", "order", "unknown", "refuse"))
                .addEdge("leave", "draft")
                .addEdge("order", "draft")
                .addEdge("refuse", "end")
                .addEdge("draft", "end");

        System.out.println("条件边把一张图分成三条腿，不是复制三条链。");
        for (String q : List.of("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单", "食堂几点开门")) {
            MiniGraph.Result out = g.invoke(Map.of("question", q, "trace", List.of()), "t-" + q.hashCode());
            System.out.println("\n问: " + q);
            @SuppressWarnings("unchecked")
            List<String> trace = (List<String>) out.state.get("trace");
            System.out.println("轨迹: " + String.join(" → ", trace));
            System.out.println("答: " + out.state.get("answer"));
        }
        System.out.println("\n读法：State 是黑板上的字。Node 擦写几格。Edge 决定下一支粉笔。");
    }
}
