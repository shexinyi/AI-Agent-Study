package com.agentbook.ch03;

import java.util.List;
import java.util.Map;

public class ReactDemo {
    public static void main(String[] args) {
        Shop shop = Shop.seed();
        Goal goal = new Goal(
                "用户投诉 ORDER-88 一直没发货，请按政策处理。",
                "u1",
                List.of("case_resolved"),
                Map.of("order_id", "ORDER-88")
        );
        RunResult result = new AgentRuntime(new ReactPlanner(), shop.tools(), StopPolicy.defaults()).run(goal);
        Shop.Eval eval = Shop.evaluate("ORDER-88", result.slots(), shop);
        System.out.println("\nRunResult: " + result.status() + " / " + result.reason() + " / 步数=" + result.steps());
        System.out.println("台账退款: " + shop.refunds);
        System.out.println("政策评测: " + eval.ok() + " / " + eval.note());
    }

    static class ReactPlanner implements Planner {
        @Override
        public Decision decide(AgentState state) {
            String orderId = String.valueOf(state.goal.constraints().get("order_id"));
            Map<String, Object> s = state.slots;
            if (Boolean.TRUE.equals(s.get("case_resolved"))) {
                return Decision.finish("观察显示工单已结案，可以结束");
            }
            if (!Boolean.TRUE.equals(s.get("order_loaded"))) {
                return Decision.callTool("用户说一直没发货，先核对订单是否存在以及支付/发货状态。", "get_order", Map.of("order_id", orderId));
            }
            if (!Boolean.TRUE.equals(s.get("shipment_checked"))) {
                return Decision.callTool("订单在，但不能只信状态字段。下一步查物流，排除已经发出。", "get_shipment", Map.of("order_id", orderId));
            }
            if (Boolean.TRUE.equals(s.get("shipped"))) {
                return Decision.callTool("物流显示已在途，应告知运单，而不是退款。", "notify_user",
                        Map.of("order_id", orderId, "message", "包裹已发出，运单 " + s.get("tracking")));
            }
            if (!Boolean.TRUE.equals(s.get("inventory_checked"))) {
                return Decision.callTool("确认未发货。接下来查库存，区分漏发和缺货。", "get_inventory", Map.of("sku", s.get("sku")));
            }
            if (Integer.valueOf(0).equals(s.get("stock")) && !Boolean.TRUE.equals(s.get("policy_checked"))) {
                return Decision.callTool("库存为 0，先查售后政策再决定退款还是让用户继续等。", "search_policy", Map.of("topic", "缺货未发货"));
            }
            int days = s.get("days_waiting") instanceof Integer d ? d : 0;
            if (Integer.valueOf(0).equals(s.get("stock")) && days >= 3) {
                return Decision.callTool("政策允许：未发货且缺货等待超过 3 天，执行退款。", "refund_order", Map.of("order_id", orderId));
            }
            return Decision.askHuman("证据不足，交给人工客服");
        }
    }
}
