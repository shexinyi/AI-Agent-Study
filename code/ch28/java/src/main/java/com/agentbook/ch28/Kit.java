package com.agentbook.ch28;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Kit {
    public static final String LEAVE = "事假每年累计不超过 10 个工作日。";
    public static final String ORDER88 = "ORDER-88 未发货，可整单退款。写退款须人工确认。";
    public static final String CAFE = "【不足】食堂菜单不在客服工具里，不能编。";
    public static final String FAT_PROMPT = "你是全能客服。" + "请结合全部历史与制度全文详细分析。".repeat(8);

    private static final Map<String, String> CACHE = new LinkedHashMap<>();

    private Kit() {
    }

    public static String route(String question) {
        if (question.contains("食堂")) {
            return "cafe";
        }
        String u = question.toUpperCase(Locale.ROOT);
        if (u.contains("ORDER") || u.contains("TZ123") || question.contains("退")) {
            return "order";
        }
        if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
            return "leave";
        }
        return "other";
    }

    public static MiniBill.Run fat(String question) {
        String intent = route(question);
        String text = switch (intent) {
            case "leave" -> LEAVE;
            case "order" -> ORDER88;
            case "cafe" -> "食堂今天有红烧肉。";
            default -> "好的。";
        };
        MiniBill.Run run = new MiniBill.Run(text, "large");
        MiniBill.addRetrieve(run, 3);
        MiniBill.addLlm(run, "large", FAT_PROMPT + question, text, 2);
        run.tools.addAll(List.of("search_policy", "get_order", "search_policy"));
        run.latency = new MiniBill.Latency(50, 240, 90, 40, 290);
        return run;
    }

    public static MiniBill.Run allLarge(String question) {
        String intent = route(question);
        String text = switch (intent) {
            case "leave" -> LEAVE;
            case "order" -> ORDER88;
            case "cafe" -> CAFE;
            default -> "【不足】不能编。";
        };
        MiniBill.Run run = new MiniBill.Run(text, "large");
        MiniBill.addLlm(run, "large", question, text, 0);
        if (!"cafe".equals(intent)) {
            MiniBill.addRetrieve(run, 1);
            run.tools.add("leave".equals(intent) ? "search_policy" : "get_order");
        }
        run.latency = new MiniBill.Latency(40, 90, run.tools.isEmpty() ? 0 : 20, 12, 130);
        return run;
    }

    public static MiniBill.Run lean(String question, boolean useCache) {
        String intent = route(question);
        MiniBill.Run run = new MiniBill.Run("", "small");
        if (useCache && "leave".equals(intent) && CACHE.containsKey(question)) {
            run.text = CACHE.get(question);
            run.cached = true;
            run.model = "cache";
            run.latency = new MiniBill.Latency(5, 0, 0, 2, 5);
            return run;
        }
        MiniBill.addLlm(run, "small", question, intent, 0);
        run.latency = new MiniBill.Latency(20, 15, 0, 8, 28);
        if ("cafe".equals(intent)) {
            run.text = CAFE;
            return run;
        }
        if ("leave".equals(intent)) {
            MiniBill.addRetrieve(run, 1);
            run.tools.add("search_policy");
            run.text = LEAVE;
            run.latency.toolMs = 12;
            run.latency.netMs += 5;
            if (useCache) {
                CACHE.put(question, run.text);
            }
            return run;
        }
        if ("order".equals(intent)) {
            MiniBill.addRetrieve(run, 1);
            MiniBill.addLlm(run, "large", question + "\n查订单", ORDER88, 0);
            run.model = "large";
            run.tools.add("get_order");
            run.text = ORDER88;
            run.latency.modelMs += 80;
            run.latency.toolMs = 25;
            run.latency.netMs += 10;
            run.latency.ttftMs = run.latency.queueMs + 20;
            return run;
        }
        run.text = "【不足】不能编。";
        return run;
    }

    public static MiniBill.Run batchLeave(List<String> questions) {
        MiniBill.Run run = new MiniBill.Run(LEAVE, "small");
        MiniBill.addLlm(run, "small", String.join("\n", questions), LEAVE, 0);
        MiniBill.addRetrieve(run, 1);
        run.tools.add("search_policy");
        run.latency = new MiniBill.Latency(20, 18, 12, 8, 28);
        return run;
    }

    public static void resetCache() {
        CACHE.clear();
    }

    public static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }
}
