package com.agentbook.ch27;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Kit {
    public static final String RAW_LOG = "user=林秋 phone=13800138000 id=110101200001011234 "
            + "email=linqiu@example.com bank=6222021234567890123 "
            + "问：ORDER-88 能不能退？";

    private Kit() {
    }

    public static int tokens(String text) {
        return Math.max(1, text.length());
    }

    public static MiniTrace.Tracer healthyRefundTrace() {
        MiniTrace.Tracer tr = new MiniTrace.Tracer();
        tr.begin("tr-88", "usr_linqiu", "林秋问：ORDER-88 能不能退？");
        MiniTrace.Span root = tr.open("agent", "agent", "");
        MiniTrace.Span ret = tr.open("search_policy", "retrieve", root.spanId);
        tr.close(ret, "ok", attrs("took", 12, "tokens_in", 8, "tokens_out", 0, "title", "现行事假"));
        MiniTrace.Span tool = tr.open("get_order", "tool", root.spanId);
        tr.close(tool, "ok", attrs("took", 25, "order_id", "ORDER-88", "shipped", false));
        MiniTrace.Span llm = tr.open("draft", "llm", root.spanId);
        String out = "ORDER-88 未发货，可整单退款。写退款须人工确认。";
        tr.close(llm, "ok", attrs("took", 80, "tokens_in", tokens("ORDER-88"),
                "tokens_out", tokens(out), "output", out));
        MiniTrace.Span hitl = tr.open("approve_refund", "hitl", root.spanId);
        tr.close(hitl, "ok", attrs("took", 5, "decision", "pending"));
        tr.close(root, "ok", attrs("took", 1));
        return tr;
    }

    public static MiniTrace.Tracer incidentToolTimeout() {
        MiniTrace.Tracer tr = new MiniTrace.Tracer();
        tr.begin("tr-timeout", "usr_linqiu", "林秋问：ORDER-88 帮我退。");
        MiniTrace.Span root = tr.open("agent", "agent", "");
        MiniTrace.Span tool = tr.open("get_order", "tool", root.spanId);
        tr.close(tool, "ok", attrs("took", 20, "order_id", "ORDER-88"));
        MiniTrace.Span refund = tr.open("refund_order", "tool", root.spanId);
        tr.close(refund, "error", attrs("took", 3000, "error", "TimeoutError: 3000ms", "order_id", "ORDER-88"));
        MiniTrace.Span llm = tr.open("apology", "llm", root.spanId);
        tr.close(llm, "ok", attrs("took", 30, "tokens_in", 10, "tokens_out", 8, "output", "系统出错，请稍后重试。"));
        tr.close(root, "error", attrs("took", 1));
        return tr;
    }

    public static MiniTrace.Tracer incidentModelLie() {
        MiniTrace.Tracer tr = new MiniTrace.Tracer();
        tr.begin("tr-lie", "usr_linqiu", "林秋问：ORDER-88 能不能退？");
        MiniTrace.Span root = tr.open("agent", "agent", "");
        MiniTrace.Span llm = tr.open("draft", "llm", root.spanId);
        String out = "当然可以退，三天到账。已办理。";
        tr.close(llm, "ok", attrs("took", 70, "tokens_in", 12, "tokens_out", tokens(out), "output", out));
        tr.close(root, "ok", attrs("took", 1));
        return tr;
    }

    public static MiniTrace.Tracer incidentStaleDoc() {
        MiniTrace.Tracer tr = new MiniTrace.Tracer();
        tr.begin("tr-stale", "usr_linqiu", "林秋问：事假每年上限几天？");
        MiniTrace.Span root = tr.open("agent", "agent", "");
        MiniTrace.Span ret = tr.open("search_policy", "retrieve", root.spanId);
        tr.close(ret, "ok", attrs("took", 15, "deprecated", true, "title", "废止稿 5 天"));
        MiniTrace.Span llm = tr.open("draft", "llm", root.spanId);
        String out = "根据资料，事假不超过 5 天。";
        tr.close(llm, "ok", attrs("took", 40, "tokens_in", 20, "tokens_out", tokens(out), "output", out));
        tr.close(root, "ok", attrs("took", 1));
        return tr;
    }

    public static Map<String, Object> attrs(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    public static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }

    public static boolean hasKind(MiniTrace.Trace trace, String kind) {
        return trace.spans.stream().anyMatch(s -> kind.equals(s.kind));
    }
}
