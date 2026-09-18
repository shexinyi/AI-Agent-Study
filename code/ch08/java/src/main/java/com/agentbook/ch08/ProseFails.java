package com.agentbook.ch08;

public class ProseFails {
    public static void main(String[] args) {
        tryParse("A 纯散文", MessyModel.complete("prose"));
        tryParse("B 围栏 JSON，但 days=5 与日期矛盾", MessyModel.complete("fenced_bad"));
        tryParse("C 字段残缺", MessyModel.complete("missing_field"));
        tryParse("D 合格 JSON", MessyModel.complete("valid"));
        System.out.println("\n读法：能读成 JSON 不等于能进 HR 系统。B 会被天数校验拦住。");
    }

    static void tryParse(String title, String raw) {
        System.out.println("\n### " + title);
        System.out.println("模型原文:\n" + raw);
        try {
            LeaveExtract parsed = ParseUtil.parse(raw);
            System.out.println("Jackson+校验 通过: " + parsed);
        } catch (Exception e) {
            System.out.println("拒绝: " + e.getClass().getSimpleName() + " / " + e.getMessage());
        }
    }
}
