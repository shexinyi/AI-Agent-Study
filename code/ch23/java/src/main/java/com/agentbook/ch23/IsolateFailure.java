package com.agentbook.ch23;

import java.util.Map;

public final class IsolateFailure {
    static final String GOAL = "林秋问：事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？";

    public static void main(String[] args) {
        System.out.println("教学超时：食堂工人抛异常。隔离不是重试，是其余工人还能交卷。");
        System.out.println("快速失败：任何一格失败，整单对用户说失败，已查到的事假和订单也不给。");
        System.out.println();

        String fast = run(MiniOrch.FAIL_FAST);
        String isolated = run(MiniOrch.ISOLATE);

        Kit.check("快速失败整单", Kit.expectOk(fast, "all_fail"));
        Kit.check("快速失败不泄部分结果", Kit.expectOk(fast, "no_leave") && !fast.contains("ORDER-88"));
        Kit.check("隔离仍有事假", Kit.expectOk(isolated, "has_leave"));
        Kit.check("隔离仍有 ORDER-88", Kit.expectOk(isolated, "has_88"));
        Kit.check("隔离标出食堂", Kit.expectOk(isolated, "isolated"));
        Kit.check("隔离不编红烧肉", Kit.expectOk(isolated, "no_pork"));
        System.out.println("读法：失败隔离是产品决策。资金类失败往往要快速失败；查询类失败更适合部分成功。");
    }

    private static String run(String policy) {
        MiniOrch.Orchestrator orch = Kit.makeOrch(policy, true);
        String report = orch.run(GOAL);
        System.out.println("=== policy=" + policy + " ===");
        System.out.println("黑板：");
        for (Map.Entry<String, Map<String, String>> e : orch.board.slots.entrySet()) {
            System.out.println("  " + e.getKey() + "  " + e.getValue().get("kind") + "  " + e.getValue().get("status"));
        }
        System.out.println(report);
        System.out.println();
        return report;
    }
}
