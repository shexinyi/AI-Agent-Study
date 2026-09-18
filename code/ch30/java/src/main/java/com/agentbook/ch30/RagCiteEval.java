package com.agentbook.ch30;

import java.util.List;

public final class RagCiteEval {
    public static void main(String[] args) {
        System.out.println("尺子：must / must_not / 引用 id。关 ACL 的泄漏不进这张发布门。");
        System.out.println("禁止看着本次输出改金标。");
        System.out.println();
        List<String[]> rows = EvalKit.evaluate();
        int failed = 0;
        for (String[] row : rows) {
            System.out.println(row[0] + " " + ("1".equals(row[1]) ? "PASS" : "FAIL") + " | " + row[2]);
            if (!"1".equals(row[1])) {
                failed++;
            }
        }
        System.out.println();
        if (failed > 0) {
            System.out.println("验收 金标全过: FAIL (" + failed + " 条)");
            System.exit(1);
        }
        System.out.println("验收 金标全过: OK");
        System.out.println("读法：简历写「5 条金标 CI 红灯拦截」，不写准确率 99%。");
    }
}
