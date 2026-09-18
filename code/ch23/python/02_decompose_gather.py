"""示例 2：任务拆解进黑板，汇总只读格子。"""

from kit import expect_ok, make_orch

GOAL = "林秋请事假 2026-09-14 到 16 共 3 天；ORDER-88 能不能退；食堂有没有红烧肉？"


if __name__ == "__main__":
    orch = make_orch()
    tasks = orch.decompose(GOAL)
    print("拆解是合同：每条任务有 id / kind，工人不许改别人的 kind。")
    print(f"问：{GOAL}\n")
    print("任务清单：")
    for task in tasks:
        print(f"  {task.id}  kind={task.kind}")

    report = orch.run(GOAL)
    print("\n黑板格子：")
    for task_id, slot in orch.board.slots.items():
        print(f"  {task_id}  {slot.get('kind')}  {slot.get('status')}  {slot.get('result') or slot.get('error')}")

    print()
    print(report)
    print()

    kinds = [t.kind for t in tasks]
    checks = [
        ("拆出三格", kinds == ["leave", "order", "cafe"]),
        ("汇总含 3 天", "3 天" in report and expect_ok(report, "has_leave")),
        ("汇总含 ORDER-88", expect_ok(report, "has_88")),
        ("食堂不编造", "【无资料】" in report and expect_ok(report, "no_pork")),
        ("工人不互写", all(slot.get("kind") in ("leave", "order", "cafe") for slot in orch.board.slots.values())),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：汇总读黑板，不让写手再去闭卷发挥。可退 ≠ 已调用 refund_order。")
