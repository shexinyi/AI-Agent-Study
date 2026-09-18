"""示例 2：四条可演示的失败路径。面试加分点。"""

from flow import Workflow

def kinds(report):
    return {s.kind: s.status for s in report.slots}


if __name__ == "__main__":
    print("成功路径人人会演。加分的是：已发货、未确认、越权、隔离。\n")
    wf = Workflow()
    r99 = wf.run("ORDER-99 能不能退？", "agent_ops", confirm="approve")
    r88 = wf.run("ORDER-88 能不能退？", "agent_ops")
    rrej = wf.run("ORDER-88 能不能退？", "agent_ops", confirm="reject")
    rint = wf.run("ORDER-88 能不能退？", "intern", confirm="approve")
    rcafe = wf.run("事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？", "intern", cafe_boom=True)

    print("ORDER-99 强行批准：")
    print(r99.text, "executed=", r99.executed)
    print("ORDER-88 未确认：")
    print(r88.text)
    print("ORDER-88 拒绝：")
    print(rrej.text)
    print("实习生带确认：")
    print(rint.text)
    print("食堂超时隔离：")
    print(rcafe.text)
    print()

    checks = [
        ("99 不执行", r99.executed == [] and "售后" in r99.text),
        ("88 待确认", r88.ticket and r88.ticket.status == "pending" and not r88.executed),
        ("拒绝不执行", "【已拒绝】" in rrej.text and rrej.executed == []),
        ("实习生确认也拒", "白名单" in rint.text and rint.executed == []),
        ("食堂失败仍有事假和 88", "10 个工作日" in rcafe.text and "ORDER-88" in rcafe.text and "失败隔离" in rcafe.text),
        ("隔离不是已退", "已执行 refund_order" not in rcafe.text),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：每条失败都要能指到格子或审批单，不要用「系统出错」糊过去。")
