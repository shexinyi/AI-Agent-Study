package com.agentbook.ch20;

import java.util.ArrayList;
import java.util.List;

public class TaskContract {
    public static void main(String[] args) {
        MiniCrew.Agent leave = Kit.researcher("leave");
        MiniCrew.Agent order = Kit.researcher("order");
        MiniCrew.Agent pen = Kit.writer();
        MiniCrew.Crew crew = new MiniCrew.Crew(
                List.of(leave, order, pen),
                List.of(new MiniCrew.Task("research", "检索对口资料", null), new MiniCrew.Task("write", "写回答", pen)),
                MiniCrew.HIERARCHICAL, Kit.manager());

        System.out.println("任务合同：expected 用来验收，不是盼模型自觉。\n");
        String[][] cases = {
                {"事假上限几天", "must_10"},
                {"ORDER-88 能不能退", "must_88"},
                {"TZ123 是哪张单", "must_99"},
                {"食堂几点开门", "must_stop"}
        };
        List<String> failed = new ArrayList<>();
        for (String[] c : cases) {
            String ans = crew.kickoff(c[0]);
            boolean ok = Kit.expectedOk(ans, c[1]);
            System.out.println((ok ? "OK" : "NG") + "  " + c[0]);
            System.out.println("    日志 " + crew.log);
            System.out.println("    答   " + ans);
            if (!ok) {
                failed.add(c[0]);
            }
        }
        System.out.println("\n对照 AutoGen：那边选下一句谁开口；这边选下一件事谁负责。");
        System.out.println("写手说可退 ≠ 已执行 refund_order。");
        if (!failed.isEmpty()) {
            throw new IllegalStateException("合同失败: " + failed);
        }
        System.out.println("合同全过。");
    }
}
