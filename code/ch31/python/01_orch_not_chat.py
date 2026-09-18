"""示例 1：跨系统办理 vs 三个机器人聊天。"""

from flow import Workflow, chitchat

Q = "林秋请事假 2026-09-14 到 16 共 3 天，ORDER-88 能不能退？"


if __name__ == "__main__":
    print("痛点：客服要同时查制度和订单。三个 Bot 互道早安不是项目。")
    print("编排器分格：事假工人 + 订单工人 + HITL。可退 ≠ 已退。\n")
    chat = chitchat(Q)
    wf = Workflow()
    report = wf.run(Q, "intern")
    print("=== 闲聊 ===")
    print(chat)
    print()
    print("=== 办理 ===")
    print(report.text)
    print(f"executed={report.executed} ticket={report.ticket.status if report.ticket else None}")
    print()
    kinds = [s.kind for s in report.slots]
    checks = [
        ("闲聊丢掉事假", "10 个工作日" not in chat and "3 天" not in chat),
        ("办理两格都在", "leave" in kinds and "order" in kinds),
        ("办理含 3 天和 88", "3 天" in report.text and "ORDER-88" in report.text),
        ("实习生未执行退款", report.executed == []),
        ("闲聊像客套", "红烧肉" not in chat and ("欢迎" in chat or "退款呀" in chat or "记下" in chat)),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：项目 2 卖的是工单，不是群聊记录。")
