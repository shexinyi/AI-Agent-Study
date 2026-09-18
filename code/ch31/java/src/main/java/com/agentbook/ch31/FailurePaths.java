package com.agentbook.ch31;

public final class FailurePaths {
    public static void main(String[] args) {
        System.out.println("成功路径人人会演。加分的是：已发货、未确认、越权、隔离。");
        System.out.println();
        Flow.Workflow wf = new Flow.Workflow();
        Flow.Report r99 = wf.run("ORDER-99 能不能退？", "agent_ops", "approve", false);
        Flow.Report r88 = wf.run("ORDER-88 能不能退？", "agent_ops", null, false);
        Flow.Report rrej = wf.run("ORDER-88 能不能退？", "agent_ops", "reject", false);
        Flow.Report rint = wf.run("ORDER-88 能不能退？", "intern", "approve", false);
        Flow.Report rcafe = wf.run("事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？",
                "intern", null, true);

        System.out.println("ORDER-99 强行批准：");
        System.out.println(r99.text + " executed=" + r99.executed);
        System.out.println("ORDER-88 未确认：");
        System.out.println(r88.text);
        System.out.println("ORDER-88 拒绝：");
        System.out.println(rrej.text);
        System.out.println("实习生带确认：");
        System.out.println(rint.text);
        System.out.println("食堂超时隔离：");
        System.out.println(rcafe.text);
        System.out.println();

        check("99 不执行", r99.executed.isEmpty() && r99.text.contains("售后"));
        check("88 待确认", r88.ticket != null && "pending".equals(r88.ticket.status) && r88.executed.isEmpty());
        check("拒绝不执行", rrej.text.contains("【已拒绝】") && rrej.executed.isEmpty());
        check("实习生确认也拒", rint.text.contains("白名单") && rint.executed.isEmpty());
        check("食堂失败仍有事假和 88", rcafe.text.contains("10 个工作日")
                && rcafe.text.contains("ORDER-88") && rcafe.text.contains("失败隔离"));
        check("隔离不是已退", !rcafe.text.contains("已执行 refund_order"));
        System.out.println("读法：每条失败都要能指到格子或审批单，不要用「系统出错」糊过去。");
    }

    private static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }
}
