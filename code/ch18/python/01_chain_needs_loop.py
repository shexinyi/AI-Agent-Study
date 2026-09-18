"""示例 1：线性链遇坏 JSON 就死；图画一条重试边就能活。"""

from __future__ import annotations

import json
from typing import Annotated, TypedDict

import operator
from langgraph.graph import END, START, StateGraph

from kit import get_order


class S(TypedDict):
    question: str
    draft: str
    answer: str
    steps: int
    ok: bool
    trace: Annotated[list[str], operator.add]


def model_once(question: str, attempt: int) -> str:
    """第 1 次说人话，第 2 次才吐 JSON。用来对比链和循环。"""
    if attempt == 0:
        return "当然可以退，三天到账。"
    return '{"tool":"get_order","order_id":"ORDER-88"}'


def chain_once(question: str) -> str:
    raw = model_once(question, 0)
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        return f"链断了: 不是 JSON → {raw}"
    return json.dumps(get_order(payload["order_id"]), ensure_ascii=False)


def draft(state: S) -> dict:
    raw = model_once(state["question"], state.get("steps", 0))
    return {"draft": raw, "steps": state.get("steps", 0) + 1, "trace": [f"draft#{state.get('steps', 0)}"]}


def parse(state: S) -> dict:
    try:
        payload = json.loads(state["draft"])
        fact = get_order(payload["order_id"])
        return {"ok": True, "answer": json.dumps(fact, ensure_ascii=False), "trace": ["parse_ok"]}
    except (json.JSONDecodeError, KeyError):
        return {"ok": False, "answer": "【不足】解析失败", "trace": ["parse_fail"]}


def after_parse(state: S) -> str:
    if state.get("ok"):
        return "pass"
    if state.get("steps", 0) < 2:
        return "retry"
    return "fail"


def build():
    g = StateGraph(S)
    g.add_node("draft", draft)
    g.add_node("parse", parse)
    g.add_edge(START, "draft")
    g.add_edge("draft", "parse")
    g.add_conditional_edges("parse", after_parse, {"pass": END, "retry": "draft", "fail": END})
    return g.compile()


if __name__ == "__main__":
    q = "ORDER-88 能不能退"
    print("线性链没有回边，第一次不是 JSON 就结束。")
    print("链:", chain_once(q))

    app = build()
    print("\n图自己生成的结构:\n", app.get_graph().draw_mermaid())
    out = app.invoke({"question": q, "draft": "", "answer": "", "steps": 0, "ok": False, "trace": []})
    print("图:", out["answer"])
    print("轨迹:", out["trace"])
    print("\n读法：Agent 要循环。Chain 是一条管子，Graph 才能长出回边。")
