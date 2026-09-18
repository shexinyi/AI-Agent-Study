package com.agentbook.ch16;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EvalKit {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GoldFile {
        public int k = 3;
        public List<GoldCase> cases = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GoldCase {
        public String id;
        public String query;
        public List<String> gold_headings = new ArrayList<>();
        public List<String> forbidden_headings = new ArrayList<>();
        public List<String> must_contain = new ArrayList<>();
        public List<String> must_not_contain = new ArrayList<>();
        public String role = "user";
    }

    public static final class Row {
        public final String id;
        public final List<String> headings;
        public final double hit1;
        public final double hitK;
        public final double mrr;
        public final boolean leak;
        public final boolean correct;
        public final boolean faithful;
        public final String answer;

        Row(String id, List<String> headings, double hit1, double hitK, double mrr,
            boolean leak, boolean correct, boolean faithful, String answer) {
            this.id = id;
            this.headings = headings;
            this.hit1 = hit1;
            this.hitK = hitK;
            this.mrr = mrr;
            this.leak = leak;
            this.correct = correct;
            this.faithful = faithful;
            this.answer = answer;
        }

        public boolean pass() {
            return hit1 == 1 && correct && faithful && !leak;
        }
    }

    private EvalKit() {
    }

    public static GoldFile load(Path path) {
        try {
            return MAPPER.readValue(path.toFile(), GoldFile.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Row eval(GoldCase c, List<RagKit.Chunk> chunks, String pipeline, int k,
                           String extra, boolean dropDeprecated, boolean applyAcl) {
        List<RagKit.Chunk> pool = RagKit.visible(chunks, c.role, dropDeprecated, applyAcl);
        List<RagKit.Hit> hits = RagKit.retrieve(pool, c.query, pipeline, k);
        List<String> headings = hits.stream().map(h -> h.chunk.heading).toList();
        String answer = RagKit.extractive(hits, extra);
        return new Row(c.id, headings, hitAt(hits, c.gold_headings, 1), hitAt(hits, c.gold_headings, k),
                mrr(hits, c.gold_headings), leaked(hits, c.forbidden_headings),
                correct(answer, c.must_contain, c.must_not_contain), faithful(answer, hits), answer);
    }

    public static List<Row> suite(GoldFile gold, List<RagKit.Chunk> chunks, String pipeline,
                                  String extra, boolean dropDeprecated, boolean applyAcl) {
        List<Row> rows = new ArrayList<>();
        for (GoldCase c : gold.cases) {
            rows.add(eval(c, chunks, pipeline, gold.k, extra, dropDeprecated, applyAcl));
        }
        return rows;
    }

    public static void dump(String title, List<Row> rows) {
        System.out.println("\n=== " + title + " ===");
        System.out.printf("%-12s %-5s %-5s %-6s %-4s %-4s %-4s  Top%n",
                "id", "H@1", "H@3", "MRR", "正确", "忠实", "泄漏");
        for (Row r : rows) {
            System.out.printf("%-12s %-5.0f %-5.0f %-6.2f %-4s %-4s %-4s  %s%n",
                    r.id, r.hit1, r.hitK, r.mrr, r.correct ? "Y" : "N", r.faithful ? "Y" : "N",
                    r.leak ? "是" : "否", r.headings);
        }
        System.out.printf("%-12s %-5.2f %-5.2f %-6.2f %-4.2f %-4.2f%n",
                "MACRO", avg(rows, "hit1"), avg(rows, "hitK"), avg(rows, "mrr"),
                avg(rows, "correct"), avg(rows, "faithful"));
    }

    static double hitAt(List<RagKit.Hit> hits, List<String> gold, int k) {
        Set<String> top = new HashSet<>();
        for (int i = 0; i < Math.min(k, hits.size()); i++) {
            top.add(hits.get(i).chunk.heading);
        }
        for (String g : gold) {
            if (top.contains(g)) {
                return 1;
            }
        }
        return 0;
    }

    static double mrr(List<RagKit.Hit> hits, List<String> gold) {
        Set<String> set = new HashSet<>(gold);
        for (int i = 0; i < hits.size(); i++) {
            if (set.contains(hits.get(i).chunk.heading)) {
                return 1.0 / (i + 1);
            }
        }
        return 0;
    }

    static boolean leaked(List<RagKit.Hit> hits, List<String> forbidden) {
        Set<String> set = new HashSet<>(forbidden);
        return hits.stream().anyMatch(h -> set.contains(h.chunk.heading));
    }

    static boolean correct(String answer, List<String> must, List<String> mustNot) {
        for (String p : must) {
            if (!answer.contains(p)) {
                return false;
            }
        }
        for (String p : mustNot) {
            if (answer.contains(p)) {
                return false;
            }
        }
        return true;
    }

    static boolean faithful(String answer, List<RagKit.Hit> hits) {
        if (answer.startsWith("【不足】")) {
            return true;
        }
        String corpus = hits.stream().map(h -> h.chunk.text).reduce("", (a, b) -> a + "\n" + b);
        for (String sent : answer.replace("。", "。\n").split("\n")) {
            String s = sent.strip();
            if (s.endsWith("。")) {
                s = s.substring(0, s.length() - 1);
            }
            if (!s.isEmpty() && !corpus.contains(s)) {
                return false;
            }
        }
        return true;
    }

    static double avg(List<Row> rows, String key) {
        double s = 0;
        for (Row r : rows) {
            s += switch (key) {
                case "hit1" -> r.hit1;
                case "hitK" -> r.hitK;
                case "mrr" -> r.mrr;
                case "correct" -> r.correct ? 1 : 0;
                case "faithful" -> r.faithful ? 1 : 0;
                default -> 0;
            };
        }
        return s / Math.max(rows.size(), 1);
    }
}
