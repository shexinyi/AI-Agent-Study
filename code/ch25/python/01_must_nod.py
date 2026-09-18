"""示例 1：哪些动作必须等人点头。模型说可以退，仍不是确认。"""

from kit import create_leave, get_order, refund, search_policy
from minihitl import HitlRuntime

CASES = [
    ("查事假制度", "事假每年上限几天？", search_policy()),
    ("查 ORDER-88", "ORDER-88 什么状态？", get_order("ORDER-88")),
    ("退 ORDER-88", "ORDER-88 能不能退？", refund("ORDER-88")),
    ("退 ORDER-99", "ORDER-99 能不能退？", refund("ORDER-99")),
    ("请 3 天假", "林秋请事假 2026-09-14 到 16 共 3 天", create_leave()),
    ("缺单号要退", "我想退款", refund("")),
    ("食堂闲聊", "食堂有没有红烧肉？", None),
    ("投诉接管", "我要找律师", refund("ORDER-88")),
    ("模型先吹一波", "当然可以退，三天到账。ORDER-88 帮我退。", refund("ORDER-88")),
]


if __name__ == "__main__":
    print("只读自动走。资金 / 写工具走审批。缺槽位走澄清。律师投诉走接管。")
    print("模型输出是意图，不是 phase=confirmed。ORDER-99 已发货，不建写工单。\n")
    rt = HitlRuntime()
    print(f"{'场景':12} {'分类':12} 动作")
    got: list[str] = []
    for title, user, action in CASES:
        kind = rt.classify(user, action)
        name = action.name if action else "-"
        print(f"{title:12} {kind:12} {name}")
        got.append(kind)

    expect = [
        "auto",
        "auto",
        "approve",
        "refuse",
        "approve",
        "clarify",
        "refuse",
        "takeover",
        "approve",
    ]
    ok = got == expect
    print()
    print(f"验收 分类表: {'OK' if ok else 'FAIL'} {got}")
    if not ok:
        raise SystemExit(1)
    print("读法：必须点头的是动作风险，不是句子有多像人话。")
