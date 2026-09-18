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

/**
 * 第 1 章示例 2 的 Java 版：直接用 JDK HttpClient 调用 OpenAI 兼容接口。
 *
 * 环境变量与 Python 版相同：OPENAI_API_KEY、OPENAI_BASE_URL、OPENAI_MODEL。
 *
 * 运行：
 *   mvn -q -DskipTests compile
 *   java -cp "target/classes;%USERPROFILE%\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.17.2\\jackson-databind-2.17.2.jar;..." com.agentbook.ch01.LlmHello
 *
 * 更省事的方式见正文中的 Maven 命令，或先跑 RuleAgent（无需 Key）。
 */
public class LlmHello {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String question = "用三句话解释：AI Agent 和 Chatbot 的核心区别是什么？";
        System.out.println("用户: " + question + "\n");
        System.out.println("模型: " + ask(question));
    }

    static String ask(String question) throws Exception {
        String apiKey = envOrThrow("OPENAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");

        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        ArrayNode messages = root.putArray("messages");
        messages.add(message("system", "你是一名耐心的技术导师。用中文回答，先给结论，再给理由。"));
        messages.add(message("user", question));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(baseUrl) + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(root)))
                .build();

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode content = MAPPER.readTree(response.body())
                .path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("模型没有返回文本内容: " + response.body());
        }
        return content.asText();
    }

    private static ObjectNode message(String role, String content) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private static String envOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量 " + key + "。可先运行 RuleAgent。");
        }
        return value;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
