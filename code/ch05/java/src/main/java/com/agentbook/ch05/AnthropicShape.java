package com.agentbook.ch05;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AnthropicShape {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String system = "你是技术导师。用中文，先结论后理由，三句以内。";
        String user = "为什么 Anthropic 不把 system 放进 messages 数组？";
        String openaiModel = envOr("OPENAI_MODEL", "gpt-4o-mini");
        String anthropicModel = envOr("ANTHROPIC_MODEL", "claude-sonnet-4-5");

        ObjectNode openai = MAPPER.createObjectNode();
        openai.put("model", openaiModel);
        var messages = openai.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);

        ObjectNode anthropic = MAPPER.createObjectNode();
        anthropic.put("model", anthropicModel);
        anthropic.put("max_tokens", 256);
        anthropic.put("system", system);
        anthropic.putArray("messages").addObject().put("role", "user").put("content", user);

        System.out.println("=== 同一轮对话：OpenAI 形状 ===");
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(openai));
        System.out.println("\n=== 同一轮对话：Anthropic 形状 ===");
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(anthropic));
        System.out.println("\n差异要点：system 在 Anthropic 是顶层字段；messages 一般不放 system。");
        System.out.println("鉴权头是 x-api-key + anthropic-version，不是 Authorization: Bearer。");

        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("\n未检测到 ANTHROPIC_API_KEY，解析教学响应。");
            parseAnthropic(sample());
            return;
        }
        System.out.println("\n检测到 Key，发起真实 Messages 请求...\n");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .header("anthropic-version", envOr("ANTHROPIC_VERSION", "2023-06-01"))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(anthropic)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }
        parseAnthropic(MAPPER.readTree(response.body()));
    }

    static void parseAnthropic(JsonNode raw) {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : raw.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        System.out.println("模型回复: " + text);
        System.out.println("stop_reason: " + raw.path("stop_reason").asText());
        System.out.println(Billing.explain(
                raw.path("model").asText("claude-sonnet-4-5"),
                raw.path("usage").path("input_tokens").asInt(0),
                raw.path("usage").path("output_tokens").asInt(0)
        ));
    }

    private static JsonNode sample() throws Exception {
        return MAPPER.readTree("""
                {
                  "id": "msg_teaching",
                  "role": "assistant",
                  "model": "claude-sonnet-4-5",
                  "content": [{"type": "text", "text": "结论：Anthropic 把 system 放在顶层，messages 里通常只有 user/assistant。"}],
                  "stop_reason": "end_turn",
                  "usage": {"input_tokens": 36, "output_tokens": 24}
                }
                """);
    }

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
