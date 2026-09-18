package com.agentbook.ch32;

import java.util.List;
import java.util.Map;

public final class ScriptVsService {
    private static final String Q = "林秋手机13800138000，事假上限几天？";
    private static final Map<String, String> AUTH = Map.of("Authorization", "Bearer intern-lin");

    public static void main(String[] args) {
        System.out.println("痛点：脚本能跑不等于能上线。谁都能调、手机号进日志、配额打穿。");
        System.out.println("服务管道：鉴权 → 配额 → 办理 → 审计脱敏。可退 ≠ 已退。\n");

        String leaked = Service.scriptAsk(Q);
        System.out.println("=== 脚本 ===");
        System.out.println(leaked);
        System.out.println();

        Service.AgentService svc = new Service.AgentService();
        Service.Response denied = svc.handle(Map.of(), Q);
        Service.Response ok = svc.handle(AUTH, Q);
        Service.Response second = svc.handle(AUTH, "ORDER-88 能退吗");
        svc.handle(AUTH, "再确认一下事假");
        Service.Response fourth = svc.handle(AUTH, "还想问食堂");

        System.out.println("=== 服务 ===");
        System.out.println("无 token → " + denied.status + " " + denied.body);
        System.out.println("第1次 → " + ok.status + " " + ok.body.get("answer"));
        System.out.println("第2次 ORDER-88 → executed=" + second.body.get("executed"));
        System.out.println("第4次 → " + fourth.status + " " + fourth.body);
        System.out.println("审计问句: " + svc.audits.stream().map(a -> a.get("question")).toList());
        System.out.println();

        @SuppressWarnings("unchecked")
        List<String> executed = (List<String>) second.body.get("executed");
        boolean phoneGone = svc.audits.stream().noneMatch(a -> a.get("question").toString().contains("13800138000"));
        boolean phoneTag = svc.audits.stream().anyMatch(a -> a.get("question").toString().contains("[PHONE]"));
        Object[][] checks = {
                {"脚本泄漏手机号", leaked.contains("13800138000")},
                {"无 token 是 401", denied.status == 401},
                {"实习生第1次 200 且 10 个工作日", ok.status == 200 && ok.body.get("answer").toString().contains("10 个工作日")},
                {"ORDER-88 未执行退款", executed.isEmpty()},
                {"第4次 429", fourth.status == 429 && Integer.valueOf(3).equals(fourth.body.get("quota_used"))},
                {"审计无手机号", phoneGone},
                {"审计有脱敏标记", phoneTag},
        };
        for (Object[] c : checks) {
            boolean passed = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (passed ? "OK" : "FAIL"));
            if (!passed) {
                System.exit(1);
            }
        }
        System.out.println("读法：项目 3 卖的是门禁和审计，不是又写了一个脚本。");
    }
}
