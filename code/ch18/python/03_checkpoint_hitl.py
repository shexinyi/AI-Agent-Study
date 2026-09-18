"""示例 3：写工具前打断；检查点按 thread_id 恢复。对照手写 MiniGraph。"""

from __future__ import annotations

from typing import Annotated, TypedDict

import operator
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.types import Command, interrupt

from kit import get_order, order_id_of, paused
from mini_runtime import Interrupt, MiniGraph


class S(TypedDict):
    question: str
    facts: str
    answer: str
    approved: bool
    trace: Annotated[list[str], operator.add]


def fetch(state: S) -> dict:
    fact = get_order(order_id_of(state["question"]))
    return {"facts": str(fact), "trace": [f"fetch:{fact.get('order_id')}"]}


def gate(state: S) -> dict:
    if "unshipped" not in state.get("facts", ""):
        return {"answer": "已发货须走售后，不调用写工具。", "approved": False, "trace": ["gate:skip"]}
    decision = interrupt({"action": "refund_order", "order_id": "ORDER-88", "need": "人工确认"})
    if str(decision).strip() in {"确认", "yes", "Y"}:
        return {"answer": "已执行 refund_order(ORDER-88)。", "approved": True, "trace": ["gate:ok"]}
    return {"answer": "用户拒绝，未退款。", "approved": False, "trace": ["gate:deny"]}


def build():
    g = StateGraph(S)
    g.add_node("fetch", fetch)
    g.add_node("gate", gate)
    g.add_edge(START, "fetch")
    g.add_edge("fetch", "gate")
    g.add_edge("gate", END)
    return g.compile(checkpointer=InMemorySaver())


def handwritten_gate(state: dict, resume):
    if "unshipped" not in state.get("facts", ""):
        return {"answer": "已发货须走售后，不调用写工具。", "approved": False, "trace": ["gate:skip"]}
    if resume is None:
        raise Interrupt({"action": "refund_order", "order_id": "ORDER-88"})
    if str(resume).strip() in {"确认", "yes", "Y"}:
        return {"answer": "已执行 refund_order(ORDER-88)。", "approved": True, "trace": ["gate:ok"]}
    return {"answer": "用户拒绝，未退款。", "approved": False, "trace": ["gate:deny"]}


def handwritten_fetch(state: dict, resume):
    fact = get_order(order_id_of(state["question"]))
    return {"facts": str(fact), "trace": [f"fetch:{fact.get('order_id')}"]}


def build_hand():
    g = MiniGraph()
    g.add_node("fetch", handwritten_fetch)
    g.add_node("gate", handwritten_gate)
    g.add_edge("start", "fetch")
    g.add_edge("fetch", "gate")
    g.add_edge("gate", "end")
    return g


if __name__ == "__main__":
    app = build()
    seed = {"question": "ORDER-88 能不能退", "facts": "", "answer": "", "approved": False, "trace": []}
    cfg88 = {"configurable": {"thread_id": "linqiu-order88"}}
    cfg99 = {"configurable": {"thread_id": "linqiu-order99"}}

    print("thread linqiu-order88：未发货退款，必须停在人工节点。")
    paused88 = app.invoke(seed, cfg88)
    print("第一次:", "PAUSED" if paused(paused88) else paused88.get("answer"), paused88.get("__interrupt__"))
    snap = app.get_state(cfg88)
    print("检查点 next=", snap.next, " thread_id=linqiu-order88")

    done = app.invoke(Command(resume="确认"), cfg88)
    print("确认后:", done["answer"], "approved=", done["approved"])

    print("\nthread linqiu-order99：已发货，不打断。")
    seed99 = {**seed, "question": "TZ123 那张单能直接退吗"}
    print("99:", app.invoke(seed99, cfg99)["answer"])

    print("\n--- 同一套节点，手写 MiniGraph ---")
    hand = build_hand()
    p = hand.invoke(dict(seed), "hand-88")
    print("手写第一次:", "PAUSED" if p.get("__interrupt__") else p.get("answer"))
    d = hand.invoke(dict(seed), "hand-88", resume="确认")
    print("手写确认后:", d["answer"])
    print("\n读法：LangGraph 的价值是检查点协议，不是把 if/else 画得更好看。")
    print("恢复时 gate 节点会从头再跑一遍，所以 fetch 必须放在打断前面、并且可重入。")
