"""
第 3 章示例 2：Plan-and-Execute —— 先计划再执行。

先写下步骤清单，再按指针执行。若中途发现「已发货」，就改计划。
场景：ORDER-99 其实已经发出，初始计划按「未发货排查」来写，必须 replan。

    python 02_plan_and_execute.py
"""

from __future__ import annotations

from runtime import AgentRuntime, Decision, Goal, StopPolicy
from shop import build_tools, evaluate_resolution, new_db


DEFAULT_PLAN = ["get_order", "get_shipment", "get_inventory", "search_policy", "decide_action"]


class PlanAndExecutePlanner:
    def decide(self, state) -> Decision:
        order_id = state.goal.constraints["order_id"]
        s = state.slots

        if s.get("case_resolved"):
            return Decision("finish", "计划中的结案动作已完成")

        if not s.get("has_plan"):
            s["plan"] = list(DEFAULT_PLAN)
            s["plan_index"] = 0
            s["has_plan"] = True
            print(f"Plan: {s['plan']}")
            state.remember(f"Plan: {s['plan']}")

        if s.get("shipment_checked") and s.get("shipped") and not s.get("replanned"):
            s["plan"] = ["notify_in_transit"]
            s["plan_index"] = 0
            s["replanned"] = True
            return Decision(
                "call_tool",
                "观察推翻了「未发货」前提，废弃后续查库存/退款步骤，改计划为通知运单。",
                "notify_user",
                {"order_id": order_id, "message": f"包裹已发出，运单 {s.get('tracking')}"},
            )

        step_name = s["plan"][s["plan_index"]] if s["plan_index"] < len(s["plan"]) else "decide_action"
        s["plan_index"] += 1
        return self._execute_step(step_name, order_id, s)

    def _execute_step(self, step_name: str, order_id: str, s: dict) -> Decision:
        if step_name == "get_order":
            return Decision("call_tool", f"执行计划步骤 get_order", "get_order", {"order_id": order_id})
        if step_name == "get_shipment":
            return Decision("call_tool", f"执行计划步骤 get_shipment", "get_shipment", {"order_id": order_id})
        if step_name == "get_inventory":
            return Decision("call_tool", f"执行计划步骤 get_inventory", "get_inventory", {"sku": s["sku"]})
        if step_name == "search_policy":
            return Decision("call_tool", f"执行计划步骤 search_policy", "search_policy", {"topic": "缺货未发货"})
        if step_name == "notify_in_transit":
            return Decision(
                "call_tool",
                "执行改写后的计划：通知在途",
                "notify_user",
                {"order_id": order_id, "message": f"包裹已发出，运单 {s.get('tracking')}"},
            )
        if s.get("shipped"):
            return Decision(
                "call_tool",
                "计划收尾：已发货则通知",
                "notify_user",
                {"order_id": order_id, "message": f"包裹已发出，运单 {s.get('tracking')}"},
            )
        if s.get("stock") == 0 and s.get("days_waiting", 0) >= 3:
            return Decision("call_tool", "计划收尾：缺货超时则退款", "refund_order", {"order_id": order_id})
        return Decision("ask_human", "计划执行完仍无法决策")


if __name__ == "__main__":
    db = new_db()
    goal = Goal(
        description="用户投诉 ORDER-99 一直没发货，请按政策处理。",
        user_id="u1",
        success_when=("case_resolved",),
        constraints={"order_id": "ORDER-99"},
    )
    result = AgentRuntime(PlanAndExecutePlanner(), build_tools(db), StopPolicy()).run(goal)
    ok, note = evaluate_resolution("ORDER-99", result.slots, db)
    print(f"\nRunResult: {result.status} / 步数={result.steps}")
    print(f"是否改计划: {result.slots.get('replanned')}")
    print(f"最终计划: {result.slots.get('plan')}")
    print(f"台账退款: {db['refunds']}")
    print(f"政策评测: {ok} / {note}")
