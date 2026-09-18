"""示例 3：带进面试的三张卡片——对应全书三份简历项目。"""

from catalog import FRAMEWORKS, NEEDS, rank, why_not, winner

RESUME = [
    ("项目1 企业知识库 Agent", "leave_kb"),
    ("项目2 多 Agent 协作（退款主路径）", "refund_88"),
    ("项目3 可观测生产 Agent（状态自建）", "refund_88"),
]


def card(title: str, need_id: str) -> None:
    need = next(n for n in NEEDS if n.id == need_id)
    pick = winner(need)
    print(f"【{title}】")
    print(f"  场景: {need.title} — {need.question}")
    print(f"  默认语言: {FRAMEWORKS[pick]}")
    print("  约束: HITL={0} 回边={1} 自建状态={2} 工期约{3}天".format(
        need.hitl, need.loop, need.own_state, need.days))
    others = [k for k, _ in rank(need)[1:3]]
    print("  不用: " + "；".join(f"{FRAMEWORKS[k]}（{why_not(need, k)}）" for k in others) + "…")
    print()


if __name__ == "__main__":
    print("林秋面试卡。项目3 仍选图：可观测要的是检查点和自建状态，不是再换圆桌。\n")
    for title, nid in RESUME:
        card(title, nid)
    print("换一句约束：三天演示里托管会加分，但 2026-08-26 日落后主链教具仍然更稳。")
    hack = next(n for n in NEEDS if n.id == "hackathon")
    print(f"hackathon 首选: {FRAMEWORKS[winner(hack)]}")
    print("读法：卡片随约束改。信仰式「我们全栈 LangGraph」不是工程决策。")
