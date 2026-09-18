package com.agentbook.ch04;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionRetry {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        run();
    }

    static void run() throws Exception {
        Catalog.resetFlaky(1);
        Map<String, ToolSpec> specs = Catalog.supportSpecs();
        Set<String> granted = Set.of("notify_user");
        ToolExecutor executor = new ToolExecutor(specs, granted, 2);
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(text("system", "按政策处理 ORDER-88。资金工具未授权时不要编造成功。"));
        messages.add(text("user", "ORDER-88 一直没发货，给我退款。"));
        System.out.println("写授权名单: " + granted + "\n");

        for (int round = 1; round <= 8; round++) {
            ObjectNode assistant = complete(messages);
            messages.add(assistant);
            JsonNode calls = assistant.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                System.out.println("最终: " + assistant.path("content").asText());
                return;
            }
            List<String> names = new ArrayList<>();
            calls.forEach(item -> names.add(item.path("function").path("name").asText()));
            System.out.println("第 " + round + " 轮模型点菜: " + names);
            for (JsonNode raw : calls) {
                ToolCall call = ToolCall.fromOpenai(raw);
                ToolResult result = executor.execute(call);
                System.out.println("  执行 " + call.name() + call.arguments() + " -> "
                        + (result.ok() ? "OK " + result.data() : result.error()));
                messages.add(result.asOpenaiMessage());
                if (result.ok() && "notify_user".equals(call.name())) {
                    System.out.println("\n结案：通知已发出，退款未发生。");
                    return;
                }
            }
        }
    }

    static ObjectNode complete(List<ObjectNode> messages) throws Exception {
        boolean hasOrder = false;
        boolean hasStock = false;
        boolean denied = false;
        for (ObjectNode item : messages) {
            if (!"tool".equals(item.path("role").asText())) {
                continue;
            }
            JsonNode payload = MAPPER.readTree(item.path("content").asText());
            if (payload.path("error").asText("").startsWith("PERMISSION_DENIED")) {
                denied = true;
            }
            if (payload.path("data").has("shipped")) {
                hasOrder = true;
            }
            if (payload.path("data").has("stock")) {
                hasStock = true;
            }
        }
        if (!hasOrder) {
            return call("call_order", "get_order", "{\"order_id\":\"ORDER-88\"}");
        }
        if (!hasStock && !denied) {
            return call("call_stock", "get_inventory", "{\"sku\":\"SKU-9\"}");
        }
        if (!denied) {
            return call("call_refund", "refund_order", "{\"order_id\":\"ORDER-88\"}");
        }
        return call("call_notify", "notify_user",
                "{\"order_id\":\"ORDER-88\",\"message\":\"退款未授权，已升级人工；库存为 0，请等待主管审批。\"}");
    }

    private static ObjectNode call(String id, String name, String arguments) {
        ObjectNode assistant = MAPPER.createObjectNode();
        assistant.put("role", "assistant");
        assistant.putNull("content");
        ObjectNode toolCall = assistant.putArray("tool_calls").addObject();
        toolCall.put("id", id);
        toolCall.put("type", "function");
        ObjectNode fn = toolCall.putObject("function");
        fn.put("name", name);
        fn.put("arguments", arguments);
        return assistant;
    }

    private static ObjectNode text(String role, String content) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }
}
