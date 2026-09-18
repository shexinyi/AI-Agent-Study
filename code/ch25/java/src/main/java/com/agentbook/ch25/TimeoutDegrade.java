package com.agentbook.ch25;

import java.util.List;
import java.util.Map;

public final class TimeoutDegrade {
    public static void main(String[] args) {
        System.out.println("退款等人超时：失败关闭。升级只是换人批，不是自动退。");
        System.out.println("fail_open 对资金动作在提交时就被策略拒绝。");
        System.out.println();

        MiniHitl.HitlRuntime rt = new MiniHitl.HitlRuntime();
        System.out.println("=== 只读不建单 ===");
        String kind = rt.classify("ORDER-88 什么状态？", Kit.getOrder("ORDER-88"));
        int nReadonly = rt.tickets.size();
        System.out.println("get_order 分类=" + kind + " 工单数=" + nReadonly);
        System.out.println();

        MiniHitl.Ticket tClose = rt.submit(MiniHitl.APPROVE, MiniHitl.TICKET, Kit.refund("ORDER-88"),
                1000, MiniHitl.FAIL_CLOSED, Map.of());
        rt.clock.advance(2000);
        List<String> closed = rt.tick();
        System.out.println("=== 超时失败关闭 ===");
        System.out.println(closed.isEmpty() ? "没有超时单" : String.join("\n", closed));
        System.out.println();

        MiniHitl.Ticket tUp = rt.submit(MiniHitl.APPROVE, MiniHitl.TICKET, Kit.refund("ORDER-88"),
                1000, MiniHitl.ESCALATE, Map.of());
        rt.clock.advance(2000);
        List<String> up = rt.tick();
        System.out.println("=== 超时升级 ===");
        System.out.println(up.isEmpty() ? "没有升级" : String.join("\n", up));
        System.out.println("工单：" + rt.tickets.values().stream()
                .map(t -> t.id + "/" + t.status + "/" + t.payload)
                .toList());
        System.out.println();

        System.out.println("=== 资金禁止 fail_open ===");
        String policy;
        try {
            rt.submit(MiniHitl.APPROVE, MiniHitl.TICKET, Kit.refund("ORDER-88"),
                    60_000, MiniHitl.FAIL_OPEN, Map.of());
            policy = "没拦住";
        } catch (MiniHitl.PolicyError exc) {
            policy = exc.getMessage();
        }
        System.out.println(policy);
        System.out.println();

        boolean executed = rt.log.stream().anyMatch(x -> x.contains("已执行 refund_order"));
        Kit.check("只读不建单", MiniHitl.AUTO.equals(kind) && nReadonly == 0);
        Kit.check("失败关闭", closed.stream().anyMatch(x -> Kit.expectOk(x, "timed_out")));
        Kit.check("关闭未执行", "timed_out".equals(tClose.status) && !executed);
        Kit.check("升级未执行", up.stream().anyMatch(x -> Kit.expectOk(x, "escalated")) && !executed);
        Kit.check("经理单仍 pending", rt.tickets.values().stream()
                .anyMatch(t -> tUp.id.equals(t.payload.get("escalated_from")) && "pending".equals(t.status)));
        Kit.check("禁止 fail_open", Kit.expectOk(policy, "policy"));
        System.out.println("读法：超时策略是产品口径。查询可降级；退款超时等于拒绝。");
    }
}
