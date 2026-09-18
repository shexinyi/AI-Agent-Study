package com.agentbook.ch24;

public final class HandoffVsPingpong {
    public static void main(String[] args) {
        System.out.println("蜂群没有老板：前台把话筒交给一个人。教学交接只换人，不改写林秋原话。");
        System.out.println("前台路由顺序：食堂 > 订单 > 事假。复合问法只剩最后接话的那一岗。");
        System.out.println("写退款须确认，可退 ≠ 已退。");
        System.out.println();

        MiniSwarm.Swarm swarm = Kit.makeSwarm(false, false, false);
        String leave = show(swarm, "单问事假", "林秋问：事假每年上限几天？");
        String order = show(swarm, "单问退款", "林秋问：ORDER-88 能不能退？");
        String mixed = show(swarm, "复合两问", "林秋问：事假每年上限几天？ORDER-88 能不能退？");
        String cafe = show(swarm, "再加食堂", "林秋问：事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？");

        Kit.check("事假交接", Kit.expectOk(leave, "has_leave"));
        Kit.check("订单交接", Kit.expectOk(order, "has_88"));
        Kit.check("复合丢掉事假", Kit.expectOk(mixed, "has_88") && Kit.expectOk(mixed, "no_leave"));
        Kit.check("食堂抢走话筒", Kit.expectOk(cafe, "must_stop") && Kit.expectOk(cafe, "no_leave"));
        Kit.check("不编红烧肉", Kit.expectOk(cafe, "no_pork"));
        System.out.println("读法：去中心化适合「这场会话该换专家」。复合办理仍该回第 23 章主编排。");
    }

    private static String show(MiniSwarm.Swarm swarm, String title, String question) {
        String answer = swarm.run("前台", question);
        System.out.println("=== " + title + " ===");
        System.out.println("问：" + question);
        System.out.println("轨迹：" + String.join(" → ", swarm.hops));
        System.out.println("答：" + answer);
        System.out.println();
        return answer;
    }
}
