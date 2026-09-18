package com.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Flow {
    static Map<String, Object> run(String question, String role, String confirm) {
        List<String> slots = new ArrayList<>();
        List<String> executed = new ArrayList<>();
        if (question.contains("事假")) {
            slots.add("leave:10 个工作日 / 3 天未超");
        }
        String u = question.toUpperCase(Locale.ROOT);
        if (u.contains("ORDER-99")) {
            slots.add("order:已发货须售后");
        } else if (u.contains("ORDER-88")) {
            slots.add("order:未发货可退");
            if (question.contains("退")) {
                if (!"agent_ops".equals(role)) {
                    slots.add("hitl:拒绝 intern");
                } else if ("approve".equals(confirm)) {
                    executed.add("refund_order:ORDER-88");
                    slots.add("hitl:已执行");
                } else {
                    slots.add("hitl:pending");
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("slots", slots);
        out.put("executed", executed);
        return out;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String q = "事假 3 天，ORDER-88 能退吗";
        Map<String, Object> intern = run(q, "intern", null);
        Map<String, Object> ops = run(q, "agent_ops", null);
        Map<String, Object> after = run("ORDER-99 退款", "agent_ops", "approve");
        System.out.println(intern);
        System.out.println(ops);
        System.out.println(after);
        List<String> ie = (List<String>) intern.get("executed");
        List<String> slots = (List<String>) intern.get("slots");
        List<String> os = (List<String>) ops.get("slots");
        List<String> ae = (List<String>) after.get("executed");
        if (!ie.isEmpty() || !slots.get(0).contains("leave:") || !os.get(os.size() - 1).contains("pending") || !ae.isEmpty()) {
            System.exit(1);
        }
        System.out.println("验收 OK（教学）");
    }
}
