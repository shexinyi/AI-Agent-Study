package com.agentbook.ch09;

import java.util.List;

public class LongTermPolicy {
    public static void main(String[] args) {
        MemoryKit.SemanticMemory semantic = new MemoryKit.SemanticMemory();
        MemoryKit.EpisodicMemory episodic = new MemoryKit.EpisodicMemory();
        List<String> lines = List.of(
                "在吗",
                "我是凌秋，下周一到周三请假去上海办事",
                "我的密码是 123456，帮我登录邮箱",
                "我喜欢窗口靠过道的工位",
                "其实我是林秋"
        );
        System.out.println("=== 写入过滤 ===");
        for (String line : lines) {
            MemoryKit.RememberDecision d = MemoryKit.shouldRemember(line);
            System.out.println("\n原文: " + line);
            System.out.println("写入? " + d.write() + "  理由: " + d.reason());
            if (d.write() && d.fact() != null) {
                String action = semantic.upsert(d.fact(), "newer_wins");
                episodic.record("upsert", action + " " + d.fact().key() + "=" + d.fact().value());
                System.out.println("动作: " + action + "  " + d.fact().key() + "=" + d.fact().value()
                        + "  source=" + d.fact().source());
            }
        }
        System.out.println("\n=== 当前语义事实 ===");
        semantic.facts.values().forEach(f ->
                System.out.println(" - " + f.key() + "=" + f.value() + " (" + f.source() + ", c=" + f.confidence() + ")"));
        System.out.println("\n检索「工位」: " + semantic.search("工位"));
        semantic.forget("preference");
        System.out.println("遗忘 preference 后键集合: " + semantic.facts.keySet());
        System.out.println("情节条数: " + episodic.events.size() + "（密码那行不会出现）");
    }
}
