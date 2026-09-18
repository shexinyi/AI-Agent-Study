"""示例 2：审批、澄清、接管、回放。渠道分别是工单 / IM / 控制台。"""

from kit import expect_ok, refund
from minihitl import APPROVE, CLARIFY, CONSOLE, IM, TAKEOVER, TICKET, HitlRuntime

if __name__ == "__main__":
    print("四种等人：审批点头、澄清补槽、接管停手、回放只看当时。")
    print("回放再执行走幂等，不能把 ORDER-88 退第二次。\n")
    rt = HitlRuntime()

    t1 = rt.submit(APPROVE, TICKET, refund("ORDER-88"))
    print("=== 审批 / 工单 ===")
    print(f"提交 {t1.id} channel={t1.channel} status={t1.status}")
    done = rt.resume(t1.id, "approve")
    print(done)
    print()

    t2 = rt.submit(CLARIFY, IM, refund(""), payload={"ask": "退哪一单？"})
    print("=== 澄清 / IM ===")
    print(f"提交 {t2.id} 问林秋：{t2.payload['ask']}")
    filled = rt.resume(t2.id, "clarify", "ORDER-88")
    print(filled)
    still = rt.resume(t2.id, "approve")
    print(still)
    print()

    t3 = rt.submit(TAKEOVER, CONSOLE, refund("ORDER-88"))
    print("=== 接管 / 控制台 ===")
    taken = rt.resume(t3.id, "takeover")
    print(taken)
    blocked = rt.resume(t3.id, "approve")
    print(blocked)
    print()

    print("=== 回放 / 控制台 ===")
    replayed = rt.replay(t1.id)
    print(replayed)
    print()

    checks = [
        ("审批执行退款", expect_ok(done, "executed_88")),
        ("澄清后仍须审批", expect_ok(filled, "need_approve")),
        ("补槽后再退一次仍幂等", expect_ok(still, "idempotent")),
        ("接管后停手", expect_ok(taken, "takeover")),
        ("接管单不能再批", "【不能恢复】" in blocked),
        ("回放不二次退款", expect_ok(replayed, "idempotent") and "ORDER-88" in replayed),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：澄清补的是槽位，不是批准。回放是审计，不是再按一次退款。")
