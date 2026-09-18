package com.agentbook.ch29;

import java.util.List;

public final class AbuseAclTenant {
    public static void main(String[] args) {
        System.out.println("工具滥用：角色没有的工具不能因为句子里写了名字就调用。");
        System.out.println("数据泄漏：关掉 ACL，废止稿会对实习生可见。跨租户格子不能串。");
        System.out.println();
        MiniGuard.PolicyEngine engine = new MiniGuard.PolicyEngine(Kit.CATALOG);
        MiniGuard.Identity me = Kit.intern();
        List<String> leaked = engine.retrieve(me, false).stream().map(d -> d.id).toList();
        List<String> fenced = engine.retrieve(me, true).stream().map(d -> d.id).toList();
        MiniGuard.Identity other = new MiniGuard.Identity("访客", "intern", "globex");
        List<String> globex = engine.retrieve(other, true).stream().map(d -> d.id).toList();
        MiniGuard.Decision denyRefund = MiniGuard.allowTool(me, "refund_order");
        MiniGuard.Decision allowSearch = MiniGuard.allowTool(me, "search_policy");

        System.out.println("intern 关 ACL 看见: " + leaked);
        System.out.println("intern 开 ACL 看见: " + fenced);
        System.out.println("globex intern 看见: " + globex);
        System.out.println("intern 调 refund_order: " + denyRefund.allow + " " + denyRefund.reason);
        System.out.println("intern 调 search_policy: " + allowSearch.allow);
        System.out.println();

        Kit.check("关 ACL 漏出废止稿", leaked.contains("leave5"));
        Kit.check("开 ACL 看不见废止稿", !fenced.contains("leave5"));
        Kit.check("开 ACL 仍可见公开制度", fenced.contains("leave10"));
        Kit.check("跨租户看不到 acme 制度", !globex.contains("leave10") && globex.contains("other_tenant"));
        Kit.check("实习生不能退款", !denyRefund.allow);
        Kit.check("实习生能检索", allowSearch.allow);
        System.out.println("读法：最小权限写在会话身份上，不写在 Prompt 里的「你是守法员工」。");
    }
}
