package com.agentbook.ch04;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Set;

public class ProtocolRoundtrip {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Map<String, ToolSpec> specs = Catalog.travelSpecs();
        ObjectNode request = MAPPER.createObjectNode();
        request.put("model", "gpt-4o-mini");
        request.put("tool_choice", "auto");
        ArrayNode messages = request.putArray("messages");
        messages.add(text("system", "需要事实时必须调用工具，不要编造天气和价格。"));
        messages.add(text("user", "上海今天天气怎样？"));
        ArrayNode tools = request.putArray("tools");
        specs.values().forEach(spec -> tools.add(spec.asOpenaiTool()));

        System.out.println("=== 发给模型的请求（节选 tools + messages）===");
        ObjectNode excerpt = MAPPER.createObjectNode();
        excerpt.set("messages", request.get("messages"));
        excerpt.set("tools", request.get("tools"));
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(excerpt));

        JsonNode assistant = fakeModelResponse();
        System.out.println("\n=== 模型返回的 assistant 消息 ===");
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(assistant));

        ToolExecutor executor = new ToolExecutor(specs, Set.of(), 2);
        System.out.println("\n=== 执行器观察 ===");
        ArrayNode toolMessages = MAPPER.createArrayNode();
        for (JsonNode raw : assistant.path("tool_calls")) {
            ToolCall call = ToolCall.fromOpenai(raw);
            ToolResult result = executor.execute(call);
            System.out.println(call.name() + call.arguments() + " -> ok=" + result.ok() + " data=" + result.data() + " error=" + result.error());
            toolMessages.add(result.asOpenaiMessage());
        }
        System.out.println("\n=== 追加回 messages 的 tool 消息 ===");
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(toolMessages));
        System.out.println("\n下一轮 messages 条数: " + (request.get("messages").size() + 1 + toolMessages.size()) + "（system/user/assistant/tool）");
    }

    private static ObjectNode text(String role, String content) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private static ObjectNode fakeModelResponse() {
        ObjectNode assistant = MAPPER.createObjectNode();
        assistant.put("role", "assistant");
        assistant.putNull("content");
        ObjectNode call = assistant.putArray("tool_calls").addObject();
        call.put("id", "call_weather_1");
        call.put("type", "function");
        ObjectNode fn = call.putObject("function");
        fn.put("name", "get_weather");
        fn.put("arguments", "{\"city\":\"上海\"}");
        return assistant;
    }
}
