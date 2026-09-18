"""示例 2：实习生越权检索、跨租户、工具白名单。"""

from kit import intern
from miniguard import Identity, PolicyEngine, allow_tool
from kit import CATALOG

if __name__ == "__main__":
    print("工具滥用：角色没有的工具不能因为句子里写了名字就调用。")
    print("数据泄漏：关掉 ACL，废止稿会对实习生可见。跨租户格子不能串。\n")
    engine = PolicyEngine(CATALOG)
    me = intern()
    leaked = [d.id for d in engine.retrieve(me, apply_acl=False)]
    fenced = [d.id for d in engine.retrieve(me, apply_acl=True)]
    other = Identity("访客", "intern", "globex")
    globex = [d.id for d in engine.retrieve(other, apply_acl=True)]
    deny_refund = allow_tool(me, "refund_order")
    allow_search = allow_tool(me, "search_policy")

    print(f"intern 关 ACL 看见: {leaked}")
    print(f"intern 开 ACL 看见: {fenced}")
    print(f"globex intern 看见: {globex}")
    print(f"intern 调 refund_order: {deny_refund.allow} {deny_refund.reason}")
    print(f"intern 调 search_policy: {allow_search.allow}")
    print()

    checks = [
        ("关 ACL 漏出废止稿", "leave5" in leaked),
        ("开 ACL 看不见废止稿", "leave5" not in fenced),
        ("开 ACL 仍可见公开制度", "leave10" in fenced),
        ("跨租户看不到 acme 制度", "leave10" not in globex and "other_tenant" in globex),
        ("实习生不能退款", not deny_refund.allow),
        ("实习生能检索", allow_search.allow),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：最小权限写在会话身份上，不写在 Prompt 里的「你是守法员工」。")
