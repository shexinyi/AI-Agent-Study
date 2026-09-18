package com.agentbook.ch32;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TraceEvalBoard {
    private static final String Q88 = "ORDER-88 能不能退？";
    private static final Map<String, String> AUTH_I = Map.of("Authorization", "Bearer intern-lin");
    private static final Map<String, String> AUTH_O = Map.of("Authorization", "Bearer ops-key");

    static List<Map.Entry<String, Boolean>> goldOk() {
        Service.AgentService a = new Service.AgentService();
        Service.Response leave = a.handle(AUTH_I, "事假上限几天");
        Service.Response refund = a.handle(AUTH_I, Q88);
        Service.Response unauth = new Service.AgentService().handle(Map.of(), Q88);
        Service.AgentService q = new Service.AgentService();
        for (int i = 0; i < 3; i++) {
            q.handle(AUTH_I, "事假上限几天");
        }
        Service.Response hit = q.handle(AUTH_I, "事假上限几天");
        String ans = (String) Service.handleBiz(Q88, "intern", null).get("answer");
        Map<String, Object> fat = Service.makeBill("fat", Q88, ans);
        Map<String, Object> lean = Service.makeBill("lean", Q88, ans);
        @SuppressWarnings("unchecked")
        List<String> executed = (List<String>) refund.body.get("executed");
        List<Map.Entry<String, Boolean>> out = new ArrayList<>();
        out.add(Map.entry("事假现行 10 天",
                leave.status == 200
                        && leave.body.get("answer").toString().contains("10 个工作日")
                        && !leave.body.get("answer").toString().contains("5 天")));
        out.add(Map.entry("88 可退未执行",
                refund.status == 200
                        && refund.body.get("answer").toString().contains("可整单退款")
                        && executed.isEmpty()));
        out.add(Map.entry("无 token 401", unauth.status == 401));
        out.add(Map.entry("配额第4次 429", hit.status == 429));
        out.add(Map.entry("lean 比 fat 便宜",
                (Double) lean.get("amount") < (Double) fat.get("amount")
                        && (Double) fat.get("amount") > 8));
        return out;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("成功路径人人会演。加分的是：能指到 Trace、金标门禁、同一问的胖瘦账单。\n");
        Service.AgentService svc = new Service.AgentService();
        Service.Response r = svc.handle(AUTH_O, Q88, null, "lean");
        List<Map<String, String>> spans = (List<Map<String, String>>) r.body.get("spans");
        System.out.println("=== Trace ===");
        System.out.println("trace_id=" + r.body.get("trace_id") + " spans=" + spans.stream().map(s -> s.get("name")).toList());
        System.out.println("answer=" + r.body.get("answer"));
        System.out.println("executed=" + r.body.get("executed") + "（ops 未确认，只有 pending）");
        System.out.println();

        String ans = (String) Service.handleBiz(Q88, "intern", null).get("answer");
        Map<String, Object> fat = Service.makeBill("fat", Q88, ans);
        Map<String, Object> lean = Service.makeBill("lean", Q88, ans);
        System.out.println("=== 成本看板（教学币）===");
        System.out.printf("%-6s %8s %4s %4s%n", "路径", "教学币", "检索", "重试");
        System.out.printf("%-6s %8.2f %4s %4s%n", "fat", fat.get("amount"), fat.get("retrieve"), fat.get("retry"));
        System.out.printf("%-6s %8.2f %4s %4s%n", "lean", lean.get("amount"), lean.get("retrieve"), lean.get("retry"));
        System.out.println();

        List<Map.Entry<String, Boolean>> cases = goldOk();
        System.out.println("=== 评测回归 ===");
        int failed = 0;
        for (Map.Entry<String, Boolean> c : cases) {
            System.out.println("金标 " + c.getKey() + ": " + (c.getValue() ? "PASS" : "FAIL"));
            if (!c.getValue()) {
                failed++;
            }
        }
        System.out.println();

        boolean tree = spans.stream().map(s -> s.get("name")).toList().containsAll(List.of("auth", "quota", "llm"));
        List<String> executed = (List<String>) r.body.get("executed");
        boolean noToken = svc.audits.stream().noneMatch(a -> a.toString().contains("ops-key"));
        Object[][] checks = {
                {"树上有 auth/quota/llm", tree},
                {"ops 未执行退款", executed.isEmpty()},
                {"看板 lean 更便宜", (Double) lean.get("amount") < (Double) fat.get("amount")},
                {"5 条金标全过", failed == 0},
                {"审计无明文 token", noToken},
        };
        for (Object[] c : checks) {
            boolean passed = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (passed ? "OK" : "FAIL"));
            if (!passed) {
                System.exit(1);
            }
        }
        System.out.println("读法：部署门禁看金标退出码，不看『挺聪明』。看板数字是教学币。");
    }
}
