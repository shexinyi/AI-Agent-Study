"""示例 2：State + Node + 条件边。同一张图走请假 / 订单 / 拒答。"""

from __future__ import annotations

from typing import Annotated, TypedDict

import operator
from langgraph.graph import END, START, StateGraph

from kit import draft_from_facts, get_order, leave_fact, order_id_of, route_of


class S(TypedDict):
    question: str
    route: str
    facts: str
    answer: str
    trace: Annotated[list[str], operator.add]


def route(state: S) -> dict:
    r = route_of(state["question"])
    return {"route": r, "trace": [f"route:{r}"]}


def leave(state: S) -> dict:
    return {"facts": leave_fact(), "trace": ["node:leave"]}


def order(state: S) -> dict:
    fact = get_order(order_id_of(state["question"]))
    return {"facts": str(fact), "trace": [f"node:order:{fact.get('order_id')}"]}


def refuse(state: S) -> dict:
    return {"facts": "", "answer": "【不足】不是请假也不是订单。", "trace": ["node:refuse"]}


def draft(state: S) -> dict:
    if state.get("answer"):
        return {"trace": ["draft:skip"]}
    return {"answer": draft_from_facts(state.get("facts", "")), "trace": ["node:draft"]}


def after_route(state: S) -> str:
    return state["route"]


def build():
    g = StateGraph(S)
    g.add_node("route", route)
    g.add_node("leave", leave)
    g.add_node("order", order)
    g.add_node("refuse", refuse)
    g.add_node("draft", draft)
    g.add_edge(START, "route")
    g.add_conditional_edges("route", after_route, {"leave": "leave", "order": "order", "unknown": "refuse"})
    g.add_edge("leave", "draft")
    g.add_edge("order", "draft")
    g.add_edge("refuse", END)
    g.add_edge("draft", END)
    return g.compile()


if __name__ == "__main__":
    app = build()
    print("条件边把一张图分成三条腿，不是复制三条链。")
    for q in ("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单", "食堂几点开门"):
        out = app.invoke({"question": q, "route": "", "facts": "", "answer": "", "trace": []})
        print(f"\n问: {q}")
        print("轨迹:", " → ".join(out["trace"]))
        print("答:", out["answer"])
    print("\n读法：State 是黑板上的字。Node 擦写几格。Edge 决定下一支粉笔。")
