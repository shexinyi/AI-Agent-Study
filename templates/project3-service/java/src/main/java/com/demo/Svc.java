package com.demo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Svc {
    static Map<String, Object> handle(String token, String question, Map<String, Integer> used) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!"intern-lin".equals(token)) {
            out.put("status", 401);
            out.put("trace_id", "tr-0001");
            return out;
        }
        int n = used.getOrDefault(token, 0);
        if (n >= 3) {
            out.put("status", 429);
            out.put("quota_used", n);
            out.put("quota_limit", 3);
            return out;
        }
        used.put(token, n + 1);
        out.put("status", 200);
        out.put("answer", "事假 10 个工作日。可退 ≠ 已退。");
        return out;
    }

    public static void main(String[] args) {
        Map<String, Integer> used = new HashMap<>();
        String q = "林秋事假几天（教学）";
        Map<String, Object> denied = handle("", q, used);
        Map<String, Object> ok = handle("intern-lin", q, used);
        handle("intern-lin", q, used);
        handle("intern-lin", q, used);
        Map<String, Object> fourth = handle("intern-lin", q, used);
        System.out.println(denied);
        System.out.println(ok);
        System.out.println(fourth);
        if (!Integer.valueOf(401).equals(denied.get("status"))
                || !Integer.valueOf(200).equals(ok.get("status"))
                || !Integer.valueOf(429).equals(fourth.get("status"))) {
            System.exit(1);
        }
        System.out.println("验收 OK（教学）");
    }
}
