package com.agentbook.ch03;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shop {
    public final Map<String, Map<String, Object>> orders = new HashMap<>();
    public final Map<String, Integer> inventory = new HashMap<>();
    public final List<String> refunds = new ArrayList<>();

    public static Shop seed() {
        Shop shop = new Shop();
        shop.orders.put("ORDER-88", order("u1", false, "SKU-9", 4, null));
        shop.orders.put("ORDER-99", order("u1", true, "SKU-9", 1, "TZ123"));
        shop.inventory.put("SKU-9", 0);
        return shop;
    }

    private static Map<String, Object> order(String userId, boolean shipped, String sku, int days, String tracking) {
        Map<String, Object> item = new HashMap<>();
        item.put("user_id", userId);
        item.put("status", "paid");
        item.put("shipped", shipped);
        item.put("sku", sku);
        item.put("days_waiting", days);
        if (tracking != null) {
            item.put("tracking", tracking);
        }
        return item;
    }

    public ToolRegistry tools() {
        ToolRegistry tools = new ToolRegistry();
        tools.register("get_order", args -> {
            String orderId = String.valueOf(args.get("order_id"));
            Map<String, Object> order = orders.get(orderId);
            if (order == null) {
                return new Observation(false, Map.of(), "订单不存在: " + orderId);
            }
            Map<String, Object> data = new HashMap<>(order);
            data.put("order_loaded", true);
            data.put("order_id", orderId);
            return new Observation(true, data, "订单 " + orderId + ": 已支付, 已发货=" + order.get("shipped") + ", SKU=" + order.get("sku"));
        });
        tools.register("get_shipment", args -> {
            String orderId = String.valueOf(args.get("order_id"));
            Map<String, Object> order = orders.get(orderId);
            boolean shipped = Boolean.TRUE.equals(order.get("shipped"));
            Object tracking = order.get("tracking");
            Map<String, Object> data = new HashMap<>();
            data.put("shipment_checked", true);
            data.put("shipped", shipped);
            data.put("tracking", tracking);
            return new Observation(true, data, shipped ? "物流: 在途 " + tracking : "物流: 尚未发出");
        });
        tools.register("get_inventory", args -> {
            String sku = String.valueOf(args.get("sku"));
            int stock = inventory.getOrDefault(sku, 0);
            return new Observation(true, Map.of("inventory_checked", true, "sku", sku, "stock", stock), sku + " 库存=" + stock);
        });
        tools.register("search_policy", args -> {
            String text = "未发货且缺货等待超过 3 天，可退款或协商换货。已发货订单禁止直接退款，应提供运单并引导催件。";
            return new Observation(true, Map.of("policy_checked", true, "policy", text), text);
        });
        tools.register("refund_order", args -> {
            String orderId = String.valueOf(args.get("order_id"));
            refunds.add(orderId);
            return new Observation(true, Map.of("refunded", true, "case_resolved", true, "resolution", "refund"), "已退款 " + orderId);
        });
        tools.register("notify_user", args -> {
            String message = String.valueOf(args.get("message"));
            return new Observation(true, Map.of(
                    "notified", true,
                    "case_resolved", true,
                    "resolution", "notify",
                    "notify_message", message
            ), "已通知用户: " + message);
        });
        return tools;
    }

    public static Eval evaluate(String orderId, Map<String, Object> slots, Shop shop) {
        Map<String, Object> order = shop.orders.get(orderId);
        if (Boolean.TRUE.equals(order.get("shipped")) && Boolean.TRUE.equals(slots.get("refunded"))) {
            return new Eval(false, "已发货订单被退款，违反售后政策");
        }
        if (!Boolean.TRUE.equals(slots.get("case_resolved"))) {
            return new Eval(false, "工单未结案");
        }
        if (Boolean.TRUE.equals(order.get("shipped")) && !"notify".equals(slots.get("resolution"))) {
            return new Eval(false, "已发货订单应通知运单，而不是其他动作");
        }
        int days = (Integer) order.get("days_waiting");
        if (!Boolean.TRUE.equals(order.get("shipped")) && days >= 3 && !"refund".equals(slots.get("resolution"))) {
            return new Eval(false, "长时缺货未发货应按政策退款");
        }
        return new Eval(true, "政策校验通过");
    }

    public record Eval(boolean ok, String note) {
    }
}
