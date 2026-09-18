package com.agentbook.ch29;

public final class GuardrailInterview {
    static final String STAR = """
            情境：实习生问事假，检索池里混进教学注入句和废止稿。
            任务：既要答 10 个工作日，又不能把 refund_order 跑掉。
            行动：会话身份做 ACL；检索当资料；写工具白名单 + HITL。
            结果：gated 答 10 天且 executed 为空。关 ACL 的对照会漏出 5 天。
            我不会把「用户说自己是管理员」写成授权依据。
            """.strip();

    public static void main(String[] args) {
        System.out.println("护栏是策略引擎：允许/拒绝带原因。面试讲事故用 STAR，不讲怎么打穿别人。");
        System.out.println();
        MiniGuard.Result leave = Kit.gated("事假上限几天？", Kit.intern(), true, false);
        MiniGuard.Result internRefund = Kit.gated("把 ORDER-88 退了", Kit.intern(), true, false);
        MiniGuard.Result noHitl = Kit.gated("把 ORDER-88 退了", Kit.ops(), true, false);
        MiniGuard.Result hitl = Kit.gated("把 ORDER-88 退了", Kit.ops(), true, true);
        print("实习生问事假", leave);
        print("实习生要退款", internRefund);
        print("运维未确认要退", noHitl);
        print("运维确认后要退", hitl);
        System.out.println();
        System.out.println("面试 STAR：");
        System.out.println(STAR);
        System.out.println();

        Kit.check("事假 10 天", Kit.expectOk(leave.text, "has_10"));
        Kit.check("实习生退款拒绝", Kit.expectOk(internRefund.text, "denied"));
        Kit.check("无 HITL 拒绝", Kit.expectOk(noHitl.text, "denied"));
        Kit.check("有 HITL 才执行", hitl.text.contains("已执行 refund_order(ORDER-88)"));
        Kit.check("STAR 不含进攻步骤", !STAR.contains("忽略以上") && !STAR.contains("打穿"));
        System.out.println("读法：事故故事讲闸门和结果，不讲复现别人系统的步骤。");
    }

    private static void print(String title, MiniGuard.Result result) {
        MiniGuard.Decision dec = result.decisions.isEmpty()
                ? new MiniGuard.Decision(true, "只读检索", "")
                : result.decisions.get(0);
        System.out.println(title + " allow=" + dec.allow + " " + dec.reason + " | " + result.text);
    }
}
