package com.agentbook.ch14;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChunkKit {
    public static final int DIM = 64;

    public static final class Chunk {
        public final String id;
        public final String text;
        public final double[] embedding;
        public final Map<String, String> meta = new LinkedHashMap<>();

        Chunk(String id, String text, double[] embedding) {
            this.id = id;
            this.text = text;
            this.embedding = embedding;
        }

        public String preview(int n) {
            String one = text.replaceAll("\\s+", " ").strip();
            return one.length() <= n ? one : one.substring(0, n) + "…";
        }
    }

    public static final class Hit {
        public final double score;
        public final Chunk chunk;

        Hit(double score, Chunk chunk) {
            this.score = score;
            this.chunk = chunk;
        }
    }

    private ChunkKit() {
    }

    public static String readKb() {
        try {
            return Files.readString(Path.of("kb", "messy_leave.md"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String clean(String raw) {
        raw = raw.replaceAll("(?s)```.*?```", "");
        List<String> kept = new ArrayList<>();
        for (String line : raw.split("\n", -1)) {
            String t = line.strip();
            if (t.matches(".*(机密|作废|Page \\d|打印于|内部作废).*")) {
                continue;
            }
            if (t.matches("=+")) {
                continue;
            }
            kept.add(line.stripTrailing());
        }
        return String.join("\n", kept).replaceAll("\n{3,}", "\n\n").strip();
    }

    public static double[] embed(String text, boolean asciiOnly) {
        double[] vec = new double[DIM];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String tok : tokens(text, asciiOnly)) {
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

    public static List<Chunk> whole(String text) {
        return List.of(make("whole-1", text, "whole", "document"));
    }

    public static List<Chunk> byHeading(String text) {
        List<Chunk> out = new ArrayList<>();
        String heading = "doc";
        List<String> buf = new ArrayList<>();
        int n = 0;
        for (String line : text.split("\n", -1)) {
            if (line.startsWith("## ")) {
                n += flushHeading(out, heading, buf, n);
                heading = line.substring(3).strip();
                buf = new ArrayList<>();
            } else if (!line.startsWith("# ")) {
                buf.add(line);
            }
        }
        flushHeading(out, heading, buf, n);
        return out;
    }

    public static List<Chunk> window(String text, int size, int overlap) {
        if (overlap >= size) {
            throw new IllegalArgumentException("overlap 必须小于 size");
        }
        String compact = text.replaceAll("\n+", "\n").strip();
        List<Chunk> out = new ArrayList<>();
        int step = size - overlap;
        int n = 0;
        for (int i = 0; i < compact.length(); i += step) {
            int end = Math.min(compact.length(), i + size);
            String piece = compact.substring(i, end);
            if (!piece.isBlank()) {
                n++;
                out.add(make("win-" + n, piece, "window", i + ":" + end));
            }
            if (end == compact.length()) {
                break;
            }
        }
        return out;
    }

    public static List<Chunk> semantic(String text, int target) {
        String[] sentences = text.split("(?<=[。！？\n])");
        List<Chunk> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int n = 0;
        for (String sent : sentences) {
            String s = sent.strip();
            if (s.isEmpty()) {
                continue;
            }
            if (buf.length() > 0 && buf.length() + s.length() > target) {
                n++;
                out.add(make("sem-" + n, buf.toString(), "semantic", "sentence-merge"));
                buf = new StringBuilder(s);
            } else {
                buf.append(s);
            }
        }
        if (!buf.toString().isBlank()) {
            n++;
            out.add(make("sem-" + n, buf.toString(), "semantic", "sentence-merge"));
        }
        return out;
    }

    public static List<Hit> retrieve(List<Chunk> chunks, String query, int topK, boolean asciiOnly) {
        double[] q = embed(query, asciiOnly);
        return chunks.stream()
                .map(c -> new Hit(dot(q, c.embedding), c))
                .sorted(Comparator.comparingDouble((Hit h) -> h.score).reversed())
                .limit(topK)
                .toList();
    }

    public static List<Hit> retrieveFiltered(List<Chunk> chunks, String query, String key, String value) {
        List<Chunk> allowed = chunks.stream().filter(c -> value.equals(c.meta.get(key))).toList();
        return retrieve(allowed, query, 1, false);
    }

    static Chunk make(String id, String text, String strategy, String heading) {
        Chunk c = new Chunk(id, text, embed(text, false));
        c.meta.put("source", "messy_leave.md");
        c.meta.put("strategy", strategy);
        c.meta.put("heading", heading);
        c.meta.put("acl", "hr");
        c.meta.put("version", "2026-03");
        c.meta.put("kind", "policy");
        return c;
    }

    private static int flushHeading(List<Chunk> out, String heading, List<String> buf, int n) {
        String body = String.join("\n", buf).strip();
        if (body.isEmpty()) {
            return 0;
        }
        out.add(make("head-" + (n + 1), heading + "。" + body, "heading", heading));
        return 1;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    static List<String> tokens(String text, boolean asciiOnly) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("[a-z0-9]+").matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            out.add(m.group());
        }
        if (asciiOnly) {
            return out;
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
