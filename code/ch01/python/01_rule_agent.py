"""
第 1 章示例 1：不调用大模型的规则 Agent。

它演示 Agent 的最小循环：感知 -> 思考 -> 行动 -> 观察，直到目标完成或步数用尽。
无需 API Key，Python 3.10+ 即可运行：

    python 01_rule_agent.py
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable


@dataclass
class Observation:
    """一次工具执行后回到 Agent 手里的观察结果。"""

    ok: bool
    data: dict[str, Any]
    message: str


@dataclass
class AgentState:
    goal: str
    budget_cny: int
    step: int = 0
    max_steps: int = 8
    memory: list[str] = field(default_factory=list)
    booked_flight: dict[str, Any] | None = None
    booked_transfer: dict[str, Any] | None = None
    finished: bool = False


def search_flights(date: str, dest: str) -> Observation:
    catalog = [
        {"flight_no": "MU5101", "date": date, "dest": dest, "price": 680, "seats": 4},
        {"flight_no": "CA1831", "date": date, "dest": dest, "price": 920, "seats": 1},
        {"flight_no": "9C8801", "date": date, "dest": dest, "price": 450, "seats": 0},
    ]
    available = [item for item in catalog if item["seats"] > 0]
    return Observation(True, {"flights": available}, f"找到 {len(available)} 个有票航班")


def book_flight(flight_no: str, budget_cny: int) -> Observation:
    prices = {"MU5101": 680, "CA1831": 920, "9C8801": 450}
    if flight_no not in prices:
        return Observation(False, {}, f"航班不存在: {flight_no}")
    price = prices[flight_no]
    if flight_no == "9C8801":
        return Observation(False, {}, "该航班已售罄")
    if price > budget_cny:
        return Observation(False, {}, f"价格 {price} 超出预算 {budget_cny}")
    return Observation(
        True,
        {"flight_no": flight_no, "price": price},
        f"已预订 {flight_no}，花费 {price} 元",
    )


def book_transfer(flight_no: str) -> Observation:
    if not flight_no:
        return Observation(False, {}, "没有航班信息，无法安排接机")
    return Observation(
        True,
        {"car": "沪A·D328", "pickup": "虹桥T2", "flight_no": flight_no},
        f"已为 {flight_no} 安排接机：沪A·D328，虹桥T2",
    )


TOOLS: dict[str, Callable[..., Observation]] = {
    "search_flights": search_flights,
    "book_flight": book_flight,
    "book_transfer": book_transfer,
}


def think(state: AgentState) -> dict[str, Any]:
    """
    思考器：根据当前记忆决定下一步。

    这里用明确规则代替 LLM，方便你先看懂循环本身。
    第 3–4 章会把这个函数换成大模型 + 工具协议。
    """
    if state.booked_flight and state.booked_transfer:
        return {"type": "finish", "reason": "机票和接机都已办妥"}

    if state.booked_flight and not state.booked_transfer:
        return {
            "type": "tool",
            "name": "book_transfer",
            "args": {"flight_no": state.booked_flight["flight_no"]},
        }

    searched = any(item.startswith("观察[search_flights]") for item in state.memory)
    if not searched:
        return {
            "type": "tool",
            "name": "search_flights",
            "args": {"date": "2026-09-14", "dest": "SHA"},
        }

    return {
        "type": "tool",
        "name": "book_flight",
        "args": {"flight_no": "MU5101", "budget_cny": state.budget_cny},
    }


def run_agent(goal: str, budget_cny: int = 800) -> AgentState:
    state = AgentState(goal=goal, budget_cny=budget_cny)
    print(f"目标: {state.goal}")
    print(f"预算: {state.budget_cny} 元\n")

    while not state.finished and state.step < state.max_steps:
        state.step += 1
        decision = think(state)
        print(f"第 {state.step} 步思考 -> {decision}")

        if decision["type"] == "finish":
            state.finished = True
            state.memory.append(f"结束: {decision['reason']}")
            print(f"结束: {decision['reason']}")
            break

        tool_name = decision["name"]
        tool_args = decision["args"]
        observation = TOOLS[tool_name](**tool_args)
        state.memory.append(f"观察[{tool_name}]: {observation.message}")
        print(f"行动: {tool_name}{tool_args}")
        print(f"观察: {observation.message}\n")

        if tool_name == "book_flight" and observation.ok:
            state.booked_flight = observation.data
        if tool_name == "book_transfer" and observation.ok:
            state.booked_transfer = observation.data

    if not state.finished:
        print("触发终止条件: 达到最大步数，避免死循环。")
    return state


if __name__ == "__main__":
    final_state = run_agent("预订明天去上海的机票，并安排接机")
    print("最终记忆:")
    for line in final_state.memory:
        print(f"- {line}")
