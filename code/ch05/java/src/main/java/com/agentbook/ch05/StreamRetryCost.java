package com.agentbook.ch05;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class StreamRetryCost {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<Integer> RETRYABLE = Set.of(408, 409, 429, 500, 502, 503, 504);
    private static final List<String> TEACHING_SSE = List.of(
            "data: {\"choices\":[{\"delta\":{\"content\":\"先\"}}]}",
            "data: {\"choices\":[{\"delta\":{\"content\":\"出字\"}}]}",
            "data: {\"choices\":[{\"delta\":{\"content\":\"，再\"}}]}",
            "data: {\"choices\":[{\"delta\":{\"content\":\"算账。\"}}]}",
            "data: [DONE]"
    );

    public static void main(String[] args) throws Exception {
        System.out.println("可重试状态码: " + RETRYABLE);
        System.out.println("不可重试: 400 参数错误、401 鉴权失败、403 权限不足。重试它们只会重复交学费。\n");

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            String text = playSse(TEACHING_SSE);
            System.out.println(Billing.explain("gpt-4o-mini",
                    Billing.estimateTokens("流式输出有什么好处"),
                    Billing.estimateTokens(text)));
            return;
        }
        String text = streamLive(apiKey, "流式输出对客服产品的用户体验有什么好处？", 3);
        System.out.println(Billing.explain(envOr("OPENAI_MODEL", "gpt-4o-mini"),
                Billing.estimateTokens("流式输出对客服产品的用户体验有什么好处？"),
                Billing.estimateTokens(text)));
    }

    static boolean retryable(int statusCode) {
        return RETRYABLE.contains(statusCode);
    }

    static String streamLive(String apiKey, String question, int attempts) throws Exception {
        String model = envOr("OPENAI_MODEL", "gpt-4o-mini");
        String url = trimSlash(envOr("OPENAI_BASE_URL", "https://api.openai.com/v1")) + "/chat/completions";
        ObjectNode body = OpenAICompatible.payload(question, model);
        body.put("stream", true);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        Exception last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            System.out.println("=== 第 " + attempt + " 次流式请求 ===");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                last = new IllegalStateException("HTTP " + response.statusCode());
                if (!retryable(response.statusCode()) || attempt == attempts) {
                    throw last;
                }
                Thread.sleep(400L * attempt);
                continue;
            }
            return readSse(response);
        }
        throw new IllegalStateException("重试耗尽: " + last);
    }

    static String readSse(HttpResponse<java.io.InputStream> response) throws Exception {
        StringBuilder chunks = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).strip();
                if ("[DONE]".equals(data)) {
                    System.out.println();
                    break;
                }
                JsonNode payload = MAPPER.readTree(data);
                String piece = payload.path("choices").path(0).path("delta").path("content").asText("");
                chunks.append(piece);
                System.out.print(piece);
            }
        }
        return chunks.toString();
    }

    static String playSse(List<String> lines) throws Exception {
        System.out.println("=== 教学 SSE 回放 ===");
        StringBuilder chunks = new StringBuilder();
        for (String line : lines) {
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring(5).strip();
            if ("[DONE]".equals(data)) {
                System.out.println("\n[DONE]");
                break;
            }
            JsonNode payload = MAPPER.readTree(data);
            String piece = payload.path("choices").path(0).path("delta").path("content").asText("");
            chunks.append(piece);
            System.out.print(piece);
        }
        return chunks.toString();
    }

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
