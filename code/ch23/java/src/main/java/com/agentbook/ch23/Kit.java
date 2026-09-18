package com.agentbook.ch23;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 林秋世界。工人是脚本，不是 GPT。 */
public final class Kit {
    public static final String LEAVE = "事假每年累计不超过 10 个工作日。";
    public static final Map<String, String> ORDERS = Map.of(
            "ORDER-88", "ORDER-88 未发货，可整单退款。写退款须人工确认。",
            "ORDER-99", "ORDER-99 已发货，物流 TZ123，须走售后。");

    private Kit() {
    }

    public static String oid(String text) {
        String q = text.toUpperCase(Locale.ROOT);
        if (q.contains("TZ123") || q.contains("ORDER-99")) {
            return "ORDER-99";
        }
        if (q.contains("ORDER-88")) {
            return "ORDER-88";
        }
        return "";
    }

    /** 单 Agent 天花板：后写的 if 覆盖前面的意图。 */
    public static String soloAnswer(String goal) {
        String text = "【不足】听不清要办哪一件。";
        if (goal.contains("事假") || goal.contains("请假") || goal.contains("工作日")) {
            text = LEAVE;
        }
        String upper = goal.toUpperCase(Locale.ROOT);
        if (upper.contains("ORDER") || upper.contains("TZ123") || goal.contains("退")) {
            String id = oid(goal);
            text = ORDERS.getOrDefault(id, "【无资料】没有能对上的单号。");
        }
        if (goal.contains("食堂")) {
            text = "食堂今天有红烧肉。";
        }
        return text;
    }

    public static MiniOrch.Worker leaveWorker() {
        return new MiniOrch.Worker("事假工人", "leave", task -> {
            if (task.query.contains("事假") || task.query.contains("请假") || task.query.contains("工作日")) {
                if (task.query.contains("2026-09-14") && task.query.contains("3")) {
                    return LEAVE + " 林秋 2026-09-14..16 共 3 天，未超上限，可请。";
                }
                return LEAVE;
            }
            return "【无资料】不是事假问题。";
        });
    }

    public static MiniOrch.Worker orderWorker() {
        return new MiniOrch.Worker("订单工人", "order", task -> {
            String id = oid(task.query);
            if (!id.isEmpty()) {
                return ORDERS.get(id);
            }
            return "【无资料】没有单号。";
        });
    }

    public static MiniOrch.Worker cafeWorker(boolean explode) {
        return new MiniOrch.Worker("食堂工人", "cafe", task -> {
            if (explode) {
                throw new RuntimeException("TimeoutError: 食堂菜单服务超时 3000ms");
            }
            return "【无资料】食堂菜单不在客服工具里，不能编。";
        });
    }

    public static MiniOrch.Worker unknownWorker() {
        return new MiniOrch.Worker("兜底工人", "unknown", task -> "【不足】编排器拆不出对口任务，不能编。");
    }

    public static MiniOrch.Orchestrator makeOrch(String policy, boolean cafeExplode) {
        return new MiniOrch.Orchestrator(
                List.of(leaveWorker(), orderWorker(), cafeWorker(cafeExplode), unknownWorker()),
                policy);
    }

    public static boolean expectOk(String report, String tag) {
        return switch (tag) {
            case "has_leave" -> report.contains("10 个工作日");
            case "has_88" -> report.contains("ORDER-88") && report.contains("未发货");
            case "has_99" -> report.contains("ORDER-99") || report.contains("TZ123");
            case "no_pork" -> !report.contains("红烧肉");
            case "isolated" -> report.contains("【失败隔离】");
            case "all_fail" -> report.startsWith("【整单失败】");
            case "no_leave" -> !report.contains("10 个工作日");
            default -> false;
        };
    }

    public static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }
}
