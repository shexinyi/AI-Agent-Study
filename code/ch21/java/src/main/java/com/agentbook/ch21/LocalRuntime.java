package com.agentbook.ch21;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LocalRuntime {
    private LocalRuntime() {
    }

    public static String getOrder(String orderId) {
        if ("ORDER-88".equals(orderId)) {
            return "ORDER-88 未发货，可整单退款。写退款须人工确认。";
        }
        if ("ORDER-99".equals(orderId)) {
            return "ORDER-99 已发货，物流 TZ123，须走售后。";
        }
        return orderId + " 查无此单。";
    }

    public static Map<String, String> answer(String question) {
        Map<String, String> session = new LinkedHashMap<>();
        session.put("question", question);
        if (question.contains("食堂") || question.contains("年假")) {
            session.put("answer", "【不足】资料不够。");
            return session;
        }
        if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
            session.put("answer", "根据制度，事假每年累计不超过 10 个工作日。");
            return session;
        }
        String oid = (question.toUpperCase().contains("TZ123") || question.toUpperCase().contains("ORDER-99"))
                ? "ORDER-99" : "ORDER-88";
        if (question.toUpperCase().contains("ORDER") || question.toUpperCase().contains("TZ") || question.contains("退")) {
            String tool = getOrder(oid);
            session.put("tool", tool);
            session.put("answer", tool.contains("未发货")
                    ? "根据工具，ORDER-88 未发货可退。写工具前须人工确认。"
                    : "根据工具，TZ123 对应 ORDER-99，已发货须走售后。");
            return session;
        }
        session.put("answer", "【不足】资料不够。");
        return session;
    }
}
