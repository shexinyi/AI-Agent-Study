package com.agentbook.ch22;

import java.util.Map;

public class ScoreConstraints {
    public static void main(String[] args) {
        System.out.println("权重是课堂约定：HITL/回边偏图，对打偏圆桌，工种流水线偏排班。不是官方排名。\n");
        System.out.printf("%-16s %7s %7s %7s %7s %7s  首选%n", "场景", "chain", "graph", "chat", "crew", "hosted");
        for (Catalog.Need need : Catalog.NEEDS) {
            Map<String, Integer> scores = Catalog.score(need);
            String pick = Catalog.winner(need);
            System.out.printf("%-16s %7d %7d %7d %7d %7d  %s%n",
                    need.id, scores.get("chain"), scores.get("graph"), scores.get("chat"),
                    scores.get("crew"), scores.get("hosted"), Catalog.NAMES.get(pick));
            System.out.println("  " + need.title + " / " + need.question);
        }
        System.out.println("\n读法：分最高的是约束下的默认语言。换约束，分会翻。");
    }
}
