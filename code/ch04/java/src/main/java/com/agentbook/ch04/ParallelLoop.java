package com.agentbook.ch04;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParallelLoop {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        String answer = runLoop("明天北京去上海出差，上海天气和最低机票各是多少？");
        System.out.println("\n最终答复: " + answer);
    }

    static String runLoop(String userText) {
        Map<String, ToolSpec> specs = Catalog.travelSpecs();
        ToolExecutor executor = new ToolExecutor(specs, Set.of(), 2);
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(text("system", "需要事实时并行调用工具，不要编造。"));
        messages.add(text("user", userText));
        for (int round = 1; round <= 4; round++) {
            ObjectNode assistant = complete(messages);
            messages.add(assistant);
            JsonNode calls = assistant.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                return assistant.path("content").asText("");
            }
            System.out.println("第 " + round + " 轮：并行 " + calls.size() + " 个工具");
            for (JsonNode raw : calls) {
                ToolCall call = ToolCall.fromOpenai(raw);
                ToolResult result = executor.execute(call);
                System.out.println("  " + call.name() + call.arguments() + " -> " + (result.ok() ? result.data() : result.error()));
                messages.add(result.asOpenaiMessage());
            }
        }
        return "达到最大轮次";
    }

    static ObjectNode complete(List<ObjectNode> messages) {
        boolean hasTool = messages.stream().anyMatch(item -> "tool".equals(item.path("role").asText()));
        if (!hasTool) {
            ObjectNode assistant = MAPPER.createObjectNode();
            assistant.put("role", "assistant");
            assistant.putNull("content");
            ArrayNode calls = assistant.putArray("tool_calls");
            calls.add(call("call_weather", "get_weather", "{\"city\":\"上海\"}"));
            calls.add(call("call_price", "get_flight_price",
                    "{\"date\":\"2026-09-14\",\"from_city\":\"北京\",\"to_city\":\"上海\"}"));
            return assistant;
        }
        String weather = "";
        int temp = 0;
        int price = 0;
        for (ObjectNode item : messages) {
            if (!"tool".equals(item.path("role").asText())) {
                continue;
            }
            try {
                JsonNode payload = MAPPER.readTree(item.path("content").asText());
                JsonNode data = payload.path("data");
                if (data.has("weather")) {
                    weather = data.path("weather").asText();
                    temp = data.path("temp_c").asInt();
                }
                if (data.has("lowest_price")) {
                    price = data.path("lowest_price").asInt();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        ObjectNode assistant = MAPPER.createObjectNode();
        assistant.put("role", "assistant");
        assistant.put("content", "上海今天" + weather + "，" + temp + "°C；北京到上海最低票价约 " + price + " 元。");
        return assistant;
    }

    private static ObjectNode call(String id, String name, String arguments) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("id", id);
        node.put("type", "function");
        ObjectNode fn = node.putObject("function");
        fn.put("name", name);
        fn.put("arguments", arguments);
        return node;
    }

    private static ObjectNode text(String role, String content) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }
}
