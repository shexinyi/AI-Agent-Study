"""
第 2 章示例 2：用 Runtime 办理一张请假单。

思考器仍是规则，重点看组件如何拆开：Goal / Planner / Tools / Memory / StopPolicy。

    python 02_leave_agent.py
"""

from __future__ import annotations

from runtime import AgentRuntime, Decision, Goal, Observation, Planner, StopPolicy, ToolRegistry

HR_DB: dict[str, dict] = {
    "u1001": {"name": "林秋", "leave_balance": 5, "tickets": [], "manager": "m2001"},
}


def parse_leave_request(user_text: str) -> Observation:
    if "周一" in user_text and "周三" in user_text:
        data = {
            "parsed": True,
            "start_date": "2026-09-14",
            "end_date": "2026-09-16",
            "days": 3,
            "leave_type": "事假",
        }
        return Observation(True, data, "解析成功：2026-09-14 至 2026-09-16，共 3 天事假")
    return Observation(False, {"parsed": False}, "无法解析请假日期，需要用户补充")


def check_leave_balance(user_id: str, days: int) -> Observation:
    user = HR_DB.get(user_id)
    if user is None:
        return Observation(False, {}, f"用户不存在: {user_id}")
    enough = user["leave_balance"] >= days
    return Observation(
        True,
        {"balance_ok": enough, "leave_balance": user["leave_balance"]},
        f"剩余 {user['leave_balance']} 天，申请 {days} 天，{'通过' if enough else '不足'}",
    )


def create_leave_ticket(user_id: str, start_date: str, end_date: str, days: int) -> Observation:
    user = HR_DB[user_id]
    ticket_id = f"L-{start_date.replace('-', '')}"
    user["tickets"].append(ticket_id)
    user["leave_balance"] -= days
    return Observation(
        True,
        {"ticket_created": True, "ticket_id": ticket_id},
        f"已创建工单 {ticket_id}，余额变为 {user['leave_balance']}",
    )


def notify_manager(user_id: str, ticket_id: str) -> Observation:
    manager = HR_DB[user_id]["manager"]
    return Observation(
        True,
        {"manager_notified": True, "manager_id": manager},
        f"已通知经理 {manager}，待审批工单 {ticket_id}",
    )


class LeavePlanner:
    def decide(self, state) -> Decision:
        slots = state.slots
        if state.goal.is_satisfied(slots):
            return Decision("finish", "请假单已创建且经理已收到通知")
        if not slots.get("parsed"):
            return Decision(
                "call_tool",
                "先把自然语言变成结构化槽位",
                "parse_leave_request",
                {"user_text": state.goal.description},
            )
        if "balance_ok" not in slots:
            return Decision(
                "call_tool",
                "核对剩余假期是否足够",
                "check_leave_balance",
                {"user_id": state.goal.user_id, "days": slots["days"]},
            )
        if slots.get("balance_ok") is False:
            return Decision("ask_human", "余额不足，需要主管特批或缩短天数")
        if not slots.get("ticket_created"):
            return Decision(
                "call_tool",
                "余额足够，创建请假工单",
                "create_leave_ticket",
                {
                    "user_id": state.goal.user_id,
                    "start_date": slots["start_date"],
                    "end_date": slots["end_date"],
                    "days": slots["days"],
                },
            )
        return Decision(
            "call_tool",
            "工单已在，通知经理",
            "notify_manager",
            {"user_id": state.goal.user_id, "ticket_id": slots["ticket_id"]},
        )


def build_runtime() -> AgentRuntime:
    tools = ToolRegistry()
    tools.register("parse_leave_request", parse_leave_request)
    tools.register("check_leave_balance", check_leave_balance)
    tools.register("create_leave_ticket", create_leave_ticket)
    tools.register("notify_manager", notify_manager)
    return AgentRuntime(LeavePlanner(), tools, StopPolicy(max_steps=8, timeout_seconds=3.0))


if __name__ == "__main__":
    goal = Goal(
        description="我是林秋，下周一到周三请假去上海办事，帮我走流程。",
        user_id="u1001",
        success_when=("ticket_created", "manager_notified"),
        constraints={"max_days_without_approval": 3},
    )
    result = build_runtime().run(goal)
    print(f"\n状态: {result.status}")
    print(f"原因: {result.reason}")
    print(f"槽位: {result.slots}")
    print(f"HR 工单: {HR_DB['u1001']['tickets']}，余额 {HR_DB['u1001']['leave_balance']}")
