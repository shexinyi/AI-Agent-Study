package com.agentbook.ch03;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanExecuteDemo {
    static final List<String> DEFAULT_PLAN = List.of("get_order", "get_shipment", "get_inventory", "search_policy", "decide_action");

    public static void main(String[] args) {
        Shop shop = Shop.seed();
        Goal goal = new Goal(
                "用户投诉 ORDER-99 一直没发货，请按政策处理。",
                "u1",
                List.of("case_resolved"),
                Map.of("order_id", "ORDER-99")
        );
        RunResult result = new AgentRuntime(new PlanAndExecutePlanner(), shop.tools(), StopPolicy.defaults()).run(goal);
        Shop.Eval eval = Shop.evaluate("ORDER-99", result.slots(), shop);
        System.out.println("\nRunResult: " + result.status() + " / 步数=" + result.steps());
        System.out.println("是否改计划: " + result.slots().get("replanned"));
        System.out.println("最终计划: " + result.slots().get("plan"));
        System.out.println("台账退款: " + shop.refunds);
        System.out.println("政策评测: " + eval.ok() + " / " + eval.note());
    }

    static class PlanAndExecutePlanner implements Planner {
        @Override
        public Decision decide(AgentState state) {
            String orderId = String.valueOf(state.goal.constraints().get("order_id"));
            Map<String, Object> s = state.slots;
            if (Boolean.TRUE.equals(s.get("case_resolved"))) {
                return Decision.finish("计划中的结案动作已完成");
            }
            if (!Boolean.TRUE.equals(s.get("has_plan"))) {
                s.put("plan", new ArrayList<>(DEFAULT_PLAN));
                s.put("plan_index", 0);
                s.put("has_plan", true);
                System.out.println("Plan: " + s.get("plan"));
                state.remember("Plan: " + s.get("plan"));
            }
            if (Boolean.TRUE.equals(s.get("shipment_checked")) && Boolean.TRUE.equals(s.get("shipped"))
                    && !Boolean.TRUE.equals(s.get("replanned"))) {
                s.put("plan", new ArrayList<>(List.of("notify_in_transit")));
                s.put("plan_index", 0);
                s.put("replanned", true);
                return Decision.callTool(
                        "观察推翻了「未发货」前提，废弃后续查库存/退款步骤，改计划为通知运单。",
                        "notify_user",
                        Map.of("order_id", orderId, "message", "包裹已发出，运单 " + s.get("tracking"))
                );
            }
            @SuppressWarnings("unchecked")
            List<String> plan = (List<String>) s.get("plan");
            int index = (Integer) s.get("plan_index");
            String stepName = index < plan.size() ? plan.get(index) : "decide_action";
            s.put("plan_index", index + 1);
            return executeStep(stepName, orderId, s);
        }

        private Decision executeStep(String stepName, String orderId, Map<String, Object> s) {
            return switch (stepName) {
                case "get_order" -> Decision.callTool("执行计划步骤 get_order", "get_order", Map.of("order_id", orderId));
                case "get_shipment" -> Decision.callTool("执行计划步骤 get_shipment", "get_shipment", Map.of("order_id", orderId));
                case "get_inventory" -> Decision.callTool("执行计划步骤 get_inventory", "get_inventory", Map.of("sku", s.get("sku")));
                case "search_policy" -> Decision.callTool("执行计划步骤 search_policy", "search_policy", Map.of("topic", "缺货未发货"));
                case "notify_in_transit" -> Decision.callTool("执行改写后的计划：通知在途", "notify_user",
                        Map.of("order_id", orderId, "message", "包裹已发出，运单 " + s.get("tracking")));
                default -> {
                    if (Boolean.TRUE.equals(s.get("shipped"))) {
                        yield Decision.callTool("计划收尾：已发货则通知", "notify_user",
                                Map.of("order_id", orderId, "message", "包裹已发出，运单 " + s.get("tracking")));
                    }
                    int days = s.get("days_waiting") instanceof Integer d ? d : 0;
                    if (Integer.valueOf(0).equals(s.get("stock")) && days >= 3) {
                        yield Decision.callTool("计划收尾：缺货超时则退款", "refund_order", Map.of("order_id", orderId));
                    }
                    yield Decision.askHuman("计划执行完仍无法决策");
                }
            };
        }
    }
}
