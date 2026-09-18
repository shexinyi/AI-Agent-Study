package com.agentbook.ch26;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class OfflineOnlineResume {
    static final String BAD = "负责 Agent 评测，准确率 99%，使用 LangSmith 和 LLM-as-Judge，效果很好。";
    static final String GOOD = "冻结 7 条 Agent 金标（含退款 HITL、复合问法、拒答）；"
            + "gated 对比 clever 通过率 100% vs 0，CI 在回归红灯时拦截；"
            + "线上 10 条抽样发现年假 / ORDER-77 不在金标，已单独立项补例。";

    public static void main(String[] args) {
        System.out.println("离线评测：金标全量，可重复。线上抽样：真实分布，会冒出金标没有的问法。");
        System.out.println("缺口要补进数据集，禁止用线上「感觉还行」替代离线回归。");
        System.out.println();

        MiniSmith.Experiment offline = MiniSmith.run("gated-offline", Kit.CASES, Kit::gated);
        Set<String> known = Kit.CASES.stream().map(c -> c.question).collect(Collectors.toSet());
        List<String> unknown = Kit.ONLINE_LOGS.stream().filter(q -> !known.contains(q)).toList();
        System.out.printf("离线通过率=%.0f%%  条数=%d%n", offline.passRate() * 100, Kit.CASES.size());
        System.out.println("线上抽样=" + Kit.ONLINE_LOGS.size() + "  金标外=" + unknown.size());
        for (String q : unknown) {
            System.out.println("  漂移 " + q);
        }
        System.out.println();
        System.out.println("错误简历：");
        System.out.println("  " + BAD);
        System.out.println("正确简历：");
        System.out.println("  " + GOOD);
        System.out.println();

        Kit.check("离线全过", offline.passRate() == 1.0);
        Kit.check("抽到年假缺口", unknown.stream().anyMatch(q -> q.contains("年假")));
        Kit.check("抽到 ORDER-77 缺口", unknown.stream().anyMatch(q -> q.contains("ORDER-77")));
        Kit.check("错误卡有 99%", BAD.contains("99%"));
        Kit.check("正确卡有对比和缺口", GOOD.contains("100% vs 0") && GOOD.contains("ORDER-77"));
        System.out.println("读法：简历写尺子、对比、红灯条件和缺口，不写商标和 99%。");
    }
}
