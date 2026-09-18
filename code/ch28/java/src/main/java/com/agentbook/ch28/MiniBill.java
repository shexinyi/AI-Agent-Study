package com.agentbook.ch28;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 成本教具。Token 按字符数；单价是教学币。 */
public final class MiniBill {
    public static final Map<String, Double> RATES = Map.of(
            "large_in", 3.0,
            "large_out", 15.0,
            "small_in", 0.2,
            "small_out", 0.8,
            "retrieve", 1.0
    );

    public static int tokens(String text) {
        return Math.max(1, text.length());
    }

    public static double fee(String kind, int n) {
        if ("retrieve".equals(kind)) {
            return RATES.get("retrieve") * n;
        }
        return RATES.get(kind) * n / 1000.0;
    }

    public static final class Line {
        public final String bucket;
        public final String model;
        public final int tokens;
        public final double amount;
        public final String note;

        public Line(String bucket, String model, int tokens, double amount, String note) {
            this.bucket = bucket;
            this.model = model;
            this.tokens = tokens;
            this.amount = amount;
            this.note = note;
        }
    }

    public static final class Latency {
        public int queueMs;
        public int modelMs;
        public int toolMs;
        public int netMs;
        public int ttftMs;

        public Latency(int queueMs, int modelMs, int toolMs, int netMs, int ttftMs) {
            this.queueMs = queueMs;
            this.modelMs = modelMs;
            this.toolMs = toolMs;
            this.netMs = netMs;
            this.ttftMs = ttftMs;
        }

        public int totalMs() {
            return queueMs + modelMs + toolMs + netMs;
        }
    }

    public static final class Run {
        public String text;
        public String model;
        public boolean cached;
        public final List<Line> lines = new ArrayList<>();
        public Latency latency = new Latency(0, 0, 0, 0, 0);
        public final List<String> tools = new ArrayList<>();

        public Run(String text, String model) {
            this.text = text;
            this.model = model;
        }

        public double amount() {
            double n = 0;
            for (Line line : lines) {
                n += line.amount;
            }
            return Math.round(n * 10000.0) / 10000.0;
        }
    }

    public static void addLlm(Run run, String model, String prompt, String output, int retry) {
        int inn = tokens(prompt);
        int out = tokens(output);
        run.lines.add(new Line("input", model, inn, fee(model + "_in", inn), "prompt"));
        run.lines.add(new Line("output", model, out, fee(model + "_out", out), "completion"));
        for (int i = 0; i < retry; i++) {
            run.lines.add(new Line("retry", model, inn, fee(model + "_in", inn), "retry#" + (i + 1) + " in"));
            run.lines.add(new Line("retry", model, out, fee(model + "_out", out), "retry#" + (i + 1) + " out"));
        }
    }

    public static void addRetrieve(Run run, int n) {
        run.lines.add(new Line("retrieve", "-", n, fee("retrieve", n), n + " 次检索"));
    }

    public static Map<String, Double> summarize(List<Run> runs) {
        Map<String, Double> buckets = new LinkedHashMap<>();
        buckets.put("input", 0.0);
        buckets.put("output", 0.0);
        buckets.put("retry", 0.0);
        buckets.put("retrieve", 0.0);
        for (Run run : runs) {
            for (Line line : run.lines) {
                buckets.put(line.bucket, buckets.get(line.bucket) + line.amount);
            }
        }
        buckets.replaceAll((k, v) -> Math.round(v * 10000.0) / 10000.0);
        return buckets;
    }
}
