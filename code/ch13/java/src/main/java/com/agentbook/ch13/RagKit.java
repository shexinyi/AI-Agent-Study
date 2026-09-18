package com.agentbook.ch13;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RagKit {
    public static final int DIM = 64;

    public static final class Chunk {
        public final String chunkId;
        public final String source;
        public final String heading;
        public final String text;
        public final double[] embedding;

        public Chunk(String chunkId, String source, String heading, String text, double[] embedding) {
            this.chunkId = chunkId;
            this.source = source;
            this.heading = heading;
            this.text = text;
            this.embedding = embedding;
        }
    }

    public static final class Hit {
        public final double score;
        public final Chunk chunk;

        public Hit(double score, Chunk chunk) {
            this.score = score;
            this.chunk = chunk;
        }
    }

    private RagKit() {
    }

    public static double[] embed(String text) {
        double[] vec = new double[DIM];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String tok : tokens(text)) {
                byte[] digest = md.digest(tok.getBytes(StandardCharsets.UTF_8));
                int idx = ((digest[0] & 0xff) | ((digest[1] & 0xff) << 8)) % DIM;
                double sign = ((digest[2] & 0xff) % 2 == 0) ? 1.0 : -1.0;
                vec[idx] += sign;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        double norm = 0;
        for (double x : vec) {
            norm += x * x;
        }
        norm = Math.sqrt(norm == 0 ? 1 : norm);
        for (int i = 0; i < vec.length; i++) {
            vec[i] /= norm;
        }
        return vec;
    }

    public static double cosine(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    public static List<Chunk> buildIndex(Path kbDir) {
        List<Chunk> chunks = new ArrayList<>();
        try (Stream<Path> walk = Files.list(kbDir)) {
            walk.filter(p -> p.getFileName().toString().endsWith(".md")).sorted().forEach(p -> chunks.addAll(chunkMarkdown(p)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return chunks;
    }

    public static List<Chunk> chunkMarkdown(Path path) {
        List<Chunk> chunks = new ArrayList<>();
        String heading = path.getFileName().toString().replace(".md", "");
        List<String> buf = new ArrayList<>();
        int n = 0;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.startsWith("## ")) {
                    n += flushChunk(chunks, path, heading, buf, n);
                    heading = line.substring(3).strip();
                    buf = new ArrayList<>();
                } else if (!line.startsWith("# ")) {
                    buf.add(line);
                }
            }
            flushChunk(chunks, path, heading, buf, n);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return chunks;
    }

    private static int flushChunk(List<Chunk> chunks, Path path, String heading, List<String> buf, int n) {
        String body = String.join("\n", buf).strip();
        if (body.isEmpty()) {
            return 0;
        }
        String text = heading + "。" + body;
        chunks.add(new Chunk(path.getFileName().toString().replace(".md", "") + "-" + (n + 1),
                path.getFileName().toString(), heading, text, embed(text)));
        return 1;
    }

    public static List<Hit> retrieve(List<Chunk> chunks, String query, int topK) {
        double[] q = embed(query);
        return chunks.stream()
                .map(c -> new Hit(cosine(q, c.embedding), c))
                .sorted(Comparator.comparingDouble((Hit h) -> h.score).reversed())
                .limit(topK)
                .toList();
    }

    public static int overlap(String query, String text) {
        java.util.Set<Character> q = new java.util.HashSet<>();
        java.util.Set<Character> t = new java.util.HashSet<>();
        query.chars().forEach(c -> {
            if (c >= 0x4e00 && c <= 0x9fff) {
                q.add((char) c);
            }
        });
        text.chars().forEach(c -> {
            if (c >= 0x4e00 && c <= 0x9fff) {
                t.add((char) c);
            }
        });
        q.retainAll(t);
        return q.size();
    }

    public static String generateExtractive(String question, List<Hit> hits, int minOverlap) {
        List<Hit> usable = hits.stream().filter(h -> overlap(question, h.chunk.text) >= minOverlap).toList();
        if (usable.isEmpty()) {
            return "【不足】检索结果里没有能支撑的句子，不能编造。";
        }
        StringBuilder sb = new StringBuilder("【依据资料】\n");
        for (Hit h : usable) {
            sb.append(String.format("- (%s / %s, sim=%.3f) %s%n",
                    h.chunk.source, h.chunk.heading, h.score, h.chunk.text));
        }
        sb.append("【回答】请只根据上面的制度原文作答，不要补充制度里没有的数字。\n");
        sb.append("结论摘录：").append(usable.get(0).chunk.text);
        return sb.toString();
    }

    public static String generateClosedBook(String question) {
        return "闭卷回答（无资料）：" + question + " → 按惯例事假最多 5 天，当天申请即可。";
    }

    public static void saveIndex(List<Chunk> chunks, Path path) {
        try {
            Files.createDirectories(path.getParent());
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode arr = mapper.createArrayNode();
            for (Chunk c : chunks) {
                ObjectNode n = mapper.createObjectNode();
                n.put("chunkId", c.chunkId);
                n.put("source", c.source);
                n.put("heading", c.heading);
                n.put("text", c.text);
                n.set("embedding", mapper.valueToTree(c.embedding));
                arr.add(n);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), arr);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<Chunk> loadIndex(Path path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(path.toFile());
            List<Chunk> chunks = new ArrayList<>();
            for (JsonNode n : arr) {
                chunks.add(new Chunk(
                        n.get("chunkId").asText(),
                        n.get("source").asText(),
                        n.get("heading").asText(),
                        n.get("text").asText(),
                        mapper.convertValue(n.get("embedding"), double[].class)
                ));
            }
            return chunks;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("[a-z0-9]+").matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            out.add(m.group());
        }
        List<String> chars = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 0x4e00 && ch <= 0x9fff) {
                chars.add(String.valueOf(ch));
            }
        }
        out.addAll(chars);
        for (int i = 0; i < chars.size() - 1; i++) {
            out.add(chars.get(i) + chars.get(i + 1));
        }
        return out;
    }
}
