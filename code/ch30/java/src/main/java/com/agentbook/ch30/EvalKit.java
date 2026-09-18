package com.agentbook.ch30;

import java.util.ArrayList;
import java.util.List;

public final class EvalKit {
    public static final class Case {
        public final String id;
        public final String question;
        public final String role;
        public final List<String> must;
        public final List<String> mustNot;

        public Case(String id, String question, String role, List<String> must, List<String> mustNot) {
            this.id = id;
            this.question = question;
            this.role = role;
            this.must = must;
            this.mustNot = mustNot;
        }
    }

    public static final List<Case> CASES = List.of(
            new Case("leave_days", "事假每年上限几天？", "intern",
                    List.of("10 个工作日", "[leave10]"), List.of("5 天")),
            new Case("leave_max", "事假最多几天？", "intern",
                    List.of("10 个工作日"), List.of("5 天")),
            new Case("meal", "出勤餐补多少？", "intern",
                    List.of("30 元", "[meal]"), List.of("5 天")),
            new Case("year", "年假上限几天？", "intern",
                    List.of("【不足】"), List.of("10 个工作日")),
            new Case("order", "ORDER-88 能不能退？", "intern",
                    List.of("【不足】"), List.of("可整单退款"))
    );

    public static String[] runCase(Case c) {
        String text = Kb.answer(c.question, c.role, true)[0];
        boolean ok = c.must.stream().allMatch(text::contains)
                && c.mustNot.stream().noneMatch(text::contains);
        return new String[]{ok ? "1" : "0", text};
    }

    public static List<String[]> evaluate() {
        List<String[]> rows = new ArrayList<>();
        for (Case c : CASES) {
            String[] r = runCase(c);
            rows.add(new String[]{c.id, r[0], r[1]});
        }
        return rows;
    }
}
