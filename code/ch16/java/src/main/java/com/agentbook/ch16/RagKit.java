package com.agentbook.ch16;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RagKit {
    public static final int DIM = 64;
    private static final Pattern META = Pattern.compile("<!--\\s*meta:\\s*(.+?)\\s*-->");

    public static final class Chunk {
        public final String id;
        public final String source;
        public final String heading;
        public final String text;
        public final String version;
        public final String acl;
        public final String kind;
        public final double[] embedding;
        public final List<String> terms;

        Chunk(String id, String source, String heading, String text, Map<String, String> meta) {
            this.id = id;
            this.source = source;
            this.heading = heading;
            this.text = text;
            this.version = meta.getOrDefault("version", "unknown");
            this.acl = meta.getOrDefault("acl", "all");
            this.kind = meta.getOrDefault("kind", "current");
            this.terms = tokens(text);
            this.embedding = embed(text, true);
        }
    }

    public static final class Hit {
        public final Chunk chunk;
        public final double score;
        public final String channel;
        public final int rank;

        public Hit(Chunk chunk, double score, String channel, int rank) {
            this.chunk = chunk;
            this.score = score;
            this.channel = channel;
            this.rank = rank;
        }
    }

    private RagKit() {
    }

    public static List<Chunk> buildCorpus(Path kbDir, String mode) {
        List<Path> files;
        try (Stream<Path> walk = Files.list(kbDir)) {
            files = walk.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if ("coarse".equals(mode)) {
            String blob = files.stream().map(RagKit::read).collect(Collectors.joining("\n\n"));
            return List.of(make("ALL.md", "整库糊成一块", blob, parseMeta(blob), 1));
        }
        List<Chunk> chunks = new ArrayList<>();
        for (Path path : files) {
            String raw = read(path);
            Map<String, String> meta = parseMeta(raw);
            if ("whole".equals(mode)) {
                String heading = path.getFileName().toString().replace(".md", "");
                for (String line : raw.split("\n")) {
                    if (line.startsWith("# ")) {
                        heading = line.substring(2).strip();
                        break;
                    }
                }
                chunks.add(make(path.getFileName().toString(), heading, META.matcher(raw).replaceAll(""), meta, 1));
                continue;
            }
            String heading = path.getFileName().toString().replace(".md", "");
            List<String> buf = new ArrayList<>();
            int n = 0;
            for (String line : raw.split("\n", -1)) {
                if (line.startsWith("## ")) {
                    n += flush(chunks, path, heading, buf, meta, n);
                    heading = line.substring(3).strip();
                    buf = new ArrayList<>();
                } else if (!line.startsWith("# ")) {
                    buf.add(line);
                }
            }
            flush(chunks, path, heading, buf, meta, n);
        }
        return chunks;
    }

    public static List<Chunk> visible(List<Chunk> chunks, String role, boolean dropDeprecated, boolean applyAcl) {
        Set<String> allowed = switch (role) {
            case "hr" -> Set.of("all", "hr");
            default -> Set.of("all");
        };
        List<Chunk> out = new ArrayList<>();
        for (Chunk c : chunks) {
            if (dropDeprecated && "deprecated".equals(c.kind)) {
                continue;
            }
            if (applyAcl && !allowed.contains(c.acl)) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    public static List<Hit> retrieve(List<Chunk> chunks, String query, String pipeline, int topK) {
        if ("vector".equals(pipeline)) {
            return vectorSearch(chunks, query, topK);
        }
        List<Hit> fused = rrf(List.of(vectorSearch(chunks, query, 5), bm25Search(chunks, query, 5)), 60, 5);
        if ("hybrid".equals(pipeline)) {
            return fused.subList(0, Math.min(topK, fused.size()));
        }
        List<Hit> ranked = rerank(query, fused);
        return ranked.subList(0, Math.min(topK, ranked.size()));
    }

    public static String extractive(List<Hit> hits, String extra) {
        if (hits.isEmpty()) {
            return "【不足】没有可引用的块。";
        }
        return hits.get(0).chunk.text + extra;
    }

    static List<Hit> vectorSearch(List<Chunk> chunks, String query, int topK) {
        double[] q = embed(query, true);
        List<Hit> ranked = chunks.stream()
                .map(c -> new Hit(c, dot(q, c.embedding), "vector", 0))
                .sorted(Comparator.comparingDouble((Hit h) -> h.score).reversed())
                .limit(topK)
                .toList();
        return number(ranked);
    }

    static List<Hit> bm25Search(List<Chunk> chunks, String query, int topK) {
        List<String> qTerms = tokens(query);
        Map<String, Integer> df = new HashMap<>();
        for (Chunk c : chunks) {
            for (String t : new HashSet<>(c.terms)) {
                df.merge(t, 1, Integer::sum);
            }
        }
        int nDocs = Math.max(chunks.size(), 1);
        double avgdl = chunks.stream().mapToInt(c -> c.terms.size()).average().orElse(1);
        Map<String, Integer> qtf = new HashMap<>();
        qTerms.forEach(t -> qtf.merge(t, 1, Integer::sum));
        List<Hit> scored = new ArrayList<>();
        for (Chunk c : chunks) {
            Map<String, Integer> tf = new HashMap<>();
            c.terms.forEach(t -> tf.merge(t, 1, Integer::sum));
            double score = 0;
            int dl = Math.max(1, c.terms.size());
            for (Map.Entry<String, Integer> e : qtf.entrySet()) {
                int f = tf.getOrDefault(e.getKey(), 0);
                if (f == 0) {
                    continue;
                }
                double idf = Math.log((nDocs - df.getOrDefault(e.getKey(), 0) + 0.5)
                        / (df.getOrDefault(e.getKey(), 0) + 0.5) + 1.0);
                double denom = f + 1.5 * (1 - 0.75 + 0.75 * dl / avgdl);
                score += e.getValue() * idf * (f * 2.5 / denom);
            }
            if (score > 0) {
                scored.add(new Hit(c, score, "bm25", 0));
            }
        }
        scored.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed());
        return number(scored.subList(0, Math.min(topK, scored.size())));
    }

    static List<Hit> rrf(List<List<Hit>> lists, int k, int topK) {
        record Acc(double score, Chunk chunk, List<String> chans) {
        }
        Map<String, Acc> bag = new LinkedHashMap<>();
        for (List<Hit> hits : lists) {
            for (Hit hit : number(hits)) {
                double add = 1.0 / (k + hit.rank);
                Acc old = bag.get(hit.chunk.id);
                if (old == null) {
                    List<String> chans = new ArrayList<>();
                    chans.add(hit.channel);
                    bag.put(hit.chunk.id, new Acc(add, hit.chunk, chans));
                } else {
                    old.chans.add(hit.channel);
                    bag.put(hit.chunk.id, new Acc(old.score + add, old.chunk, old.chans));
                }
            }
        }
        List<Hit> fused = bag.values().stream()
                .sorted(Comparator.comparingDouble((Acc a) -> a.score).reversed())
                .limit(topK)
                .map(a -> new Hit(a.chunk, a.score, String.join("+", a.chans.stream().distinct().toList()), 0))
                .toList();
        return number(fused);
    }

    static List<Hit> rerank(String query, List<Hit> hits) {
        Matcher m = Pattern.compile("ORDER-\\d+|TZ\\d+").matcher(query.toUpperCase(Locale.ROOT));
        List<String> ids = new ArrayList<>();
        while (m.find()) {
            ids.add(m.group());
        }
        Set<Character> qCjk = cjk(query);
        List<Hit> scored = new ArrayList<>();
        for (Hit hit : hits) {
            double s = 0;
            String upper = hit.chunk.text.toUpperCase(Locale.ROOT);
            for (String id : ids) {
                if (upper.contains(id)) {
                    s += 3;
                }
                if (hit.chunk.heading.toUpperCase(Locale.ROOT).contains(id)) {
                    s += 1;
                }
            }
            s += 0.15 * intersect(qCjk, cjk(hit.chunk.text));
            if (hit.chunk.text.contains("88 折") || hit.chunk.text.contains("会议室")) {
                s -= 2.5;
            }
            scored.add(new Hit(hit.chunk, s, "rerank", 0));
        }
        scored.sort(Comparator.comparingDouble((Hit h) -> h.score).reversed());
        return number(scored);
    }

    private static int flush(List<Chunk> chunks, Path path, String heading, List<String> buf,
                             Map<String, String> meta, int n) {
        String body = buf.stream().filter(ln -> !ln.startsWith("<!--")).collect(Collectors.joining("\n")).strip();
        if (body.isEmpty()) {
            return 0;
        }
        chunks.add(make(path.getFileName().toString(), heading, body, meta, n + 1));
        return 1;
    }

    private static Chunk make(String source, String heading, String body, Map<String, String> meta, int n) {
        String stem = source.replace(".md", "");
        return new Chunk(stem + "-" + n, source, heading, heading + "。" + body.strip(), meta);
    }

    static Map<String, String> parseMeta(String raw) {
        Map<String, String> meta = new HashMap<>();
        meta.put("version", "unknown");
        meta.put("acl", "all");
        meta.put("kind", "current");
        Matcher m = META.matcher(raw);
        if (m.find()) {
            for (String part : m.group(1).split("\\s+")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    meta.put(part.substring(0, eq), part.substring(eq + 1));
                }
            }
        }
        return meta;
    }

    static List<Hit> number(List<Hit> hits) {
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            Hit h = hits.get(i);
            out.add(new Hit(h.chunk, h.score, h.channel, i + 1));
        }
        return out;
    }

    static double[] embed(String text, boolean dropIds) {
        double[] vec = new double[DIM];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String tok : tokens(text)) {
                if (dropIds && tok.matches("(order|tz)?\\d+")) {
                    continue;
                }
                byte[] digest = md.digest(tok.getBytes(StandardCharsets.UTF_8));
                int idx = ((digest[0] & 0xff) | ((digest[1] & 0xff) << 8)) % DIM;
                vec[idx] += ((digest[2] & 0xff) % 2 == 0) ? 1.0 : -1.0;
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

    static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    static Set<Character> cjk(String text) {
        Set<Character> s = new HashSet<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 0x4e00 && ch <= 0x9fff) {
                s.add(ch);
            }
        }
        return s;
    }

    static int intersect(Set<Character> a, Set<Character> b) {
        int n = 0;
        for (Character ch : a) {
            if (b.contains(ch)) {
                n++;
            }
        }
        return n;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
