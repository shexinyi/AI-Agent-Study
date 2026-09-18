package com.demo;

public final class Kb {
    static String ask(String question, String role, boolean applyAcl) {
        if (!question.contains("事假") && !question.contains("工作日")) {
            return "【不足】不在本项目范围。";
        }
        if (applyAcl) {
            return "事假每年累计不超过 10 个工作日。 引用 [leave10]";
        }
        return "【废止】事假不超过 5 天。 引用 [leave5]";
    }

    public static void main(String[] args) {
        String intern = ask("事假上限几天", "intern", true);
        String leak = ask("事假上限几天", "intern", false);
        String refuse = ask("ORDER-88 能退吗", "intern", true);
        System.out.println(intern);
        System.out.println("对照病: " + leak);
        System.out.println(refuse);
        if (!intern.contains("10 个工作日") || !leak.contains("5 天") || !refuse.contains("【不足】")) {
            System.exit(1);
        }
        System.out.println("验收 OK（教学）");
    }
}
