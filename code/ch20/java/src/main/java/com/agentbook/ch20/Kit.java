package com.agentbook.ch20;

import java.util.Map;

public final class Kit {
    static final String LEAVE = "事假每年累计不超过 10 个工作日。";

    private Kit() {
    }

    static String oid(String q) {
        String u = q.toUpperCase();
        if (u.contains("TZ123") || u.contains("ORDER-99")) {
            return "ORDER-99";
        }
        if (u.contains("ORDER-88")) {
            return "ORDER-88";
        }
        return "";
    }

    public static MiniCrew.Agent researcher(String kind) {
        String role = "leave".equals(kind) ? "事假研究员" : "订单研究员";
        return new MiniCrew.Agent(role, (desc, blob) -> {
            String q = String.valueOf(blob.get("question"));
            if ("leave".equals(kind)) {
                if (q.contains("事假") || q.contains("请假") || q.contains("工作日")) {
                    return LEAVE;
                }
                return "【无资料】不是事假问题。";
            }
            String id = oid(q);
            if ("ORDER-88".equals(id)) {
                return "ORDER-88 未发货，可整单退款。写退款须人工确认。";
            }
            if ("ORDER-99".equals(id)) {
                return "ORDER-99 已发货，物流 TZ123，须走售后。";
            }
            return "【无资料】没有单号。";
        });
    }

    public static MiniCrew.Agent writer() {
        return new MiniCrew.Agent("写手", (desc, blob) -> {
            @SuppressWarnings("unchecked")
            Map<String, String> ctxMap = (Map<String, String>) blob.get("context");
            String ctx = String.join(" ", ctxMap.values());
            String q = String.valueOf(blob.get("question"));
            if (ctx.contains("10 个工作日") && (q.contains("事假") || q.contains("请假") || q.contains("工作日") || q.contains("几天"))) {
                return "根据资料，事假每年累计不超过 10 个工作日。";
            }
            if (ctx.contains("ORDER-88") && ctx.contains("未发货")) {
                return "根据工具，ORDER-88 未发货可退，写工具前须确认。";
            }
            if (ctx.contains("TZ123") || ctx.contains("ORDER-99")) {
                return "根据工具，TZ123 对应 ORDER-99，已发货须走售后。";
            }
            if (ctx.contains("【无资料】") || q.contains("食堂") || q.contains("年假")) {
                return "【不足】资料不够，不能编。";
            }
            return "【不足】写手看不见能用的上下文。";
        });
    }

    public static MiniCrew.Agent manager() {
        return new MiniCrew.Agent("经理", (desc, blob) -> {
            String q = desc;
            boolean orderish = q.toUpperCase().contains("ORDER") || q.toUpperCase().contains("TZ") || q.contains("退");
            if ((q.contains("事假") || q.contains("请假") || q.contains("工作日") || q.contains("年假") || q.contains("食堂") || q.contains("几天"))
                    && !orderish) {
                return (q.contains("年假") || q.contains("食堂")) ? "写手" : "事假研究员";
            }
            if (orderish) {
                return "订单研究员";
            }
            return "写手";
        });
    }

    public static boolean expectedOk(String output, String expected) {
        return switch (expected) {
            case "must_10" -> output.contains("10 个工作日") && !output.contains("5 天");
            case "must_88" -> output.contains("ORDER-88") && output.contains("未发货");
            case "must_99" -> output.contains("ORDER-99") || output.contains("TZ123");
            case "must_stop" -> output.startsWith("【不足】");
            default -> true;
        };
    }
}
