package com.agentbook.ch01;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 第 1 章示例 3 的 Java 版：模型选工具，Java 执行工具，再把观察结果回传。
 */
public class ToolCallingAgent {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String goal = "我明天要从北京去上海出差，帮我看一眼上海天气，以及最低机票大概多少钱。";
        System.out.println("目标: " + goal + "\n");
        System.out.println("最终答复:");
        System.out.println(runToolAgent(goal, 6));
    }

    static String runToolAgent(String userGoal, int maxSteps) throws Exception {
        String apiKey = envOrThrow("OPENAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");

        ArrayNode messages = MAPPER.createArrayNode();
        messages.add(textMessage("system",
                "你是出行助手。需要事实信息时必须调用工具，不要编造天气和价格。拿到足够信息后，用中文给出简洁结论。"));
        messages.add(textMessage("user", userGoal));

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

        for (int step = 1; step <= maxSteps; step++) {
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", 0);
            payload.set("messages", messages);
            payload.set("tools", toolsSpec());
            payload.put("tool_choice", "auto");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(baseUrl) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode message = MAPPER.readTree(response.body()).path("choices").path(0).path("message");
            messages.add(message);

            JsonNode toolCalls = message.path("tool_calls");
            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                return message.path("content").asText("");
            }

            System.out.println("第 " + step + " 步：模型决定调用 " + toolCalls.size() + " 个工具");
            for (JsonNode call : toolCalls) {
                String name = call.path("function").path("name").asText();
                String rawArgs = call.path("function").path("arguments").asText("{}");
                JsonNode args = MAPPER.readTree(rawArgs);
                System.out.println("  - " + name + args);
                Object result = dispatch(name, args);

                ObjectNode toolMessage = MAPPER.createObjectNode();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", call.path("id").asText());
                toolMessage.put("content", MAPPER.writeValueAsString(result));
                messages.add(toolMessage);
            }
        }
        return "达到最大步数，提前结束，避免无限调用。";
    }

    private static Object dispatch(String name, JsonNode args) {
        return switch (name) {
            case "get_weather" -> getWeather(args.path("city").asText());
            case "get_flight_price" -> getFlightPrice(
                    args.path("date").asText(),
                    args.path("from_city").asText(),
                    args.path("to_city").asText()
            );
            default -> Map.of("error", "未知工具: " + name);
        };
    }

    private static Map<String, Object> getWeather(String city) {
        if ("上海".equals(city)) {
            return Map.of("weather", "小雨", "temp_c", 22, "wind", "东风3级");
        }
        if ("北京".equals(city)) {
            return Map.of("weather", "晴", "temp_c", 26, "wind", "北风2级");
        }
        return Map.of("weather", "未知", "temp_c", "", "note", "演示数据未覆盖该城市");
    }

    private static Map<String, Object> getFlightPrice(String date, String fromCity, String toCity) {
        int price = ("北京".equals(fromCity) && "上海".equals(toCity)) ? 680 : 1099;
        return Map.of("date", date, "from_city", fromCity, "to_city", toCity, "lowest_price", price);
    }

    private static ArrayNode toolsSpec() {
        ArrayNode tools = MAPPER.createArrayNode();
        tools.add(function(
                "get_weather",
                "查询某城市当前天气，城市用中文名，例如 上海、北京。",
                """
                {"type":"object","properties":{"city":{"type":"string","description":"城市中文名"}},"required":["city"]}
                """
        ));
        tools.add(function(
                "get_flight_price",
                "查询某日从出发地到目的地的最低机票价格（人民币）。",
                """
                {"type":"object","properties":{"date":{"type":"string"},"from_city":{"type":"string"},"to_city":{"type":"string"}},"required":["date","from_city","to_city"]}
                """
        ));
        return tools;
    }

    private static ObjectNode function(String name, String description, String parametersJson) {
        try {
            ObjectNode tool = MAPPER.createObjectNode();
            tool.put("type", "function");
            ObjectNode function = tool.putObject("function");
            function.put("name", name);
            function.put("description", description);
            function.set("parameters", MAPPER.readTree(parametersJson));
            return tool;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static ObjectNode textMessage(String role, String content) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private static String envOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量 " + key);
        }
        return value;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
