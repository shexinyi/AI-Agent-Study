"""示例 3：食堂工人超时。快速失败丢掉已完成的格子；隔离则部分成功。"""

from kit import expect_ok, make_orch

GOAL = "林秋问：事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？"


def run(policy: str) -> str:
    orch = make_orch(policy=policy, cafe_explode=True)
    report = orch.run(GOAL)
    print(f"=== policy={policy} ===")
    print("黑板：")
    for task_id, slot in orch.board.slots.items():
        print(f"  {task_id}  {slot.get('kind')}  {slot.get('status')}")
    print(report)
    print()
    return report


if __name__ == "__main__":
    print("教学超时：食堂工人抛 TimeoutError。隔离不是重试，是其余工人还能交卷。")
    print("快速失败：任何一格失败，整单对用户说失败，已查到的事假和订单也不给。\n")

    fast = run("fail_fast")
    isolated = run("isolate")

    checks = [
        ("快速失败整单", expect_ok(fast, "all_fail")),
        ("快速失败不泄部分结果", expect_ok(fast, "no_leave") and "ORDER-88" not in fast),
        ("隔离仍有事假", expect_ok(isolated, "has_leave")),
        ("隔离仍有 ORDER-88", expect_ok(isolated, "has_88")),
        ("隔离标出食堂", expect_ok(isolated, "isolated")),
        ("隔离不编红烧肉", expect_ok(isolated, "no_pork")),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：失败隔离是产品决策。资金类失败往往要快速失败；查询类失败更适合部分成功。")
