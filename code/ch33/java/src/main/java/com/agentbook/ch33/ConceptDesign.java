package com.agentbook.ch33;

public final class ConceptDesign {
    public static void main(String[] args) {
        System.out.println("面试先答闭环和尺子，再报框架名。\n");
        System.out.println("=== 概念 ===");
        for (Kit.Concept card : Kit.CONCEPTS) {
            System.out.println("Q: " + card.q);
            System.out.println("浅：" + card.shallow);
            System.out.println("深：" + card.deep);
            System.out.println();
        }
        System.out.println("=== 设计（90 秒画框）===");
        Kit.DESIGNS.forEach((key, d) -> {
            System.out.println("[" + key + "] " + d.name + ": " + d.one);
            System.out.println("  必须有: " + d.must);
            System.out.println("  不准有: " + d.ban);
        });
        System.out.println();

        Kit.Design cs = Kit.DESIGNS.get("cs");
        Kit.Design research = Kit.DESIGNS.get("research");
        Kit.Design ops = Kit.DESIGNS.get("ops");
        Object[][] checks = {
                {"四张概念卡都有加分句", Kit.CONCEPTS.stream().allMatch(Kit::conceptOk)},
                {"客服有 HITL 和编排器", cs.must.contains("hitl") && cs.must.contains("orchestrator")},
                {"客服禁止自动退款", cs.ban.contains("auto_refund")},
                {"研报只读带引用", research.must.contains("cite") && research.ban.contains("write_tool")},
                {"运维变更须人批", ops.must.contains("hitl") && ops.ban.contains("fail_open")},
                {"不把 Swarm 当目标主人", cs.ban.contains("swarm_owns_goal")},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：设计题先画门禁和人，再画模型框。");
    }
}
