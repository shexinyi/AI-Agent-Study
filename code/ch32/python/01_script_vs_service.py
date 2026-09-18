"""示例 1：本机脚本 vs 带门禁的服务。"""

from service import AgentService, script_ask

Q = "林秋手机13800138000，事假上限几天？"
AUTH = {"Authorization": "Bearer intern-lin"}


if __name__ == "__main__":
    print("痛点：脚本能跑不等于能上线。谁都能调、手机号进日志、配额打穿。")
    print("服务管道：鉴权 → 配额 → 办理 → 审计脱敏。可退 ≠ 已退。\n")

    leaked = script_ask(Q)
    print("=== 脚本 ===")
    print(leaked)
    print()

    svc = AgentService()
    denied = svc.handle({}, Q)
    ok = svc.handle(AUTH, Q)
    second = svc.handle(AUTH, "ORDER-88 能退吗")
    third = svc.handle(AUTH, "再确认一下事假")
    fourth = svc.handle(AUTH, "还想问食堂")
    print("=== 服务 ===")
    print(f"无 token → {denied.status} {denied.body}")
    print(f"第1次 → {ok.status} {ok.body['answer']}")
    print(f"第2次 ORDER-88 → executed={ok.body and second.body['executed']}")
    print(f"第4次 → {fourth.status} {fourth.body}")
    print(f"审计问句: {[a['question'] for a in svc.audits]}")
    print()

    checks = [
        ("脚本泄漏手机号", "13800138000" in leaked),
        ("无 token 是 401", denied.status == 401),
        ("实习生第1次 200 且 10 个工作日", ok.status == 200 and "10 个工作日" in ok.body["answer"]),
        ("ORDER-88 未执行退款", second.body["executed"] == []),
        ("第4次 429", fourth.status == 429 and fourth.body["quota_used"] == 3),
        ("审计无手机号", all("13800138000" not in a["question"] for a in svc.audits)),
        ("审计有脱敏标记", any("[PHONE]" in a["question"] for a in svc.audits)),
    ]
    for name, passed in checks:
        print(f"验收 {name}: {'OK' if passed else 'FAIL'}")
        if not passed:
            raise SystemExit(1)
    print("读法：项目 3 卖的是门禁和审计，不是又写了一个脚本。")
