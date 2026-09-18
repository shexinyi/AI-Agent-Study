package com.agentbook.ch18;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Kit {
    private Kit() {
    }

    public static String routeOf(String question) {
        String q = question.toUpperCase();
        if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
            return "leave";
        }
        if (q.contains("ORDER-") || q.contains("TZ") || question.contains("退")) {
            return "order";
        }
        return "unknown";
    }

    public static String orderIdOf(String question) {
        String q = question.toUpperCase();
        if (q.contains("TZ123") || q.contains("ORDER-99")) {
            return "ORDER-99";
        }
        return "ORDER-88";
    }

    public static Map<String, Object> getOrder(String orderId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("order_id", orderId);
        if ("ORDER-88".equals(orderId)) {
            row.put("status", "unshipped");
            row.put("refundable", true);
            return row;
        }
        if ("ORDER-99".equals(orderId)) {
            row.put("status", "shipped");
            row.put("refundable", false);
            row.put("tracking", "TZ123");
            return row;
        }
        row.put("error", "not_found");
        return row;
    }

    public static String leaveFact() {
        return "事假每年累计不超过 10 个工作日。";
    }

    public static String draftFromFacts(String facts) {
        if (facts.contains("10 个工作日")) {
            return "根据资料，事假每年累计不超过 10 个工作日。";
        }
        if (facts.contains("unshipped") || facts.contains("refundable=true")) {
            return "根据工具，该单未发货，可申请整单退款。";
        }
        if (facts.contains("shipped")) {
            return "根据工具，该单已发货，对话里不能直接退款，须走售后。";
        }
        return "【不足】资料不够，不能编天数或单号状态。";
    }

    public static String modelOnce(int attempt) {
        if (attempt == 0) {
            return "当然可以退，三天到账。";
        }
        return "{\"tool\":\"get_order\",\"order_id\":\"ORDER-88\"}";
    }
}
