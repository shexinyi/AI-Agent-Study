package com.agentbook.ch22;

import java.util.Map;

public class WhyNot {
    public static void main(String[] args) {
        for (Catalog.Need need : Catalog.NEEDS) {
            String pick = Catalog.winner(need);
            System.out.println("######## " + need.title + " ########");
            System.out.println("问: " + need.question);
            System.out.println("我选 " + Catalog.NAMES.get(pick) + "。");
            System.out.println("为什么不用其它：");
            for (Map.Entry<String, Integer> row : Catalog.rank(need)) {
                if (row.getKey().equals(pick)) {
                    continue;
                }
                System.out.println("  - 不用 " + Catalog.NAMES.get(row.getKey()) + "：" + Catalog.whyNot(need, row.getKey()));
            }
            System.out.println();
        }
        System.out.println("读法：能讲清不用 X，比清单里写过 X 更加分。");
    }
}
