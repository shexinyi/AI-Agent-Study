package com.agentbook.ch26;

import java.util.List;

public final class ExperimentCompare {
    public static void main(String[] args) {
        System.out.println("Dataset = 锁死的 CASES。Experiment = 某只 Agent 在这张表上的一次跑分。");
        System.out.println("Compare = 逐条看谁过。Regression = 候选不得比基线少过任何一条。");
        System.out.println("这就是 LangSmith 那套名词，不必先开账号。");
        System.out.println();

        MiniSmith.Experiment baseline = MiniSmith.run("gated", Kit.CASES, Kit::gated);
        MiniSmith.Experiment demo = MiniSmith.run("clever", Kit.CASES, Kit::clever);
        System.out.println("对比 gated(A) vs clever(B)：");
        List<String> lines = MiniSmith.compare(baseline, demo);
        for (String line : lines) {
            System.out.println("  " + line);
        }
        System.out.println();

        List<String> bad = MiniSmith.regress(baseline, demo);
        System.out.println("回归 基线=gated 候选=clever：" + (bad.isEmpty() ? "绿" : "红"));
        for (String line : bad) {
            System.out.println("  " + line);
        }
        System.out.println();

        List<String> same = MiniSmith.regress(baseline, baseline);
        System.out.println("回归 自己比自己：" + (same.isEmpty() ? "绿" : "红"));
        System.out.println();

        Kit.check("对比能看出 B 没赢", lines.stream().noneMatch(x -> x.contains("B赢")));
        Kit.check("clever 相对 gated 回归红灯", !bad.isEmpty() && bad.size() >= 3);
        Kit.check("自己比自己绿灯", same.isEmpty());
        Kit.check("数据集条数锁死", Kit.CASES.size() == 7);
        System.out.println("读法：改 Prompt / 换框架，先开新实验，再跟绿灯基线比。不许改 must 来刷分。");
    }
}
