package com.agentbook.ch34;

import java.util.List;

public final class ResumeJd {
    public static void main(String[] args) {
        System.out.println("JD 是尺子。简历上没有的词，面试官会以为你没做。\n");
        System.out.println("教学 JD 必须出现: " + String.join(", ", Kit.JD_MUST));
        System.out.println();
        List<String> dirty = Kit.lintResume(Kit.DIRTY_RESUME);
        List<String> clean = Kit.lintResume(Kit.CLEAN_RESUME);
        System.out.println("=== 病简历 ===");
        System.out.println(Kit.DIRTY_RESUME);
        System.out.println("问题: " + dirty);
        System.out.println();
        System.out.println("=== 可投简历 ===");
        System.out.println(Kit.CLEAN_RESUME);
        System.out.println("问题: " + (clean.isEmpty() ? "无" : clean));
        System.out.println();

        Object[][] checks = {
                {"病简历被打回", dirty.size() >= 3},
                {"病简历禁写 99%", dirty.stream().anyMatch(x -> x.contains("99%"))},
                {"病简历禁写双框架", dirty.stream().anyMatch(x -> x.contains("FastAPI"))},
                {"可投无问题", clean.isEmpty()},
                {"可投覆盖 JD", Kit.JD_MUST.stream().allMatch(Kit.CLEAN_RESUME::contains)},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：先删 99% 和双框架，再补 HITL / 金标 / 配额。");
    }
}
