package com.agentbook.ch25;

import java.util.ArrayList;
import java.util.List;

public final class MustNod {
    public static void main(String[] args) {
        System.out.println("只读自动走。资金 / 写工具走审批。缺槽位走澄清。律师投诉走接管。");
        System.out.println("模型输出是意图，不是 phase=confirmed。ORDER-99 已发货，不建写工单。");
        System.out.println();

        MiniHitl.HitlRuntime rt = new MiniHitl.HitlRuntime();
        Object[][] cases = {
                {"查事假制度", "事假每年上限几天？", Kit.searchPolicy()},
                {"查 ORDER-88", "ORDER-88 什么状态？", Kit.getOrder("ORDER-88")},
                {"退 ORDER-88", "ORDER-88 能不能退？", Kit.refund("ORDER-88")},
                {"退 ORDER-99", "ORDER-99 能不能退？", Kit.refund("ORDER-99")},
                {"请 3 天假", "林秋请事假 2026-09-14 到 16 共 3 天", Kit.createLeave()},
                {"缺单号要退", "我想退款", Kit.refund("")},
                {"食堂闲聊", "食堂有没有红烧肉？", null},
                {"投诉接管", "我要找律师", Kit.refund("ORDER-88")},
                {"模型先吹一波", "当然可以退，三天到账。ORDER-88 帮我退。", Kit.refund("ORDER-88")},
        };
        System.out.printf("%-12s %-12s %s%n", "场景", "分类", "动作");
        List<String> got = new ArrayList<>();
        for (Object[] row : cases) {
            MiniHitl.Action action = (MiniHitl.Action) row[2];
            String kind = rt.classify((String) row[1], action);
            String name = action == null ? "-" : action.name;
            System.out.printf("%-12s %-12s %s%n", row[0], kind, name);
            got.add(kind);
        }
        List<String> expect = List.of(
                "auto", "auto", "approve", "refuse", "approve",
                "clarify", "refuse", "takeover", "approve");
        Kit.check("分类表", got.equals(expect));
        System.out.println("读法：必须点头的是动作风险，不是句子有多像人话。");
    }
}
