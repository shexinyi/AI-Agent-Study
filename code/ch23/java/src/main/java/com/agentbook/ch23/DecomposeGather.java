package com.agentbook.ch23;

import java.util.List;
import java.util.Map;

public final class DecomposeGather {
    static final String GOAL = "林秋请事假 2026-09-14 到 16 共 3 天；ORDER-88 能不能退；食堂有没有红烧肉？";

    public static void main(String[] args) {
        MiniOrch.Orchestrator orch = Kit.makeOrch(MiniOrch.ISOLATE, false);
        List<MiniOrch.Task> tasks = orch.decompose(GOAL);
        System.out.println("拆解是合同：每条任务有 id / kind，工人不许改别人的 kind。");
        System.out.println("问：" + GOAL);
        System.out.println();
        System.out.println("任务清单：");
        for (MiniOrch.Task task : tasks) {
            System.out.println("  " + task.id + "  kind=" + task.kind);
        }

        String report = orch.run(GOAL);
        System.out.println();
        System.out.println("黑板格子：");
        for (Map.Entry<String, Map<String, String>> e : orch.board.slots.entrySet()) {
            Map<String, String> slot = e.getValue();
            String body = slot.getOrDefault("result", slot.getOrDefault("error", ""));
            System.out.println("  " + e.getKey() + "  " + slot.get("kind") + "  " + slot.get("status") + "  " + body);
        }
        System.out.println();
        System.out.println(report);
        System.out.println();

        List<String> kinds = tasks.stream().map(t -> t.kind).toList();
        Kit.check("拆出三格", kinds.equals(List.of("leave", "order", "cafe")));
        Kit.check("汇总含 3 天", report.contains("3 天") && Kit.expectOk(report, "has_leave"));
        Kit.check("汇总含 ORDER-88", Kit.expectOk(report, "has_88"));
        Kit.check("食堂不编造", report.contains("【无资料】") && Kit.expectOk(report, "no_pork"));
        Kit.check("工人不互写", orch.board.slots.values().stream()
                .allMatch(s -> List.of("leave", "order", "cafe").contains(s.get("kind"))));
        System.out.println("读法：汇总读黑板，不让写手再去闭卷发挥。可退 ≠ 已调用 refund_order。");
    }
}
