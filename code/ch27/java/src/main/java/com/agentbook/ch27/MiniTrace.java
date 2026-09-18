package com.agentbook.ch27;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** 可观测教具。Token 按字符数计。不接 OpenTelemetry SDK。 */
public final class MiniTrace {
    public static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    public static final Pattern ID18 = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");
    public static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    public static final Pattern BANK = Pattern.compile("6222\\d{12,15}");

    public static final class Span {
        public final String spanId;
        public final String parentId;
        public final String name;
        public final String kind;
        public String status = "unset";
        public int startMs;
        public int endMs;
        public final Map<String, Object> attrs = new LinkedHashMap<>();

        public Span(String spanId, String parentId, String name, String kind, int startMs) {
            this.spanId = spanId;
            this.parentId = parentId;
            this.name = name;
            this.kind = kind;
            this.startMs = startMs;
        }

        public int latencyMs() {
            return Math.max(0, endMs - startMs);
        }
    }

    public static final class Trace {
        public final String traceId;
        public final String userId;
        public final String question;
        public final List<Span> spans = new ArrayList<>();

        public Trace(String traceId, String userId, String question) {
            this.traceId = traceId;
            this.userId = userId;
            this.question = question;
        }

        public int[] tokens() {
            int inn = 0;
            int out = 0;
            for (Span s : spans) {
                inn += ((Number) s.attrs.getOrDefault("tokens_in", 0)).intValue();
                out += ((Number) s.attrs.getOrDefault("tokens_out", 0)).intValue();
            }
            return new int[]{inn, out};
        }

        public String tree() {
            Map<String, List<Span>> byParent = new LinkedHashMap<>();
            for (Span span : spans) {
                byParent.computeIfAbsent(span.parentId, k -> new ArrayList<>()).add(span);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("trace ").append(traceId).append(" user=").append(userId)
                    .append(" q=").append(question);
            walk(byParent, "", 0, sb);
            return sb.toString();
        }

        private void walk(Map<String, List<Span>> byParent, String parent, int indent, StringBuilder sb) {
            for (Span span : byParent.getOrDefault(parent, List.of())) {
                sb.append('\n');
                sb.append("  ".repeat(indent)).append("- [").append(span.kind).append("] ")
                        .append(span.name).append(' ').append(span.status).append(' ')
                        .append(span.latencyMs()).append("ms");
                Object inn = span.attrs.getOrDefault("tokens_in", 0);
                Object out = span.attrs.getOrDefault("tokens_out", 0);
                if (!inn.equals(0) || !out.equals(0)) {
                    sb.append(" tok=").append(inn).append('/').append(out);
                }
                Object err = span.attrs.get("error");
                if (err != null && !err.toString().isEmpty()) {
                    sb.append(" err=").append(err);
                }
                walk(byParent, span.spanId, indent + 1, sb);
            }
        }
    }

    public static final class Clock {
        public int t;

        public int advance(int ms) {
            t += ms;
            return t;
        }
    }

    public static final class Tracer {
        public final Clock clock = new Clock();
        public Trace trace;
        private int n = 0;

        public Trace begin(String traceId, String userId, String question) {
            trace = new Trace(traceId, userId, question);
            n = 0;
            return trace;
        }

        public Span open(String name, String kind, String parentId) {
            n++;
            Span span = new Span("s" + n, parentId, name, kind, clock.t);
            trace.spans.add(span);
            return span;
        }

        public void close(Span span, String status, Map<String, Object> attrs) {
            span.status = status;
            int took = ((Number) attrs.getOrDefault("took", 10)).intValue();
            span.endMs = clock.advance(took);
            for (Map.Entry<String, Object> e : attrs.entrySet()) {
                if (!"took".equals(e.getKey())) {
                    span.attrs.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    public static String redact(String text) {
        text = PHONE.matcher(text).replaceAll("[PHONE]");
        text = BANK.matcher(text).replaceAll("[BANK]");
        text = ID18.matcher(text).replaceAll("[ID]");
        text = EMAIL.matcher(text).replaceAll("[EMAIL]");
        return text;
    }

    public static List<String> leak(String text) {
        List<String> found = new ArrayList<>();
        if (PHONE.matcher(text).find()) {
            found.add("phone");
        }
        if (ID18.matcher(text).find()) {
            found.add("id");
        }
        if (EMAIL.matcher(text).find()) {
            found.add("email");
        }
        if (BANK.matcher(text).find()) {
            found.add("bank");
        }
        return found;
    }

    public static List<String> otelLines(Trace trace) {
        List<String> rows = new ArrayList<>();
        for (Span span : trace.spans) {
            rows.add("{"
                    + "\"trace_id\":\"" + trace.traceId + "\","
                    + "\"span_id\":\"" + span.spanId + "\","
                    + "\"parent_span_id\":" + (span.parentId.isEmpty() ? "null" : "\"" + span.parentId + "\"") + ","
                    + "\"name\":\"" + span.name + "\","
                    + "\"kind\":\"" + span.kind + "\","
                    + "\"status\":\"" + span.status + "\","
                    + "\"latency_ms\":" + span.latencyMs() + ","
                    + "\"tokens_in\":" + span.attrs.getOrDefault("tokens_in", 0) + ","
                    + "\"tokens_out\":" + span.attrs.getOrDefault("tokens_out", 0) + ","
                    + "\"error\":\"" + span.attrs.getOrDefault("error", "") + "\"}");
        }
        return rows;
    }

    public static String[] locate(Trace trace) {
        for (Span span : trace.spans) {
            if ("error".equals(span.status) && !"agent".equals(span.kind)) {
                return new String[]{"TOOL", span.name + " error=" + span.attrs.get("error")};
            }
            Object dep = span.attrs.get("deprecated");
            if ("retrieve".equals(span.kind) && (Boolean.TRUE.equals(dep) || "true".equals(String.valueOf(dep)))) {
                return new String[]{"RETRIEVE", span.name + " deprecated 命中废止稿"};
            }
        }
        StringBuilder llm = new StringBuilder();
        boolean refunded = false;
        for (Span span : trace.spans) {
            if ("llm".equals(span.kind)) {
                llm.append(span.attrs.getOrDefault("output", "")).append(' ');
            }
            if ("tool".equals(span.kind) && "refund_order".equals(span.name) && "ok".equals(span.status)) {
                refunded = true;
            }
        }
        String text = llm.toString();
        if ((text.contains("已办理") || text.contains("三天到账")) && !refunded) {
            return new String[]{"MODEL_LIE", "llm 说已办理，但没有成功的 refund_order span"};
        }
        return new String[]{"OK", "树上没有红灯，输出能对上工具"};
    }
}
