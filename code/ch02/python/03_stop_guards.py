"""
第 2 章示例 3：终止条件如何挡住失控循环。

这里故意放一个「只会重复查余额」的思考器，展示 Runtime 的护栏：
最大步数、重复工具、超时。

    python 03_stop_guards.py
"""

from __future__ import annotations

import time

from runtime import AgentRuntime, Decision, Goal, Observation, StopPolicy, ToolRegistry


def check_leave_balance(user_id: str, days: int) -> Observation:
    return Observation(True, {"balance_ok": True, "leave_balance": 5}, f"{user_id} 余额 5，申请 {days} 天")


def sleepy_lookup(user_id: str) -> Observation:
    time.sleep(0.6)
    return Observation(True, {"profile_loaded": True}, f"已加载 {user_id} 的档案")


class LoopingPlanner:
    """反面教材：永远只查余额，从不创建工单。"""

    def decide(self, state) -> Decision:
        return Decision(
            "call_tool",
            "我再确认一次余额",
            "check_leave_balance",
            {"user_id": state.goal.user_id, "days": 3},
        )


class SlowThenLoopPlanner:
    def decide(self, state) -> Decision:
        if not state.slots.get("profile_loaded"):
            return Decision("call_tool", "先加载档案", "sleepy_lookup", {"user_id": state.goal.user_id})
        return Decision("call_tool", "再加载一次", "sleepy_lookup", {"user_id": state.goal.user_id})


def run_case(title: str, planner, policy: StopPolicy) -> None:
    print("=" * 60)
    print(title)
    tools = ToolRegistry()
    tools.register("check_leave_balance", check_leave_balance)
    tools.register("sleepy_lookup", sleepy_lookup)
    goal = Goal(
        description="请假三天",
        user_id="u1001",
        success_when=("ticket_created", "manager_notified"),
    )
    result = AgentRuntime(planner, tools, policy).run(goal)
    print(f"结果: {result.status} / {result.reason} / 步数={result.steps}\n")


if __name__ == "__main__":
    run_case(
        "案例 A：重复调用同一工具",
        LoopingPlanner(),
        StopPolicy(max_steps=8, timeout_seconds=5.0, max_same_tool_in_a_row=2),
    )
    run_case(
        "案例 B：超时（工具故意 sleep）",
        SlowThenLoopPlanner(),
        StopPolicy(max_steps=20, timeout_seconds=1.0, max_same_tool_in_a_row=99),
    )
    run_case(
        "案例 C：没有重复检测时，靠最大步数兜底",
        LoopingPlanner(),
        StopPolicy(max_steps=3, timeout_seconds=5.0, max_same_tool_in_a_row=99),
    )
