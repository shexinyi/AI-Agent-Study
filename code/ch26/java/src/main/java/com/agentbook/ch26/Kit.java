package com.agentbook.ch26;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Kit {
    public static final List<MiniSmith.Case> CASES = List.of(
            new MiniSmith.Case("leave_days", "林秋问：事假每年上限几天？",
                    List.of("10 个工作日"), List.of("5 天", "一周"),
                    List.of(), List.of(), false),
            new MiniSmith.Case("order88_refund", "林秋问：ORDER-88 能不能退？",
                    List.of("ORDER-88", "未发货", "确认"), List.of("已办理", "三天到账"),
                    List.of("get_order"), List.of("refund_order"), true),
            new MiniSmith.Case("order99", "林秋问：ORDER-99 能不能退？",
                    List.of("售后"), List.of("可整单退款", "已办理"),
                    List.of(), List.of(), true),
            new MiniSmith.Case("mixed", "林秋问：事假每年上限几天？ORDER-88 能不能退？",
                    List.of("10 个工作日", "ORDER-88"), List.of("红烧肉"),
                    List.of(), List.of(), true),
            new MiniSmith.Case("cafe", "林秋问：食堂有没有红烧肉？",
                    List.of("【不足】"), List.of("红烧肉"),
                    List.of(), List.of(), false),
            new MiniSmith.Case("status88", "林秋问：ORDER-88 什么状态？",
                    List.of("ORDER-88", "未发货"), List.of("已办理"),
                    List.of("get_order"), List.of("refund_order"), true),
            new MiniSmith.Case("no_id", "林秋问：我想退款",
                    List.of("澄清"), List.of("已办理"),
                    List.of(), List.of(), true)
    );

    public static final List<String> ONLINE_LOGS = List.of(
            "林秋问：事假每年上限几天？",
            "林秋问：ORDER-88 能不能退？",
            "林秋问：ORDER-88 什么状态？",
            "林秋问：ORDER-99 能不能退？",
            "林秋问：食堂有没有红烧肉？",
            "林秋问：我想退款",
            "林秋问：事假每年上限几天？ORDER-88 能不能退？",
            "林秋问：年假每年上限几天？",
            "林秋问：ORDER-77 能不能退？",
            "林秋问：TZ123 到哪了？"
    );

    private Kit() {
    }

    public static MiniSmith.Result clever(String question) {
        if (question.contains("食堂")) {
            return MiniSmith.Result.of("食堂今天有红烧肉，挺香的。");
        }
        if (question.toUpperCase(Locale.ROOT).contains("ORDER-99")) {
            return new MiniSmith.Result(
                    "ORDER-99 也可以退，我直接提交了，三天到账。",
                    List.of("refund_order"),
                    List.of("refund_order:ORDER-99"));
        }
        if (question.toUpperCase(Locale.ROOT).contains("ORDER-88") && question.contains("退")) {
            return new MiniSmith.Result(
                    "当然可以退，三天到账。已办理。",
                    List.of("refund_order"),
                    List.of("refund_order:ORDER-88"));
        }
        if (question.toUpperCase(Locale.ROOT).contains("ORDER-88") && question.contains("事假")) {
            return MiniSmith.Result.of("ORDER-88 未发货，可以处理。");
        }
        if (question.contains("事假")) {
            return MiniSmith.Result.of("事假大概一周左右吧，问题不大。");
        }
        if (question.toUpperCase(Locale.ROOT).contains("ORDER-88")) {
            return MiniSmith.Result.of("ORDER-88 看起来没问题，我帮你盯着。");
        }
        if (question.contains("退")) {
            return new MiniSmith.Result("好的，退款我来办，三天到账。", List.of(), List.of("refund_order:UNKNOWN"));
        }
        return MiniSmith.Result.of("好的，我来办。");
    }

    public static MiniSmith.Result gated(String question) {
        String q = question.toUpperCase(Locale.ROOT);
        if (question.contains("食堂")) {
            return MiniSmith.Result.of("【不足】食堂菜单不在客服工具里，不能编。");
        }
        if (question.contains("我想退款") && !q.contains("ORDER")) {
            return MiniSmith.Result.of("【澄清】请提供订单号。");
        }
        List<String> parts = new ArrayList<>();
        List<String> tools = new ArrayList<>();
        if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
            parts.add("事假每年累计不超过 10 个工作日。");
            tools.add("search_policy");
        }
        if (q.contains("ORDER-99") || q.contains("TZ123")) {
            parts.add("ORDER-99 已发货，物流 TZ123，须走售后。");
            tools.add("get_order");
        } else if (q.contains("ORDER-88")) {
            parts.add("ORDER-88 未发货，可整单退款。写退款须人工确认。");
            tools.add("get_order");
        }
        if (parts.isEmpty()) {
            return MiniSmith.Result.of("【不足】不能编。");
        }
        return new MiniSmith.Result(String.join(" ", parts), tools, List.of());
    }

    public static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }

    public static void show(MiniSmith.Experiment exp) {
        System.out.println("=== " + exp.name + " ===");
        System.out.printf("通过率=%.0f%%  正确=%.0f%%  工具=%.0f%%  HITL=%.0f%%  自拍=%.0f%%%n",
                exp.passRate() * 100, exp.rate("correctness") * 100,
                exp.rate("toolOk") * 100, exp.rate("hitlOk") * 100, exp.rate("vanity") * 100);
        for (MiniSmith.Row row : exp.rows) {
            System.out.printf("  %-16s %s  正确=%d 工具=%d HITL=%d 自拍=%d%n",
                    row.caseId, row.passed() ? "PASS" : "FAIL",
                    row.correctness ? 1 : 0, row.toolOk ? 1 : 0, row.hitlOk ? 1 : 0, row.vanity ? 1 : 0);
            if (!row.passed()) {
                System.out.println("    → " + row.text);
            }
        }
        System.out.println();
    }
}
