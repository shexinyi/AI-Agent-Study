"""教学评分卡。权重是课堂约定，不是基准测试结论。"""

from __future__ import annotations

from dataclasses import dataclass

FRAMEWORKS = {
    "chain": "LangChain 主链",
    "graph": "LangGraph 状态图",
    "chat": "AutoGen 圆桌",
    "crew": "CrewAI 排班",
    "hosted": "托管 Assistants/Responses",
}


@dataclass
class Need:
    id: str
    title: str
    question: str
    hitl: bool
    loop: bool
    roles: bool
    talk: bool
    days: int
    own_state: bool
    java_ok: bool


NEEDS = [
    Need("leave_kb", "企业知识库问答", "事假上限几天", False, False, False, False, 10, True, True),
    Need("refund_88", "未发货退款", "ORDER-88 能不能退", True, True, False, False, 20, True, True),
    Need("debate", "制度评审对打", "旧稿 5 天还要不要写进回复", False, False, True, True, 7, True, True),
    Need("pipeline", "固定工种出稿", "检索制度再写一句可引用回答", False, False, True, False, 5, True, True),
    Need("hackathon", "三天供应商演示", "先让老板看见能对话", False, False, False, False, 3, False, False),
]


def score(need: Need) -> dict[str, int]:
    """每项 0–4 的课堂分，总和用来排序。"""
    s = {k: 0 for k in FRAMEWORKS}
    s["chain"] += 3 if not need.hitl and not need.talk else 1
    s["graph"] += 4 if need.hitl or need.loop else 1
    s["chat"] += 4 if need.talk else 0
    s["crew"] += 4 if need.roles and not need.talk and not need.hitl else (2 if need.roles else 0)
    s["hosted"] += 3 if need.days <= 3 and not need.own_state else 0
    if need.own_state:
        s["hosted"] -= 3
        s["graph"] += 1
        s["chain"] += 1
    if need.days <= 3:
        s["chain"] += 2
        s["hosted"] += 1
        s["graph"] -= 1
    if need.java_ok:
        s["chat"] -= 1
        s["crew"] -= 1
    if need.id == "leave_kb":
        s["chain"] += 2
    if need.id == "refund_88":
        s["graph"] += 2
        s["chat"] -= 2
        s["crew"] -= 1
    if need.id == "pipeline":
        s["crew"] += 3
        s["chain"] -= 1
    return s


def why_not(need: Need, key: str) -> str:
    if key == "graph" and not need.hitl and not need.loop:
        return "没有确认和回边，图画多了。"
    if key == "hosted" and need.days <= 3:
        return "Assistants 已日落；三天内与其赌托管，不如主链加脚本模型先让人看见。"
    if key == "crew" and need.id == "leave_kb":
        return "单次问答不必排班，角色合同是多余工单。"
    return WHY_NOT[key]


def rank(need: Need) -> list[tuple[str, int]]:
    return sorted(score(need).items(), key=lambda x: (-x[1], x[0]))


def winner(need: Need) -> str:
    return rank(need)[0][0]


WHY_NOT = {
    "chain": "管子没有回边和人工节点，写工具前停不下来。",
    "graph": "要把确认、回边画成节点；三天演示偏重。",
    "chat": "下一句谁开口不稳定，退款和权限不该靠抢麦。",
    "crew": "顺序流会绑死工种；层级经理会指派错，且不自带 HITL。",
    "hosted": "状态在供应商家。Assistants 已于 2026-08-26 日落，只存 id 等于没有备份。",
}
