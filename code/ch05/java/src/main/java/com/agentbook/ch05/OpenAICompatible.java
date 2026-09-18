package com.agentbook.ch05;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenAICompatible {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String question = "大模型 API 到底在卖什么？请用买菜来类比。";
        String model = envOr("OPENAI_MODEL", "gpt-4o-mini");
        String baseUrl = trimSlash(envOr("OPENAI_BASE_URL", "https://api.openai.com/v1"));
        ObjectNode body = payload(question, model);

        System.out.println("=== 将 POST 的地址 ===");
        System.out.println(baseUrl + "/chat/completions");
        System.out.println("=== 将发送的 JSON ===");
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(body));
        String joined = question + "你是技术导师。用中文，先结论后理由，三句以内。";
        System.out.println("=== 本地粗估输入 Token ===");
        System.out.println(Billing.explain(model, Billing.estimateTokens(joined), 0));

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("\n未检测到 OPENAI_API_KEY，进入教学解析（不发网络请求）。");
            parseUsage(sampleResponse(), "gpt-4o-mini");
            return;
        }
        System.out.println("\n检测到 Key，发起真实请求...\n");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }
        parseUsage(MAPPER.readTree(response.body()), model);
    }

    static ObjectNode payload(String question, String model) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        ArrayNode messages = body.putArray("messages");
        messages.add(message("system", "你是技术导师。用中文，先结论后理由，三句以内。"));
        messages.add(message("user", question));
        return body;
    }

    static void parseUsage(JsonNode raw, String model) {
        String text = raw.path("choices").path(0).path("message").path("content").asText();
        JsonNode usage = raw.path("usage");
        int prompt = usage.path("prompt_tokens").asInt(usage.path("input_tokens").asInt(0));
        int completion = usage.path("completion_tokens").asInt(usage.path("output_tokens").asInt(0));
        System.out.println("\n模型回复: " + text);
        System.out.println(Billing.explain(model, prompt, completion));
        System.out.println("finish_reason: " + raw.path("choices").path(0).path("finish_reason").asText());
    }

    private static JsonNode sampleResponse() throws Exception {
        return MAPPER.readTree("""
                {
                  "id": "chatcmpl-teaching",
                  "model": "gpt-4o-mini",
                  "choices": [{"index": 0, "finish_reason": "stop",
                    "message": {"role": "assistant", "content": "结论：API 卖的是 Token 与延迟，不是一个永远在线的同事。"}}],
                  "usage": {"prompt_tokens": 42, "completion_tokens": 28, "total_tokens": 70}
                }
                """);
    }

    private static ObjectNode message(String role, String content) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
