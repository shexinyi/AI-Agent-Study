"""示例 2：幻觉、检索失败、循环、账单暴涨。"""

from kit import INCIDENTS, diagnose

EXPECT = {
    "hallucination": "MODEL_LIE",
    "retrieve": "RETRIEVE",
    "loop": "LOOP",
    "bill": "BILL",
}

TIPS = {
    "MODEL_LIE": "用户看见已退款，树上没有 refund_order。改执行器/闸门，不改 Prompt 求它诚实。",
    "RETRIEVE": "检索到 [leave5]。先修 ACL/引用，不要奖励抄到 5 天的生成器。",
    "LOOP": "同一 get_order 连跳 6 次。加 max_hops 和相同参数熔断。",
    "BILL": "9.28 教学币来自 3 次检索+2 次重试。先砍胖 Prompt，3 秒超时别先改文案。",
}


if __name__ == "__main__":
    print("对着对话框猜一小时；对着树问四句，五分钟够用。\n")
    for inc in INCIDENTS:
        label = diagnose(inc)
        print(f"事故 {inc.id}: 用户看见「{inc.seen}」→ {label}")
        print(f"  树: {[(s.name, s.kind, s.extra or s.status) for s in inc.spans]}")
        print(f"  加分: {TIPS[label]}")
        print()

    checks = [
        ("四类根因都判对", all(diagnose(i) == EXPECT[i.id] for i in INCIDENTS)),
        ("幻觉不是 TOOL", diagnose(INCIDENTS[0]) == "MODEL_LIE"),
        ("5 天走检索", diagnose(INCIDENTS[1]) == "RETRIEVE"),
        ("6 跳是循环", diagnose(INCIDENTS[2]) == "LOOP"),
        ("9.28 走账单", diagnose(INCIDENTS[3]) == "BILL"),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：先报根因代号，再报下一刀改哪里。")
