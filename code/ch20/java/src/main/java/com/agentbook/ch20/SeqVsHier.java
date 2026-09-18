package com.agentbook.ch20;

import java.util.List;

public class SeqVsHier {
    public static void main(String[] args) {
        MiniCrew.Agent leave = Kit.researcher("leave");
        MiniCrew.Agent pen = Kit.writer();
        MiniCrew.Crew seq = new MiniCrew.Crew(
                List.of(leave, pen),
                List.of(new MiniCrew.Task("research", "检索", leave), new MiniCrew.Task("write", "写回答", pen)),
                MiniCrew.SEQUENTIAL, null);

        System.out.println("顺序流把研究员写死成事假岗时，订单问题会检索落空。");
        for (String q : List.of("事假上限几天", "ORDER-88 能不能退")) {
            System.out.println("\n[顺序] 问: " + q);
            System.out.println("答: " + seq.kickoff(q));
            System.out.println("日志: " + String.join(" → ", seq.log));
        }

        MiniCrew.Agent order = Kit.researcher("order");
        MiniCrew.Agent boss = Kit.manager();
        MiniCrew.Crew hier = new MiniCrew.Crew(
                List.of(leave, order, pen),
                List.of(new MiniCrew.Task("research", "检索对口资料", null), new MiniCrew.Task("write", "写回答", pen)),
                MiniCrew.HIERARCHICAL, boss);

        System.out.println("\n层级流：经理看问题再指派。写手预绑定。");
        for (String q : List.of("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单", "食堂几点开门")) {
            System.out.println("\n[层级] 问: " + q);
            System.out.println("答: " + hier.kickoff(q));
            System.out.println("日志: " + String.join(" → ", hier.log));
        }
        System.out.println("\n读法：层级流多一次经理调用。选错经理更难查。");
    }
}
