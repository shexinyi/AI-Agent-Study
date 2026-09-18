package com.agentbook.ch04;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Catalog {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final AtomicInteger FLAKY_LEFT = new AtomicInteger(1);

    private Catalog() {
    }

    public static void resetFlaky(int failTimes) {
        FLAKY_LEFT.set(failTimes);
    }

    public static Map<String, ToolSpec> travelSpecs() {
        Map<String, ToolSpec> specs = new LinkedHashMap<>();
        specs.put("get_weather", new ToolSpec(
                "get_weather",
                "查询某城市当前天气，城市用中文名。",
                schema("city"),
                Risk.READ,
                args -> {
                    String city = String.valueOf(args.get("city"));
                    if ("上海".equals(city)) {
                        return Map.of("weather", "小雨", "temp_c", 22);
                    }
                    if ("北京".equals(city)) {
                        return Map.of("weather", "晴", "temp_c", 26);
                    }
                    return Map.of("weather", "未知", "temp_c", "", "city", city);
                }
        ));
        specs.put("get_flight_price", new ToolSpec(
                "get_flight_price",
                "查询某日最低机票价格（人民币）。",
                schema("date", "from_city", "to_city"),
                Risk.READ,
                args -> {
                    boolean cheap = "北京".equals(args.get("from_city")) && "上海".equals(args.get("to_city"));
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("date", args.get("date"));
                    data.put("from_city", args.get("from_city"));
                    data.put("to_city", args.get("to_city"));
                    data.put("lowest_price", cheap ? 680 : 1099);
                    return data;
                }
        ));
        return specs;
    }

    public static Map<String, ToolSpec> supportSpecs() {
        Map<String, ToolSpec> specs = new LinkedHashMap<>();
        specs.put("get_order", new ToolSpec(
                "get_order", "查询订单支付与发货状态。", schema("order_id"), Risk.READ,
                args -> {
                    String id = String.valueOf(args.get("order_id"));
                    if ("ORDER-99".equals(id)) {
                        return Map.of("order_id", id, "shipped", true, "tracking", "TZ123");
                    }
                    if ("ORDER-88".equals(id)) {
                        return Map.of("order_id", id, "shipped", false, "sku", "SKU-9", "days_waiting", 4);
                    }
                    throw new IllegalArgumentException("订单不存在: " + id);
                }
        ));
        specs.put("get_inventory", new ToolSpec(
                "get_inventory", "查询 SKU 库存。下游可能超时，执行器应重试。", schema("sku"), Risk.READ,
                args -> {
                    if (FLAKY_LEFT.getAndDecrement() > 0) {
                        throw new TransientError("库存服务超时");
                    }
                    return Map.of("sku", args.get("sku"), "stock", 0);
                }
        ));
        specs.put("refund_order", new ToolSpec(
                "refund_order", "对订单执行退款。涉及资金，必须显式授权。", schema("order_id"), Risk.MONEY,
                args -> Map.of("order_id", args.get("order_id"), "refunded", true, "amount", 199)
        ));
        specs.put("notify_user", new ToolSpec(
                "notify_user", "向用户发送一条售后通知，不涉及资金。", schema("order_id", "message"), Risk.WRITE,
                args -> Map.of("order_id", args.get("order_id"), "notified", true, "message", args.get("message"))
        ));
        return specs;
    }

    private static ObjectNode schema(String... required) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "object");
        ObjectNode properties = root.putObject("properties");
        for (String key : required) {
            properties.putObject(key).put("type", "string");
        }
        var arr = root.putArray("required");
        for (String key : required) {
            arr.add(key);
        }
        return root;
    }
}
