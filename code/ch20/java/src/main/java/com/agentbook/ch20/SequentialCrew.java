package com.agentbook.ch20;

import java.util.List;

public class SequentialCrew {
    public static void main(String[] args) {
        MiniCrew.Agent leave = Kit.researcher("leave");
        MiniCrew.Agent pen = Kit.writer();
        MiniCrew.Crew crew = new MiniCrew.Crew(
                List.of(leave, pen),
                List.of(new MiniCrew.Task("research", "检索事假制度", leave),
                        new MiniCrew.Task("write", "写回答", pen)),
                MiniCrew.SEQUENTIAL,
                null);
        System.out.println("顺序流：你是计划员。任务写死顺序，角色预先绑好。\n");
        for (String q : List.of("事假上限几天", "年假能卖吗")) {
            System.out.println("问: " + q);
            String ans = crew.kickoff(q);
            System.out.println("日志: " + String.join(" → ", crew.log));
            System.out.println("答: " + ans);
            System.out.println();
        }
        System.out.println("读法：产品语言是「谁负责哪件事」，不是「下一句谁开口」。");
    }
}
