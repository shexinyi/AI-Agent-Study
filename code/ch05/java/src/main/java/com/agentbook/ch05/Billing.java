package com.agentbook.ch05;

import java.util.Map;

public final class Billing {
    public record Price(String name, double inputPerMillion, double outputPerMillion) {
    }

    private static final Map<String, Price> PRICES = Map.of(
            "gpt-4o-mini", new Price("gpt-4o-mini", 0.15, 0.60),
            "deepseek-chat", new Price("deepseek-chat", 0.14, 0.28),
            "qwen-plus", new Price("qwen-plus", 0.11, 0.28),
            "claude-sonnet-4-5", new Price("claude-sonnet-4-5", 3.00, 15.00)
    );

    private Billing() {
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int ascii = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < 128) {
                ascii++;
            }
        }
        int other = text.length() - ascii;
        return Math.max(1, (int) (ascii / 4.0 + other / 1.6));
    }

    public static String explain(String model, int inputTokens, int outputTokens) {
        Price price = PRICES.getOrDefault(model, new Price(model, 1.00, 3.00));
        boolean known = PRICES.containsKey(model);
        double usd = (inputTokens * price.inputPerMillion() + outputTokens * price.outputPerMillion()) / 1_000_000.0;
        String tag = known ? "教学示意价" : "未收录模型，按 1/3 USD per 1M 示意";
        return "模型=" + model + " 输入=" + inputTokens + " 输出=" + outputTokens
                + " 合计=" + (inputTokens + outputTokens) + " Token，约 "
                + String.format("%.6f", usd) + " USD（" + tag + "）";
    }
}
