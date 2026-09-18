package com.agentbook.ch18;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CheckpointHitl {
    public static void main(String[] args) {
        MiniGraph g = graph();
        Map<String, Object> seed88 = Map.of("question", "ORDER-88 能不能退", "facts", "", "answer", "", "approved", false, "trace", List.of());

        System.out.println("thread linqiu-order88：未发货退款，必须停在人工节点。");
        MiniGraph.Result paused = g.invoke(seed88, "linqiu-order88");
        System.out.println("第一次: " + (paused.paused() ? "PAUSED " + paused.interrupt : paused.state.get("answer")));
        System.out.println("检查点停在 gate，thread_id=linqiu-order88");

        MiniGraph.Result done = g.invoke(seed88, "linqiu-order88", "确认");
        System.out.println("确认后: " + done.state.get("answer") + " approved=" + done.state.get("approved"));

        System.out.println("\nthread linqiu-order99：已发货，不打断。");
        MiniGraph.Result shipped = g.invoke(Map.of("question", "TZ123 那张单能直接退吗", "facts", "", "trace", List.of()), "linqiu-order99");
        System.out.println("99: " + shipped.state.get("answer"));

        System.out.println("\n读法：LangGraph 的价值是检查点协议。Java 这里用同构 MiniGraph，不假装有官方 LCEL。");
        System.out.println("恢复时 gate 会再跑一遍，所以 fetch 必须在打断前面。");
    }

    static MiniGraph graph() {
        return new MiniGraph()
                .addNode("fetch", (s, r) -> {
                    var fact = Kit.getOrder(Kit.orderIdOf(String.valueOf(s.get("question"))));
                    return Map.of("facts", fact.toString(), "trace", List.of("fetch:" + fact.get("order_id")));
                })
                .addNode("gate", (s, r) -> {
                    String facts = String.valueOf(s.getOrDefault("facts", ""));
                    if (!facts.contains("unshipped")) {
                        return Map.of("answer", "已发货须走售后，不调用写工具。", "approved", false, "trace", List.of("gate:skip"));
                    }
                    if (r == null) {
                        throw new MiniGraph.Interrupt(Map.of("action", "refund_order", "order_id", "ORDER-88"));
                    }
                    if (Set.of("确认", "yes", "Y").contains(String.valueOf(r).strip())) {
                        return Map.of("answer", "已执行 refund_order(ORDER-88)。", "approved", true, "trace", List.of("gate:ok"));
                    }
                    return Map.of("answer", "用户拒绝，未退款。", "approved", false, "trace", List.of("gate:deny"));
                })
                .addEdge("start", "fetch")
                .addEdge("fetch", "gate")
                .addEdge("gate", "end");
    }
}
