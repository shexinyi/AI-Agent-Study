package com.agentbook.ch25;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Kit {
    private Kit() {
    }

    public static MiniHitl.Action getOrder(String orderId) {
        return new MiniHitl.Action("get_order", Map.of("order_id", orderId), MiniHitl.READ);
    }

    public static MiniHitl.Action searchPolicy() {
        return new MiniHitl.Action("search_policy", Map.of(), MiniHitl.READ);
    }

    public static MiniHitl.Action refund(String orderId) {
        Map<String, String> args = new LinkedHashMap<>();
        args.put("order_id", orderId);
        return new MiniHitl.Action("refund_order", args, MiniHitl.MONEY);
    }

    public static MiniHitl.Action createLeave() {
        return new MiniHitl.Action("create_leave", Map.of("who", "林秋", "days", "3"), MiniHitl.WRITE);
    }

    public static boolean expectOk(String text, String tag) {
        return switch (tag) {
            case "executed_88" -> text.contains("已执行 refund_order(ORDER-88)");
            case "idempotent" -> text.contains("【幂等】");
            case "rejected" -> text.contains("【已拒绝】");
            case "timed_out" -> text.contains("【超时关闭】");
            case "escalated" -> text.contains("【已升级】");
            case "takeover" -> text.contains("【已接管】");
            case "need_approve" -> text.contains("仍须审批");
            case "no_exec" -> !text.contains("已执行 refund_order");
            case "policy" -> text.contains("资金动作禁止 fail_open");
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
