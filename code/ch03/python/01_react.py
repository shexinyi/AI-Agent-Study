"""
第 3 章示例 1：ReAct —— 边想边做。

每一轮都根据最新 Observation 产生 Thought + Action。
场景：ORDER-88 已支付、未发货、库存为 0、已等待 4 天。

    python 01_react.py
"""

from __future__ import annotations

from runtime import AgentRuntime, Decision, Goal, StopPolicy
from shop import build_tools, evaluate_resolution, new_db


class ReactPlanner:
    def decide(self, state) -> Decision:
        order_id = state.goal.constraints["order_id"]
        s = state.slots

        if s.get("case_resolved"):
            return Decision("finish", "观察显示工单已结案，可以结束")

        if not s.get("order_loaded"):
            return Decision(
                "call_tool",
                "用户说一直没发货，先核对订单是否存在以及支付/发货状态。",
                "get_order",
                {"order_id": order_id},
            )

        if not s.get("shipment_checked"):
            return Decision(
                "call_tool",
                "订单在，但不能只信状态字段。下一步查物流，排除已经发出。",
                "get_shipment",
                {"order_id": order_id},
            )

        if s.get("shipped"):
            return Decision(
                "call_tool",
                "物流显示已在途，应告知运单，而不是退款。",
                "notify_user",
                {"order_id": order_id, "message": f"包裹已发出，运单 {s.get('tracking')}"},
            )

        if not s.get("inventory_checked"):
            return Decision(
                "call_tool",
                "确认未发货。接下来查库存，区分漏发和缺货。",
                "get_inventory",
                {"sku": s["sku"]},
            )

        if s.get("stock") == 0 and not s.get("policy_checked"):
            return Decision(
                "call_tool",
                "库存为 0，先查售后政策再决定退款还是让用户继续等。",
                "search_policy",
                {"topic": "缺货未发货"},
            )

        if s.get("stock") == 0 and s.get("days_waiting", 0) >= 3:
            return Decision(
                "call_tool",
                "政策允许：未发货且缺货等待超过 3 天，执行退款。",
                "refund_order",
                {"order_id": order_id},
            )

        return Decision("ask_human", "证据不足，交给人工客服")


if __name__ == "__main__":
    db = new_db()
    goal = Goal(
        description="用户投诉 ORDER-88 一直没发货，请按政策处理。",
        user_id="u1",
        success_when=("case_resolved",),
        constraints={"order_id": "ORDER-88"},
    )
    result = AgentRuntime(ReactPlanner(), build_tools(db), StopPolicy()).run(goal)
    ok, note = evaluate_resolution("ORDER-88", result.slots, db)
    print(f"\nRunResult: {result.status} / {result.reason} / 步数={result.steps}")
    print(f"台账退款: {db['refunds']}")
    print(f"政策评测: {ok} / {note}")
