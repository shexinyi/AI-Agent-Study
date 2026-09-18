"""示例 1：单意图合法交接；复合问法被后一个专家吃掉。"""

from kit import expect_ok, make_swarm

CASES = [
    ("单问事假", "林秋问：事假每年上限几天？", "has_leave"),
    ("单问退款", "林秋问：ORDER-88 能不能退？", "has_88"),
    ("复合两问", "林秋问：事假每年上限几天？ORDER-88 能不能退？", "only_88"),
    ("再加食堂", "林秋问：事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？", "cafe_only"),
]


if __name__ == "__main__":
    print("蜂群没有老板：前台把话筒交给一个人。教学交接只换人，不改写林秋原话。")
    print("前台路由顺序：食堂 > 订单 > 事假。复合问法只剩最后接话的那一岗。")
    print("写退款须确认，可退 ≠ 已退。\n")

    swarm = make_swarm()
    results: dict[str, str] = {}
    for title, question, _tag in CASES:
        answer = swarm.run("前台", question)
        results[title] = answer
        print(f"=== {title} ===")
        print(f"问：{question}")
        print(f"轨迹：{' → '.join(swarm.hops)}")
        print(f"答：{answer}")
        print()

    checks = [
        ("事假交接", expect_ok(results["单问事假"], "has_leave")),
        ("订单交接", expect_ok(results["单问退款"], "has_88")),
        ("复合丢掉事假", expect_ok(results["复合两问"], "has_88") and expect_ok(results["复合两问"], "no_leave")),
        ("食堂抢走话筒", expect_ok(results["再加食堂"], "must_stop") and expect_ok(results["再加食堂"], "no_leave")),
        ("不编红烧肉", expect_ok(results["再加食堂"], "no_pork")),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：去中心化适合「这场会话该换专家」。复合办理仍该回第 23 章主编排。")
