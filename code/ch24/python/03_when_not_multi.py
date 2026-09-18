"""示例 3：甩锅超跳数、白名单挡住退款机器人、放开后假装已退。"""

from kit import expect_ok, make_swarm

MIXED = "林秋问：事假每年上限几天？ORDER-88 能不能退？"
REFUND = "林秋问：ORDER-88 能不能退？"


if __name__ == "__main__":
    print("没有老板时，两只专家会把复合问法互相甩。")
    print("退款机器人会说「已办理」——那是事故。白名单是路由规则，不是礼貌。\n")

    ping = make_swarm(bounce=True)
    ping_ans = ping.run("前台", MIXED)
    print("=== 互相甩锅 ===")
    print(f"轨迹：{' → '.join(ping.hops)}")
    print(ping_ans)
    print()

    blocked = make_swarm(sneak_refund=True, open_route=False)
    blocked_ans = blocked.run("前台", REFUND)
    print("=== 白名单挡住退款机器人 ===")
    print(f"轨迹：{' → '.join(blocked.hops)}")
    print(blocked_ans)
    print()

    leaked = make_swarm(sneak_refund=True, open_route=True)
    leaked_ans = leaked.run("前台", REFUND)
    print("=== 放开路由后假装已退 ===")
    print(f"轨迹：{' → '.join(leaked.hops)}")
    print(leaked_ans)
    print()

    print("什么时候不该上多 Agent：")
    print("- 单意图问答（事假几天）→ 一只 Agent + 检索")
    print("- 复合办理要部分成功 → 第 23 章主编排")
    print("- 写工具 / 确认 → 第 18 章图")
    print("- 制度对打 → 第 19 章圆桌")
    print("- Swarm 只适合：同一会话中途换专家，且有白名单和 max_hops")
    print()

    checks = [
        ("甩锅超跳数", expect_ok(ping_ans, "max_hops") and "事假专家" in ping_ans and "订单专家" in ping_ans),
        ("白名单拦截", expect_ok(blocked_ans, "illegal") and "退款机器人" in blocked_ans),
        ("放开会假装已退", expect_ok(leaked_ans, "fake_refund")),
        ("拦截不等于已退", "已为 ORDER-88 办理退款" not in blocked_ans),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：目标成功（话筒交出去了）≠ 业务正确（退款已执行）。")
