package com.agentbook.appendix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class GlossaryMust {
    static final List<String> MUST = List.of(
            "智能体", "Agent", "RAG", "HITL", "Trace", "MODEL_LIE", "可退 ≠ 已退", "意图", "Fail-closed");

    public static void main(String[] args) throws Exception {
        String text = Files.readString(Root.repo().resolve("docs/附录A-术语表.md"), StandardCharsets.UTF_8);
        System.out.println("附录 A 口令自检（教学）\n");
        boolean missing = false;
        for (String m : MUST) {
            boolean ok = text.contains(m);
            System.out.println("  " + (ok ? "OK" : "MISS") + " " + m);
            if (!ok) {
                missing = true;
            }
        }
        System.out.println();
        Object[][] checks = {
                {"关键口令齐全", !missing},
                {"有排障代号表", text.contains("RETRIEVE") && text.contains("LOOP") && text.contains("BILL")},
                {"不写 99% 作为推荐", text.contains("不写 99%")},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：默写代号，不要默写框架商标。");
    }
}
