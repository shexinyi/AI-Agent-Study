"""第 3 章沿用第 2 章 Runtime：只换 Planner，不改循环。"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any, Callable, Literal, Protocol

DecisionKind = Literal["call_tool", "finish", "fail", "ask_human"]


@dataclass(frozen=True)
class Goal:
    description: str
    user_id: str
    success_when: tuple[str, ...]
    constraints: dict[str, Any] = field(default_factory=dict)

    def is_satisfied(self, slots: dict[str, Any]) -> bool:
        return all(slots.get(flag) is True for flag in self.success_when)


@dataclass
class Observation:
    ok: bool
    data: dict[str, Any]
    message: str


@dataclass
class Decision:
    kind: DecisionKind
    reason: str
    tool_name: str | None = None
    tool_args: dict[str, Any] = field(default_factory=dict)


@dataclass
class StopPolicy:
    max_steps: int = 12
    timeout_seconds: float = 5.0
    max_same_tool_in_a_row: int = 4


@dataclass
class AgentState:
    goal: Goal
    slots: dict[str, Any] = field(default_factory=dict)
    events: list[str] = field(default_factory=list)
    tool_trace: list[str] = field(default_factory=list)
    step: int = 0
    started_at: float = field(default_factory=time.monotonic)

    def remember(self, line: str) -> None:
        self.events.append(line)


@dataclass
class RunResult:
    status: Literal["success", "failed", "timeout", "max_steps", "need_human", "repeated_tool"]
    reason: str
    slots: dict[str, Any]
    events: list[str]
    steps: int


class Planner(Protocol):
    def decide(self, state: AgentState) -> Decision:
        ...


class ToolRegistry:
    def __init__(self) -> None:
        self._tools: dict[str, Callable[..., Observation]] = {}

    def register(self, name: str, fn: Callable[..., Observation]) -> None:
        self._tools[name] = fn

    def invoke(self, name: str, args: dict[str, Any]) -> Observation:
        if name not in self._tools:
            return Observation(False, {}, f"未知工具: {name}")
        return self._tools[name](**args)


class AgentRuntime:
    def __init__(self, planner: Planner, tools: ToolRegistry, policy: StopPolicy | None = None) -> None:
        self.planner = planner
        self.tools = tools
        self.policy = policy or StopPolicy()

    def run(self, goal: Goal) -> RunResult:
        state = AgentState(goal=goal)
        state.remember(f"接收目标: {goal.description}")
        while True:
            guard = self._check_guards(state)
            if guard is not None:
                return guard
            state.step += 1
            decision = self.planner.decide(state)
            print(f"Thought: {decision.reason}")
            state.remember(f"Thought: {decision.reason}")
            if decision.kind == "finish":
                if goal.is_satisfied(state.slots):
                    return self._result("success", decision.reason, state)
                return self._result("failed", "思考器宣布完成，但成功条件未满足", state)
            if decision.kind == "fail":
                return self._result("failed", decision.reason, state)
            if decision.kind == "ask_human":
                return self._result("need_human", decision.reason, state)
            assert decision.tool_name
            state.tool_trace.append(decision.tool_name)
            print(f"Action: {decision.tool_name}{decision.tool_args}")
            observation = self.tools.invoke(decision.tool_name, decision.tool_args)
            print(f"Observation: {observation.message}")
            state.remember(f"Observation[{decision.tool_name}]: {observation.message}")
            self._merge_slots(state, observation)

    def _check_guards(self, state: AgentState) -> RunResult | None:
        if time.monotonic() - state.started_at > self.policy.timeout_seconds:
            return self._result("timeout", f"超过 {self.policy.timeout_seconds}s", state)
        if state.step >= self.policy.max_steps:
            return self._result("max_steps", f"达到最大步数 {self.policy.max_steps}", state)
        if self._same_tool_streak(state) >= self.policy.max_same_tool_in_a_row:
            return self._result("repeated_tool", f"连续重复调用 {state.tool_trace[-1]}", state)
        return None

    def _same_tool_streak(self, state: AgentState) -> int:
        if not state.tool_trace:
            return 0
        last = state.tool_trace[-1]
        streak = 0
        for name in reversed(state.tool_trace):
            if name != last:
                break
            streak += 1
        return streak

    @staticmethod
    def _merge_slots(state: AgentState, observation: Observation) -> None:
        if not observation.ok:
            state.slots["last_error"] = observation.message
            return
        state.slots.pop("last_error", None)
        state.slots.update(observation.data)

    @staticmethod
    def _result(
        status: Literal["success", "failed", "timeout", "max_steps", "need_human", "repeated_tool"],
        reason: str,
        state: AgentState,
    ) -> RunResult:
        return RunResult(status, reason, dict(state.slots), list(state.events), state.step)
