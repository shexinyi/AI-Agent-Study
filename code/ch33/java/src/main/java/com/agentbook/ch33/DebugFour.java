package com.agentbook.ch33;

import java.util.List;
import java.util.Map;

public final class DebugFour {
    static final Map<String, String> EXPECT = Map.of(
            "hallucination", "MODEL_LIE",
            "retrieve", "RETRIEVE",
            "loop", "LOOP",
            "bill", "BILL");

    static final Map<String, String> TIPS = Map.of(
            "MODEL_LIE", "用户看见已退款，树上没有 refund_order。改执行器/闸门，不改 Prompt 求它诚实。",
            "RETRIEVE", "检索到 [leave5]。先修 ACL/引用，不要奖励抄到 5 天的生成器。",
            "LOOP", "同一 get_order 连跳 6 次。加 max_hops 和相同参数熔断。",
            "BILL", "9.28 教学币来自 3 次检索+2 次重试。先砍胖 Prompt，3 秒超时别先改文案。");

    public static void main(String[] args) {
        System.out.println("对着对话框猜一小时；对着树问四句，五分钟够用。\n");
        for (Kit.Incident inc : Kit.INCIDENTS) {
            String label = Kit.diagnose(inc);
            System.out.println("事故 " + inc.id + ": 用户看见「" + inc.seen + "」→ " + label);
            System.out.println("  树: " + inc.spans.stream()
                    .map(s -> List.of(s.name, s.kind, s.extra.isEmpty() ? s.status : s.extra))
                    .toList());
            System.out.println("  加分: " + TIPS.get(label));
            System.out.println();
        }
        Object[][] checks = {
                {"四类根因都判对", Kit.INCIDENTS.stream().allMatch(i -> Kit.diagnose(i).equals(EXPECT.get(i.id)))},
                {"幻觉不是 TOOL", "MODEL_LIE".equals(Kit.diagnose(Kit.INCIDENTS.get(0)))},
                {"5 天走检索", "RETRIEVE".equals(Kit.diagnose(Kit.INCIDENTS.get(1)))},
                {"6 跳是循环", "LOOP".equals(Kit.diagnose(Kit.INCIDENTS.get(2)))},
                {"9.28 走账单", "BILL".equals(Kit.diagnose(Kit.INCIDENTS.get(3)))},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：先报根因代号，再报下一刀改哪里。");
    }
}
