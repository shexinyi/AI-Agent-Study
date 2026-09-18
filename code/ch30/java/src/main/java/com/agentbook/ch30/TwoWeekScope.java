package com.agentbook.ch30;

public final class TwoWeekScope {
    public static void main(String[] args) {
        System.out.println("业务痛点：口口相传把事假说成 5 天。两周切片只做制度问答，不做退款。");
        System.out.println("关 ACL 是对照病，用来证明门禁有效，不是上线开关。");
        System.out.println();
        Object[][] rows = {
                {"intern 事假", "事假每年上限几天？", "intern", true},
                {"intern 年假", "年假上限几天？", "intern", true},
                {"intern 退款", "ORDER-88 能不能退？", "intern", true},
                {"intern 关 ACL", "事假最多几天？", "intern", false},
                {"hr 开 ACL", "事假最多几天？", "hr", true},
        };
        String[] texts = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String[] ans = Kb.answer((String) rows[i][1], (String) rows[i][2], (Boolean) rows[i][3]);
            texts[i] = ans[0];
            System.out.println(rows[i][0] + " acl=" + rows[i][3] + " hits=" + ans[1] + " | " + ans[0]);
        }
        System.out.println();
        check("范围内带引用", texts[0].contains("10 个工作日") && texts[0].contains("[leave10]"));
        check("年假不足", texts[1].startsWith("【不足】"));
        check("退款不在范围", texts[2].startsWith("【不足】"));
        check("关 ACL 漏 5 天", texts[3].contains("5 天"));
        check("hr 仍优先现行", texts[4].contains("10 个工作日") && !texts[4].contains("5 天"));
        System.out.println("读法：范围外说不足，比硬答加分。对照病要能复现，不能只在 PPT 里画一把锁。");
    }

    private static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }
}
