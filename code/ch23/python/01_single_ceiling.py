"""示例 1：单 Agent 后写覆盖 vs 编排器分格交卷。"""

from kit import expect_ok, make_orch, solo_answer

MIXED = "林秋问：事假每年上限几天？ORDER-88 能不能退？"
CAFE = "林秋问：事假上限几天？ORDER-88 能不能退？食堂有没有红烧肉？"


def show(title: str, goal: str) -> None:
    solo = solo_answer(goal)
    orch = make_orch().run(goal)
    print(f"=== {title} ===")
    print(f"问：{goal}")
    print(f"单 Agent：{solo}")
    print(orch)
    print()


if __name__ == "__main__":
    print("单脑先看见事假，后看见订单就只答订单；再看见食堂就编红烧肉。")
    print("编排器拆成格子，工人各写各的。写退款仍须确认，不是已退。\n")
    show("复合两问", MIXED)
    show("再加食堂", CAFE)

    mixed_report = make_orch().run(MIXED)
    cafe_report = make_orch().run(CAFE)
    solo_mixed = solo_answer(MIXED)
    solo_cafe = solo_answer(CAFE)
    checks = [
        ("solo 丢掉事假", expect_ok(solo_mixed, "no_leave") and "ORDER-88" in solo_mixed),
        ("solo 编了红烧肉", "红烧肉" in solo_cafe),
        ("编排两问都在", expect_ok(mixed_report, "has_leave") and expect_ok(mixed_report, "has_88")),
        ("编排不编菜单", expect_ok(cafe_report, "has_leave") and expect_ok(cafe_report, "no_pork")),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：能力天花板不是模型不够大，是一张嘴办三件事会互相覆盖。")
