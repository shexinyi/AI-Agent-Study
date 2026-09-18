"""手写运行时：和 LangGraph 同一套 State / Node / 条件边 / 检查点。"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable


@dataclass
class Interrupt(Exception):
    value: Any


@dataclass
class Thread:
    state: dict
    nxt: str
    resume: Any = None
    log: list[str] = field(default_factory=list)


class MiniGraph:
    def __init__(self) -> None:
        self.nodes: dict[str, Callable] = {}
        self.edges: dict[str, str] = {}
        self.conds: dict[str, tuple[Callable, dict[str, str]]] = {}
        self.threads: dict[str, Thread] = {}
        self.start = "start"

    def add_node(self, name: str, fn: Callable) -> MiniGraph:
        self.nodes[name] = fn
        return self

    def add_edge(self, src: str, dst: str) -> MiniGraph:
        self.edges[src] = dst
        return self

    def add_cond(self, src: str, fn: Callable, mapping: dict[str, str]) -> MiniGraph:
        self.conds[src] = (fn, mapping)
        return self

    def invoke(self, state: dict, thread_id: str, resume: Any = None) -> dict:
        th = self.threads.get(thread_id)
        if th is None:
            th = Thread(dict(state), self.edges.get("start", "start"))
            if "start" in self.nodes:
                th.nxt = "start"
            elif "route" in self.nodes:
                th.nxt = "route"
        if resume is not None:
            th.resume = resume
        self.threads[thread_id] = th

        while th.nxt and th.nxt != "end":
            fn = self.nodes[th.nxt]
            try:
                delta = fn(th.state, th.resume)
            except Interrupt as hit:
                th.log.append(f"interrupt@{th.nxt}")
                return {**th.state, "__interrupt__": [hit.value], "trace": th.state.get("trace", []) + th.log}
            th.resume = None
            if isinstance(delta, Interrupt):
                th.log.append(f"interrupt@{th.nxt}")
                return {**th.state, "__interrupt__": [delta.value], "trace": th.state.get("trace", []) + th.log}
            for k, v in delta.items():
                if k == "trace":
                    th.state["trace"] = th.state.get("trace", []) + v
                else:
                    th.state[k] = v
            if th.nxt in self.conds:
                pred, mapping = self.conds[th.nxt]
                th.nxt = mapping[pred(th.state)]
            else:
                th.nxt = self.edges.get(th.nxt, "end")
        return th.state
