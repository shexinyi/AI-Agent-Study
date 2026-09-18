"""示例 3：超时失败关闭、升级仍不执行；资金禁止 fail_open。"""

from kit import expect_ok, get_order, refund
from minihitl import APPROVE, ESCALATE, FAIL_OPEN, TICKET, HitlRuntime, PolicyError

if __name__ == "__main__":
    print("退款等人超时：失败关闭。升级只是换人批，不是自动退。")
    print("fail_open 对资金动作在提交时就被策略拒绝。\n")
    rt = HitlRuntime()

    print("=== 只读不建单 ===")
    kind = rt.classify("ORDER-88 什么状态？", get_order("ORDER-88"))
    n_readonly = len(rt.tickets)
    print(f"get_order 分类={kind} 工单数={n_readonly}")
    print()

    t_close = rt.submit(APPROVE, TICKET, refund("ORDER-88"), wait_ms=1000)
    rt.clock.advance(2000)
    closed = rt.tick()
    print("=== 超时失败关闭 ===")
    print("\n".join(closed) or "没有超时单")
    print()

    t_up = rt.submit(APPROVE, TICKET, refund("ORDER-88"), wait_ms=1000, timeout_policy=ESCALATE)
    rt.clock.advance(2000)
    up = rt.tick()
    print("=== 超时升级 ===")
    print("\n".join(up) or "没有升级")
    print(f"工单：{[(t.id, t.status, t.payload) for t in rt.tickets.values()]}")
    print()

    print("=== 资金禁止 fail_open ===")
    try:
        rt.submit(APPROVE, TICKET, refund("ORDER-88"), timeout_policy=FAIL_OPEN)
        policy = "没拦住"
    except PolicyError as exc:
        policy = str(exc)
    print(policy)
    print()

    executed = any("已执行 refund_order" in x for x in rt.log)
    checks = [
        ("只读不建单", kind == "auto" and n_readonly == 0),
        ("失败关闭", any(expect_ok(x, "timed_out") for x in closed)),
        ("关闭未执行", t_close.status == "timed_out" and not executed),
        ("升级未执行", any(expect_ok(x, "escalated") for x in up) and not executed),
        ("经理单仍 pending", any(t.payload.get("escalated_from") == t_up.id and t.status == "pending" for t in rt.tickets.values())),
        ("禁止 fail_open", expect_ok(policy, "policy")),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：超时策略是产品口径。查询可降级；退款超时等于拒绝。")
