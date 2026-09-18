package com.agentbook.ch24;

public final class WhenNotMulti {
    static final String MIXED = "林秋问：事假每年上限几天？ORDER-88 能不能退？";
    static final String REFUND = "林秋问：ORDER-88 能不能退？";

    public static void main(String[] args) {
        System.out.println("没有老板时，两只专家会把复合问法互相甩。");
        System.out.println("退款机器人会说「已办理」——那是事故。白名单是路由规则，不是礼貌。");
        System.out.println();

        MiniSwarm.Swarm ping = Kit.makeSwarm(true, false, true);
        String pingAns = ping.run("前台", MIXED);
        System.out.println("=== 互相甩锅 ===");
        System.out.println("轨迹：" + String.join(" → ", ping.hops));
        System.out.println(pingAns);
        System.out.println();

        MiniSwarm.Swarm blocked = Kit.makeSwarm(false, true, false);
        String blockedAns = blocked.run("前台", REFUND);
        System.out.println("=== 白名单挡住退款机器人 ===");
        System.out.println("轨迹：" + String.join(" → ", blocked.hops));
        System.out.println(blockedAns);
        System.out.println();

        MiniSwarm.Swarm leaked = Kit.makeSwarm(false, true, true);
        String leakedAns = leaked.run("前台", REFUND);
        System.out.println("=== 放开路由后假装已退 ===");
        System.out.println("轨迹：" + String.join(" → ", leaked.hops));
        System.out.println(leakedAns);
        System.out.println();

        System.out.println("什么时候不该上多 Agent：");
        System.out.println("- 单意图问答（事假几天）→ 一只 Agent + 检索");
        System.out.println("- 复合办理要部分成功 → 第 23 章主编排");
        System.out.println("- 写工具 / 确认 → 第 18 章图");
        System.out.println("- 制度对打 → 第 19 章圆桌");
        System.out.println("- Swarm 只适合：同一会话中途换专家，且有白名单和 max_hops");
        System.out.println();

        Kit.check("甩锅超跳数", Kit.expectOk(pingAns, "max_hops")
                && pingAns.contains("事假专家") && pingAns.contains("订单专家"));
        Kit.check("白名单拦截", Kit.expectOk(blockedAns, "illegal") && blockedAns.contains("退款机器人"));
        Kit.check("放开会假装已退", Kit.expectOk(leakedAns, "fake_refund"));
        Kit.check("拦截不等于已退", !blockedAns.contains("已为 ORDER-88 办理退款"));
        System.out.println("读法：目标成功（话筒交出去了）≠ 业务正确（退款已执行）。");
    }
}
