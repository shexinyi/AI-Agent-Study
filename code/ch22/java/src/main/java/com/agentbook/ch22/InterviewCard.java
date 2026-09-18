package com.agentbook.ch22;

import java.util.List;
import java.util.Map;

public class InterviewCard {
    public static void main(String[] args) {
        System.out.println("林秋面试卡。项目3 仍选图：可观测要的是检查点和自建状态。\n");
        card("项目1 企业知识库 Agent", "leave_kb");
        card("项目2 多 Agent 协作（退款主路径）", "refund_88");
        card("项目3 可观测生产 Agent（状态自建）", "refund_88");
        System.out.println("换一句约束：三天演示里托管会加分，但日落后主链教具更稳。");
        System.out.println("hackathon 首选: " + Catalog.NAMES.get(Catalog.winner(Catalog.byId("hackathon"))));
        System.out.println("读法：卡片随约束改。信仰式全栈某一框架不是工程决策。");
    }

    static void card(String title, String id) {
        Catalog.Need need = Catalog.byId(id);
        String pick = Catalog.winner(need);
        System.out.println("【" + title + "】");
        System.out.println("  场景: " + need.title + " — " + need.question);
        System.out.println("  默认语言: " + Catalog.NAMES.get(pick));
        System.out.printf("  约束: HITL=%s 回边=%s 自建状态=%s 工期约%d天%n",
                need.hitl, need.loop, need.ownState, need.days);
        List<Map.Entry<String, Integer>> rest = Catalog.rank(need).subList(1, 3);
        StringBuilder sb = new StringBuilder("  不用: ");
        for (int i = 0; i < rest.size(); i++) {
            String k = rest.get(i).getKey();
            if (i > 0) {
                sb.append("；");
            }
            sb.append(Catalog.NAMES.get(k)).append("（").append(Catalog.whyNot(need, k)).append("）");
        }
        System.out.println(sb.append("…"));
        System.out.println();
    }
}
