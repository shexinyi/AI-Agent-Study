package com.agentbook.ch32;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 项目 3 服务内核。教学假设：不拉端口；管道名对齐 FastAPI / Spring Filter。
 * Token 按字符数；教学币不是官网价。可退 ≠ 已退。
 */
public final class Service {
    public static final Map<String, Double> RATES = Map.of(
            "large_in", 3.0,
            "large_out", 15.0,
            "small_in", 0.2,
            "small_out", 0.8,
            "retrieve", 1.0);

    public static final String FAT_SYS =
            "请结合全部历史、制度全文、去年会议纪要与全部检索结果后再回答。\n".repeat(12);

    public static final String[] PIPELINE = {
            "request_id", "auth_bearer", "quota_daily", "handler", "audit_redact", "bill_span"
    };

    private static final Pattern PHONE = Pattern.compile("1\\d{10}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern ID = Pattern.compile("\\b\\d{17}[\\dXx]\\b");

    public static final class Ident {
        public final String role;
        public final int dailyQuota;
        public final String label;

        Ident(String role, int dailyQuota, String label) {
            this.role = role;
            this.dailyQuota = dailyQuota;
            this.label = label;
        }
    }

    public static final Map<String, Ident> TOKENS = Map.of(
            "intern-lin", new Ident("intern", 3, "intern"),
            "ops-key", new Ident("agent_ops", 20, "ops"));

    public static int tokens(String text) {
        return Math.max(1, text.length());
    }

    public static double fee(String kind, int n) {
        if ("retrieve".equals(kind)) {
            return RATES.get("retrieve") * n;
        }
        return RATES.get(kind) * n / 1000.0;
    }

    public static String redact(String text) {
        String t = PHONE.matcher(text).replaceAll("[PHONE]");
        t = EMAIL.matcher(t).replaceAll("[EMAIL]");
        return ID.matcher(t).replaceAll("[ID]");
    }

    public static String actorHash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d).substring(0, 8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String bearer(Map<String, String> headers) {
        String raw = headers.getOrDefault("Authorization", headers.getOrDefault("authorization", ""));
        if (raw.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return raw.substring(7).strip();
        }
        return raw.strip();
    }

    public static Map<String, Object> handleBiz(String question, String role, String confirm) {
        List<String> executed = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
            parts.add("事假每年累计不超过 10 个工作日。林秋 2026-09-14..16 共 3 天，未超上限。");
        }
        String u = question.toUpperCase(Locale.ROOT);
        if (u.contains("ORDER-99") || u.contains("TZ123")) {
            parts.add("ORDER-99 已发货，物流 TZ123，须走售后。");
        } else if (u.contains("ORDER-88") || question.contains("退")) {
            if (u.contains("ORDER-88")) {
                parts.add("ORDER-88 未发货，可整单退款。写退款须人工确认。");
                if (question.contains("退")) {
                    if (!"agent_ops".equals(role)) {
                        parts.add("【拒绝】intern 不在 refund_order 白名单。");
                    } else if ("approve".equals(confirm)) {
                        executed.add("refund_order:ORDER-88");
                        parts.add("已执行 refund_order(ORDER-88)。");
                    } else if ("reject".equals(confirm)) {
                        parts.add("【已拒绝】不执行写工具。");
                    } else {
                        parts.add("审批单 pending，等待确认。");
                    }
                }
            } else {
                parts.add("【澄清】请提供订单号。");
            }
        }
        if (question.contains("食堂")) {
            parts.add("【不足】食堂不在本服务范围。");
        }
        if (parts.isEmpty()) {
            parts.add("【不足】不是本服务范围内的办理。");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("answer", String.join(" ", parts));
        out.put("executed", executed);
        return out;
    }

    public static Map<String, Object> makeBill(String mode, String question, String answer) {
        List<Map<String, Object>> lines = new ArrayList<>();
        class Add {
            void llm(String model, String prompt, String output, int retry) {
                int inn = tokens(prompt);
                int out = tokens(output);
                line("input", model, inn, fee(model + "_in", inn));
                line("output", model, out, fee(model + "_out", out));
                for (int i = 0; i < retry; i++) {
                    line("retry", model, inn, fee(model + "_in", inn));
                    line("retry", model, out, fee(model + "_out", out));
                }
            }

            void retrieve(int n) {
                line("retrieve", "-", n, fee("retrieve", n));
            }

            void line(String bucket, String model, int tok, double amount) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("bucket", bucket);
                row.put("model", model);
                row.put("tokens", tok);
                row.put("amount", amount);
                lines.add(row);
            }
        }
        Add add = new Add();
        if ("fat".equals(mode)) {
            add.llm("large", FAT_SYS + question, answer, 2);
            add.retrieve(3);
        } else {
            String route = question.contains("事假") ? "leave" : (question.contains("食堂") ? "cafe" : "order");
            String clipped = question.length() <= 24 ? question : question.substring(0, 24);
            add.llm("small", "路由:" + clipped, route, 0);
            if (!"cafe".equals(route)) {
                add.retrieve(1);
                add.llm("large", "短上下文\n" + question, answer, 0);
            }
        }
        double amount = 0;
        int retrieve = 0;
        int retryPairs = 0;
        for (Map<String, Object> x : lines) {
            amount += (Double) x.get("amount");
            if ("retrieve".equals(x.get("bucket"))) {
                retrieve += (Integer) x.get("tokens");
            }
            if ("retry".equals(x.get("bucket"))) {
                retryPairs++;
            }
        }
        Map<String, Object> bill = new LinkedHashMap<>();
        bill.put("amount", Math.round(amount * 10000.0) / 10000.0);
        bill.put("retrieve", retrieve);
        bill.put("retry", retryPairs / 2);
        bill.put("lines", lines);
        return bill;
    }

    public static String scriptAsk(String question) {
        Map<String, Object> biz = handleBiz(question, "intern", null);
        return "[script] " + biz.get("answer") + " raw=" + question;
    }

    public static final class Response {
        public final int status;
        public final Map<String, Object> body;
        public final Map<String, Object> audit;

        Response(int status, Map<String, Object> body, Map<String, Object> audit) {
            this.status = status;
            this.body = body;
            this.audit = audit;
        }
    }

    public static final class AgentService {
        public final Map<String, Integer> used = new LinkedHashMap<>();
        public final List<Map<String, Object>> audits = new ArrayList<>();
        public int n = 0;

        public Response handle(Map<String, String> headers, String question) {
            return handle(headers, question, null, "lean");
        }

        public Response handle(Map<String, String> headers, String question, String confirm, String mode) {
            n++;
            String traceId = String.format("tr-%04d", n);
            String token = bearer(headers);
            Ident ident = TOKENS.get(token);
            if (ident == null) {
                Map<String, Object> audit = audit(traceId, token.isEmpty() ? "anon" : token, 401, question, 0.0, "unauthorized");
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "unauthorized");
                body.put("trace_id", traceId);
                return new Response(401, body, audit);
            }
            int already = used.getOrDefault(token, 0);
            if (already >= ident.dailyQuota) {
                Map<String, Object> audit = audit(traceId, token, 429, question, 0.0, "quota_exceeded");
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "quota_exceeded");
                body.put("trace_id", traceId);
                body.put("quota_used", already);
                body.put("quota_limit", ident.dailyQuota);
                return new Response(429, body, audit);
            }
            used.put(token, already + 1);
            Map<String, Object> biz = handleBiz(question, ident.role, confirm);
            @SuppressWarnings("unchecked")
            List<String> executed = (List<String>) biz.get("executed");
            String answer = (String) biz.get("answer");
            Map<String, Object> bill = makeBill(mode, question, answer);
            Map<String, Object> billView = new LinkedHashMap<>();
            billView.put("amount", bill.get("amount"));
            billView.put("retrieve", bill.get("retrieve"));
            billView.put("retry", bill.get("retry"));
            List<Map<String, String>> spans = new ArrayList<>();
            spans.add(Map.of("name", "agent", "kind", "agent"));
            spans.add(Map.of("name", "auth", "kind", "auth", "status", "ok"));
            spans.add(Map.of("name", "quota", "kind", "quota", "status", "ok"));
            spans.add(Map.of("name", ((Integer) bill.get("retrieve")) > 0 ? "retrieve" : "route", "kind", "tool"));
            spans.add(Map.of("name", "llm", "kind", "llm"));
            spans.add(Map.of("name", "hitl", "kind", "hitl", "status", answer.contains("pending") ? "pending" : "n/a"));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("trace_id", traceId);
            body.put("answer", answer);
            body.put("executed", executed);
            body.put("role", ident.role);
            body.put("quota_used", used.get(token));
            body.put("quota_limit", ident.dailyQuota);
            body.put("bill", billView);
            body.put("spans", spans);
            Map<String, Object> audit = audit(traceId, token, 200, question, (Double) bill.get("amount"), "ok");
            return new Response(200, body, audit);
        }

        private Map<String, Object> audit(String traceId, String token, int status, String question, double amount, String action) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("trace_id", traceId);
            rec.put("actor", actorHash(token));
            rec.put("status", status);
            rec.put("action", action);
            rec.put("question", redact(question));
            rec.put("amount", amount);
            audits.add(rec);
            return rec;
        }
    }
}
