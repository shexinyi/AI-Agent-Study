package com.agentbook.ch17;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Kit {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Map<String, Object>> ORDERS = Map.of(
            "ORDER-88", Map.of("status", "unshipped", "refundable", true),
            "ORDER-99", Map.of("status", "shipped", "refundable", false, "tracking", "TZ123")
    );

    private Kit() {
    }

    public static String retrieve(String question, Path kbDir) {
        List<String[]> chunks = new ArrayList<>();
        try (var walk = Files.list(kbDir)) {
            walk.filter(p -> p.toString().endsWith(".md")).sorted().forEach(p -> chunks.addAll(chunkFile(p)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        String q = question.toUpperCase(Locale.ROOT);
        List<int[]> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String text = chunks.get(i)[0] + "。" + chunks.get(i)[1];
            int score = 0;
            if (q.contains("ORDER-88") && text.toUpperCase(Locale.ROOT).contains("ORDER-88")) {
                score += 5;
            }
            if ((q.contains("TZ123") || q.contains("ORDER-99")) && text.toUpperCase(Locale.ROOT).contains("TZ123")) {
                score += 5;
            }
            if ((question.contains("事假") || question.contains("请假") || question.contains("工作日"))
                    && text.contains("事假")) {
                score += 4;
            }
            if (question.contains("退") && text.contains("退")) {
                score += 1;
            }
            if (score > 0) {
                scored.add(new int[]{score, i});
            }
        }
        scored.sort(Comparator.comparingInt((int[] a) -> a[0]).reversed());
        if (scored.isEmpty()) {
            return "";
        }
        String[] hit = chunks.get(scored.get(0)[1]);
        return hit[0] + "。" + hit[1];
    }

    public static String getOrder(String orderId) {
        Map<String, Object> row = ORDERS.get(orderId.toUpperCase(Locale.ROOT));
        ObjectNode node = MAPPER.createObjectNode();
        if (row == null) {
            node.put("error", "not_found");
            node.put("order_id", orderId);
            return node.toString();
        }
        node.put("order_id", orderId.toUpperCase(Locale.ROOT));
        node.put("status", String.valueOf(row.get("status")));
        node.put("refundable", Boolean.TRUE.equals(row.get("refundable")));
        if (row.containsKey("tracking")) {
            node.put("tracking", String.valueOf(row.get("tracking")));
        }
        return node.toString();
    }

    public static String runTool(String json) {
        try {
            JsonNode n = MAPPER.readTree(json);
            if (!"get_order".equals(n.path("tool").asText())) {
                throw new IllegalArgumentException("未知工具: " + json);
            }
            return getOrder(n.path("order_id").asText());
        } catch (IOException e) {
            throw new IllegalArgumentException("不是 JSON: " + json, e);
        }
    }

    public static boolean isJsonObject(String text) {
        try {
            return MAPPER.readTree(text).isObject();
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String[]> chunkFile(Path path) {
        List<String[]> out = new ArrayList<>();
        String heading = path.getFileName().toString().replace(".md", "");
        List<String> buf = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.startsWith("## ")) {
                    flush(out, heading, buf);
                    heading = line.substring(3).strip();
                    buf = new ArrayList<>();
                } else if (!line.startsWith("# ")) {
                    buf.add(line);
                }
            }
            flush(out, heading, buf);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    private static void flush(List<String[]> out, String heading, List<String> buf) {
        String body = String.join("\n", buf).strip();
        if (!body.isEmpty()) {
            out.add(new String[]{heading, body});
        }
    }
}
