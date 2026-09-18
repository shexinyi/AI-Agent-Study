package com.agentbook.ch03;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReflexionDemo {
    public static void main(String[] args) {
        runWithReflexion("ORDER-99", 2);
    }

    static void runWithReflexion(String orderId, int maxTrials) {
        List<String> lessons = new ArrayList<>();
        for (int trial = 1; trial <= maxTrials; trial++) {
            Shop shop = Shop.seed();
            Goal goal = new Goal(
                    "用户投诉 " + orderId + " 一直没发货，请按政策处理。",
                    "u1",
                    List.of("case_resolved"),
                    Map.of("order_id", orderId)
            );
            Planner planner = lessons.isEmpty() ? new NaivePlanner() : new LessonAwarePlanner(lessons);
            System.out.println("=".repeat(60));
            System.out.println("第 " + trial + " 次尝试 / 已有教训: " + (lessons.isEmpty() ? "无" : lessons));
            RunResult result = new AgentRuntime(planner, shop.tools(), StopPolicy.defaults()).run(goal);
            Shop.Eval eval = Shop.evaluate(orderId, result.slots(), shop);
            System.out.println("\nRunResult: " + result.status() + "  台账退款: " + shop.refunds);
            System.out.println("政策评测: " + eval.ok() + " / " + eval.note());
            if (eval.ok()) {
                System.out.println("Reflexion 结束：评测通过。");
                return;
            }
            String lesson = reflect(eval.note());
            lessons.add(lesson);
            System.out.println("Reflection: " + lesson);
        }
        System.out.println("达到最大尝试次数，仍未通过评测。");
    }

    static String reflect(String evalNote) {
        if (evalNote.contains("已发货订单被退款")) {
            return "退款前必须调用 get_shipment；若 shipped=true，只能 notify_user，禁止 refund_order。";
        }
        return "上一轮失败：" + evalNote;
    }

    static class NaivePlanner implements Planner {
        @Override
        public Decision decide(AgentState state) {
            if (Boolean.TRUE.equals(state.slots.get("case_resolved"))) {
                return Decision.finish("已经退款，自认为做完了");
            }
            String orderId = String.valueOf(state.goal.constraints().get("order_id"));
            return Decision.callTool("用户很着急，直接退款安抚，不必查物流。", "refund_order", Map.of("order_id", orderId));
        }
    }

    static class LessonAwarePlanner implements Planner {
        private final List<String> lessons;

        LessonAwarePlanner(List<String> lessons) {
            this.lessons = lessons;
        }

        @Override
        public Decision decide(AgentState state) {
            String orderId = String.valueOf(state.goal.constraints().get("order_id"));
            Map<String, Object> s = state.slots;
            if (Boolean.TRUE.equals(s.get("case_resolved"))) {
                return Decision.finish("本轮已结案");
            }
            if (!Boolean.TRUE.equals(s.get("order_loaded"))) {
                return Decision.callTool("教训在耳边：" + lessons.get(lessons.size() - 1) + " 先加载订单。",
                        "get_order", Map.of("order_id", orderId));
            }
            if (!Boolean.TRUE.equals(s.get("shipment_checked"))) {
                return Decision.callTool("教训要求：退款前必须查看物流。", "get_shipment", Map.of("order_id", orderId));
            }
            if (Boolean.TRUE.equals(s.get("shipped"))) {
                return Decision.callTool("已发货，按教训改为通知运单，禁止退款。", "notify_user",
                        Map.of("order_id", orderId, "message", "包裹已发出，运单 " + s.get("tracking")));
            }
            return Decision.callTool("未发货才允许走退款。", "refund_order", Map.of("order_id", orderId));
        }
    }
}
