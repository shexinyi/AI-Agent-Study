package com.agentbook.ch25;

import java.util.Map;

public final class FourModes {
    public static void main(String[] args) {
        System.out.println("四种等人：审批点头、澄清补槽、接管停手、回放只看当时。");
        System.out.println("回放再执行走幂等，不能把 ORDER-88 退第二次。");
        System.out.println();

        MiniHitl.HitlRuntime rt = new MiniHitl.HitlRuntime();
        MiniHitl.Ticket t1 = rt.submit(MiniHitl.APPROVE, MiniHitl.TICKET, Kit.refund("ORDER-88"),
                60_000, MiniHitl.FAIL_CLOSED, Map.of());
        System.out.println("=== 审批 / 工单 ===");
        System.out.println("提交 " + t1.id + " channel=" + t1.channel + " status=" + t1.status);
        String done = rt.resume(t1.id, "approve", "");
        System.out.println(done);
        System.out.println();

        MiniHitl.Ticket t2 = rt.submit(MiniHitl.CLARIFY, MiniHitl.IM, Kit.refund(""),
                60_000, MiniHitl.FAIL_CLOSED, Map.of("ask", "退哪一单？"));
        System.out.println("=== 澄清 / IM ===");
        System.out.println("提交 " + t2.id + " 问林秋：" + t2.payload.get("ask"));
        String filled = rt.resume(t2.id, "clarify", "ORDER-88");
        System.out.println(filled);
        String still = rt.resume(t2.id, "approve", "");
        System.out.println(still);
        System.out.println();

        MiniHitl.Ticket t3 = rt.submit(MiniHitl.TAKEOVER, MiniHitl.CONSOLE, Kit.refund("ORDER-88"),
                60_000, MiniHitl.FAIL_CLOSED, Map.of());
        System.out.println("=== 接管 / 控制台 ===");
        String taken = rt.resume(t3.id, "takeover", "");
        System.out.println(taken);
        String blocked = rt.resume(t3.id, "approve", "");
        System.out.println(blocked);
        System.out.println();

        System.out.println("=== 回放 / 控制台 ===");
        String replayed = rt.replay(t1.id);
        System.out.println(replayed);
        System.out.println();

        Kit.check("审批执行退款", Kit.expectOk(done, "executed_88"));
        Kit.check("澄清后仍须审批", Kit.expectOk(filled, "need_approve"));
        Kit.check("补槽后再退一次仍幂等", Kit.expectOk(still, "idempotent"));
        Kit.check("接管后停手", Kit.expectOk(taken, "takeover"));
        Kit.check("接管单不能再批", blocked.contains("【不能恢复】"));
        Kit.check("回放不二次退款", Kit.expectOk(replayed, "idempotent") && replayed.contains("ORDER-88"));
        System.out.println("读法：澄清补的是槽位，不是批准。回放是审计，不是再按一次退款。");
    }
}
