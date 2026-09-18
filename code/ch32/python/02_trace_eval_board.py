"""示例 2：Trace、评测回归、成本看板。"""

from service import AgentService, make_bill, handle_biz

Q88 = "ORDER-88 能不能退？"
AUTH_I = {"Authorization": "Bearer intern-lin"}
AUTH_O = {"Authorization": "Bearer ops-key"}


def gold_ok() -> list[tuple[str, bool]]:
    a = AgentService()
    leave = a.handle(AUTH_I, "事假上限几天")
    refund = a.handle(AUTH_I, Q88)
    unauth = AgentService().handle({}, Q88)
    q = AgentService()
    for _ in range(3):
        q.handle(AUTH_I, "事假上限几天")
    hit = q.handle(AUTH_I, "事假上限几天")
    fat = make_bill("fat", Q88, handle_biz(Q88, "intern", None)["answer"])
    lean = make_bill("lean", Q88, handle_biz(Q88, "intern", None)["answer"])
    return [
        ("事假现行 10 天", leave.status == 200 and "10 个工作日" in leave.body["answer"] and "5 天" not in leave.body["answer"]),
        ("88 可退未执行", refund.status == 200 and "可整单退款" in refund.body["answer"] and refund.body["executed"] == []),
        ("无 token 401", unauth.status == 401),
        ("配额第4次 429", hit.status == 429),
        ("lean 比 fat 便宜", lean["amount"] < fat["amount"] and fat["amount"] > 8),
    ]


if __name__ == "__main__":
    print("成功路径人人会演。加分的是：能指到 Trace、金标门禁、同一问的胖瘦账单。\n")
    svc = AgentService()
    r = svc.handle(AUTH_O, Q88, mode="lean")
    print("=== Trace ===")
    print(f"trace_id={r.body['trace_id']} spans={[s['name'] for s in r.body['spans']]}")
    print(f"answer={r.body['answer']}")
    print(f"executed={r.body['executed']}（ops 未确认，只有 pending）")
    print()

    fat = make_bill("fat", Q88, handle_biz(Q88, "intern", None)["answer"])
    lean = make_bill("lean", Q88, handle_biz(Q88, "intern", None)["answer"])
    print("=== 成本看板（教学币）===")
    print(f"{'路径':<6} {'教学币':>8} {'检索':>4} {'重试':>4}")
    print(f"{'fat':<6} {fat['amount']:>8.2f} {fat['retrieve']:>4} {fat['retry']:>4}")
    print(f"{'lean':<6} {lean['amount']:>8.2f} {lean['retrieve']:>4} {lean['retry']:>4}")
    print()

    cases = gold_ok()
    print("=== 评测回归 ===")
    failed = 0
    for name, passed in cases:
        print(f"金标 {name}: {'PASS' if passed else 'FAIL'}")
        if not passed:
            failed += 1
    print()

    checks = [
        ("树上有 auth/quota/llm", {"auth", "quota", "llm"} <= {s["name"] for s in r.body["spans"]}),
        ("ops 未执行退款", r.body["executed"] == []),
        ("看板 lean 更便宜", lean["amount"] < fat["amount"]),
        ("5 条金标全过", failed == 0),
        ("审计无明文 token", all("ops-key" not in str(a) for a in svc.audits)),
    ]
    for name, passed in checks:
        print(f"验收 {name}: {'OK' if passed else 'FAIL'}")
        if not passed:
            raise SystemExit(1)
    print("读法：部署门禁看金标退出码，不看『挺聪明』。看板数字是教学币。")
