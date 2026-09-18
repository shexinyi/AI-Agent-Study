package com.agentbook.ch07;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Catalog {
    static final Map<String, Map<String, Object>> ORDERS = Map.of(
            "ORDER-88", Map.of("shipped", false, "sku", "SKU-9", "user", "林秋"),
            "ORDER-99", Map.of("shipped", true, "sku", "SKU-9", "tracking", "TZ123", "user", "林秋")
    );
    static final List<String> REFUNDS = new ArrayList<>();

    private Catalog() {
    }

    public static Map<String, ToolSpec> badSpecs() {
        return Map.of("do_stuff", new ToolSpec(
                "do_stuff", "处理用户的问题，必要时看看数据。", "随便", "不清楚",
                List.of("q"), "write", 5, false,
                args -> Map.of("echo", args.get("q")),
                List.of("处理", "看看")
        ));
    }

    public static Map<String, ToolSpec> goodSpecs() {
        Map<String, ToolSpec> specs = new LinkedHashMap<>();
        specs.put("get_order", new ToolSpec(
                "get_order", "按订单号查询支付、发货、运单。只读，无副作用。",
                "用户提到具体订单号，需要核对状态。", "用户只是问政策，或还没有订单号。",
                List.of("order_id"), "read", 1.0, true,
                Catalog::getOrder,
                List.of("订单", "查单", "发货了吗", "ORDER-")
        ));
        specs.put("search_policy", new ToolSpec(
                "search_policy", "检索售后政策条文，返回一段只读说明。",
                "用户问能不能退、要不要承担运费、规则是什么。", "用户已经要求执行退款或改地址。",
                List.of("topic"), "read", 1.0, true,
                args -> Map.of("topic", args.get("topic"), "text", "已发货禁止直接退款；未发货缺货超 3 天可退。"),
                List.of("政策", "能不能退", "可以退吗", "规定", "规则")
        ));
        specs.put("refund_order", new ToolSpec(
                "refund_order", "对指定订单执行退款。资金动作，必须已授权且已核对物流。",
                "已确认未发货且政策允许，并且调用方带来幂等键。", "只是咨询、已发货、缺少订单号。",
                List.of("order_id"), "money", 2.0, true,
                Catalog::refund,
                List.of("给我退", "退钱", "马上退", "执行退款")
        ));
        specs.put("slow_lookup", new ToolSpec(
                "slow_lookup", "演示超时：故意 sleep 的查单。",
                "只在超时教学里使用。", "任何生产路径。",
                List.of("order_id"), "read", 0.1, true,
                args -> {
                    try {
                        Thread.sleep(350);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return getOrder(args);
                },
                List.of("慢查询")
        ));
        return specs;
    }

    static Map<String, Object> getOrder(Map<String, Object> args) {
        String id = String.valueOf(args.get("order_id"));
        Map<String, Object> order = ORDERS.get(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        Map<String, Object> data = new LinkedHashMap<>(order);
        data.put("order_id", id);
        return data;
    }

    static Map<String, Object> refund(Map<String, Object> args) {
        String id = String.valueOf(args.get("order_id"));
        if (!ORDERS.containsKey(id)) {
            throw new IllegalArgumentException("订单不存在: " + id);
        }
        REFUNDS.add(id);
        return Map.of("order_id", id, "refunded", true, "amount", 199);
    }
}
