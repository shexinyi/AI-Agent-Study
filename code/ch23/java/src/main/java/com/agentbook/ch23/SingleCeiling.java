package com.agentbook.ch23;

public final class SingleCeiling {
    static final String MIXED = "林秋问：事假每年上限几天？ORDER-88 能不能退？";
    static final String CAFE = "林秋问：事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？";

    public static void main(String[] args) {
        System.out.println("单脑先看见事假，后看见订单就只答订单；再看见食堂就编红烧肉。");
        System.out.println("编排器拆成格子，工人各写各的。写退款仍须确认，不是已退。");
        System.out.println();
        show("复合两问", MIXED);
        show("再加食堂", CAFE);

        String mixedReport = Kit.makeOrch(MiniOrch.ISOLATE, false).run(MIXED);
        String cafeReport = Kit.makeOrch(MiniOrch.ISOLATE, false).run(CAFE);
        String soloMixed = Kit.soloAnswer(MIXED);
        String soloCafe = Kit.soloAnswer(CAFE);
        Kit.check("solo 丢掉事假", Kit.expectOk(soloMixed, "no_leave") && soloMixed.contains("ORDER-88"));
        Kit.check("solo 编了红烧肉", soloCafe.contains("红烧肉"));
        Kit.check("编排两问都在", Kit.expectOk(mixedReport, "has_leave") && Kit.expectOk(mixedReport, "has_88"));
        Kit.check("编排不编菜单", Kit.expectOk(cafeReport, "has_leave") && Kit.expectOk(cafeReport, "no_pork"));
        System.out.println("读法：能力天花板不是模型不够大，是一张嘴办三件事会互相覆盖。");
    }

    private static void show(String title, String goal) {
        System.out.println("=== " + title + " ===");
        System.out.println("问：" + goal);
        System.out.println("单 Agent：" + Kit.soloAnswer(goal));
        System.out.println(Kit.makeOrch(MiniOrch.ISOLATE, false).run(goal));
        System.out.println();
    }
}
