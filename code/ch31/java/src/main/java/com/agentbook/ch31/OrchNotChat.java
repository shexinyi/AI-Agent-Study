package com.agentbook.ch31;

public final class OrchNotChat {
    static final String Q = "林秋请事假 2026-09-14 到 16 共 3 天，ORDER-88 能不能退？";

    public static void main(String[] args) {
        System.out.println("痛点：客服要同时查制度和订单。三个 Bot 互道早安不是项目。");
        System.out.println("编排器分格：事假工人 + 订单工人 + HITL。可退 ≠ 已退。");
        System.out.println();
        String chat = Flow.chitchat(Q);
        Flow.Report report = new Flow.Workflow().run(Q, "intern", null, false);
        System.out.println("=== 闲聊 ===");
        System.out.println(chat);
        System.out.println();
        System.out.println("=== 办理 ===");
        System.out.println(report.text);
        System.out.println("executed=" + report.executed + " ticket="
                + (report.ticket == null ? null : report.ticket.status));
        System.out.println();
        boolean hasLeave = report.slots.stream().anyMatch(s -> "leave".equals(s.kind));
        boolean hasOrder = report.slots.stream().anyMatch(s -> "order".equals(s.kind));
        check("闲聊丢掉事假", !chat.contains("10 个工作日") && !chat.contains("3 天"));
        check("办理两格都在", hasLeave && hasOrder);
        check("办理含 3 天和 88", report.text.contains("3 天") && report.text.contains("ORDER-88"));
        check("实习生未执行退款", report.executed.isEmpty());
        check("闲聊像客套", !chat.contains("红烧肉")
                && (chat.contains("欢迎") || chat.contains("退款呀") || chat.contains("记下")));
        System.out.println("读法：项目 2 卖的是工单，不是群聊记录。");
    }

    private static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }
}
