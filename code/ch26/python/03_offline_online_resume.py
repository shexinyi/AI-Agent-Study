"""示例 3：离线全量、线上抽样发现缺口；两张简历卡。"""

from kit import CASES, ONLINE_LOGS, gated
from minismith import run_experiment


def drift(logs: list[str], cases: list) -> list[str]:
    known = {c.question for c in cases}
    return [q for q in logs if q not in known]


BAD_RESUME = (
    "负责 Agent 评测，准确率 99%，使用 LangSmith 和 LLM-as-Judge，效果很好。"
)
GOOD_RESUME = (
    "冻结 7 条 Agent 金标（含退款 HITL、复合问法、拒答）；"
    "gated 对比 clever 通过率 100% vs 0，CI 在回归红灯时拦截；"
    "线上 10 条抽样发现年假 / ORDER-77 不在金标，已单独立项补例。"
)


if __name__ == "__main__":
    print("离线评测：金标全量，可重复。线上抽样：真实分布，会冒出金标没有的问法。")
    print("缺口要补进数据集，禁止用线上「感觉还行」替代离线回归。\n")

    offline = run_experiment("gated-offline", CASES, gated)
    unknown = drift(ONLINE_LOGS, CASES)
    print(f"离线通过率={offline.pass_rate:.0%}  条数={len(CASES)}")
    print(f"线上抽样={len(ONLINE_LOGS)}  金标外={len(unknown)}")
    for q in unknown:
        print(f"  漂移 {q}")
    print()
    print("错误简历：")
    print(" ", BAD_RESUME)
    print("正确简历：")
    print(" ", GOOD_RESUME)
    print()

    checks = [
        ("离线全过", offline.pass_rate == 1.0),
        ("抽到年假缺口", any("年假" in q for q in unknown)),
        ("抽到 ORDER-77 缺口", any("ORDER-77" in q for q in unknown)),
        ("错误卡有 99%", "99%" in BAD_RESUME),
        ("正确卡有对比和缺口", "100% vs 0" in GOOD_RESUME and "ORDER-77" in GOOD_RESUME),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：简历写尺子、对比、红灯条件和缺口，不写商标和 99%。")
