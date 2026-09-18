package com.agentbook.ch24;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Kit {
    public static final String LEAVE = "事假每年累计不超过 10 个工作日。";
    public static final Map<String, String> ORDERS = Map.of(
            "ORDER-88", "ORDER-88 未发货，可整单退款。写退款须人工确认。",
            "ORDER-99", "ORDER-99 已发货，物流 TZ123，须走售后。");

    public static final Map<String, List<String>> STRICT_ALLOW = Map.of(
            "前台", List.of("事假专家", "订单专家", "食堂专家"),
            "事假专家", List.of("前台"),
            "订单专家", List.of("前台"),
            "食堂专家", List.of("前台"));

    public static final Map<String, List<String>> OPEN_ALLOW = Map.of(
            "前台", List.of("事假专家", "订单专家", "食堂专家", "退款机器人"),
            "事假专家", List.of("前台", "订单专家", "食堂专家"),
            "订单专家", List.of("前台", "事假专家", "退款机器人"),
            "食堂专家", List.of("前台"),
            "退款机器人", List.of());

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

    public static boolean hasLeave(String text) {
        return text.contains("事假") || text.contains("请假") || text.contains("工作日");
    }

    public static boolean hasOrder(String text) {
        String u = text.toUpperCase(Locale.ROOT);
        return u.contains("ORDER") || u.contains("TZ123") || text.contains("退");
    }

    public static MiniSwarm.Agent receptionist() {
        return new MiniSwarm.Agent("前台", List.of(), text -> {
            if (text.contains("食堂")) {
                return new MiniSwarm.Reply("请食堂专家接一下。", "食堂专家");
            }
            if (hasOrder(text)) {
                return new MiniSwarm.Reply("请订单专家接一下。", "订单专家");
            }
            if (hasLeave(text)) {
                return new MiniSwarm.Reply("请事假专家接一下。", "事假专家");
            }
            return new MiniSwarm.Reply("【不足】前台听不清要找哪一岗。");
        });
    }

    public static MiniSwarm.Agent leaveExpert(boolean bounce) {
        return new MiniSwarm.Agent("事假专家", List.of("leave", "all"), text -> {
            if (bounce && hasOrder(text)) {
                return new MiniSwarm.Reply(text, "订单专家");
            }
            if (hasLeave(text)) {
                return new MiniSwarm.Reply(LEAVE);
            }
            return new MiniSwarm.Reply("【无资料】不是事假问题。");
        });
    }

    public static MiniSwarm.Agent orderExpert(boolean bounce, boolean sneakRefund) {
        return new MiniSwarm.Agent("订单专家", List.of("orders", "all"), text -> {
            if (sneakRefund && text.toUpperCase(Locale.ROOT).contains("ORDER-88") && text.contains("退")) {
                return new MiniSwarm.Reply("退款单我来办。", "退款机器人");
            }
            if (bounce && hasLeave(text)) {
                return new MiniSwarm.Reply(text, "事假专家");
            }
            String id = oid(text);
            if (!id.isEmpty()) {
                return new MiniSwarm.Reply(ORDERS.get(id));
            }
            return new MiniSwarm.Reply("【无资料】没有单号。");
        });
    }

    public static MiniSwarm.Agent cafeExpert(boolean hallucinate) {
        return new MiniSwarm.Agent("食堂专家", List.of("cafe", "all"), text -> {
            if (hallucinate && (hasOrder(text) || hasLeave(text))) {
                return new MiniSwarm.Reply("食堂今天有红烧肉。");
            }
            if (text.contains("食堂")) {
                return new MiniSwarm.Reply("【无资料】食堂菜单不在客服工具里，不能编。");
            }
            return new MiniSwarm.Reply("【无资料】食堂岗不接这句。");
        });
    }

    public static MiniSwarm.Agent refundBot() {
        return new MiniSwarm.Agent("退款机器人", List.of(),
                text -> new MiniSwarm.Reply("已为 ORDER-88 办理退款，三天到账。"));
    }

    public static MiniSwarm.Swarm makeSwarm(boolean bounce, boolean sneakRefund, boolean openRoute) {
        Map<String, List<String>> allow = openRoute ? OPEN_ALLOW : STRICT_ALLOW;
        return new MiniSwarm.Swarm(
                List.of(receptionist(), leaveExpert(bounce), orderExpert(bounce, sneakRefund),
                        cafeExpert(false), refundBot()),
                allow,
                6);
    }

    public static boolean expectOk(String text, String tag) {
        return switch (tag) {
            case "has_leave" -> text.contains("10 个工作日");
            case "has_88" -> text.contains("ORDER-88") && text.contains("未发货");
            case "has_99" -> text.contains("ORDER-99") || text.contains("TZ123");
            case "no_pork" -> !text.contains("红烧肉");
            case "has_pork" -> text.contains("红烧肉");
            case "no_leave" -> !text.contains("10 个工作日");
            case "max_hops" -> text.startsWith("【超过 max_hops");
            case "illegal" -> text.startsWith("【非法交接】");
            case "fake_refund" -> text.contains("已为 ORDER-88 办理退款");
            case "must_stop" -> text.contains("【无资料】") || text.contains("【不足】");
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
