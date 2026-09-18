"""
第 3 章示例 3：Reflexion —— 做完再反思，带着教训重来。

第一次用「naive」规划器：不查物流，直接退款。
外层评测器判定政策失败，写出 lesson，第二次换「听话」的规划器。

场景：ORDER-99 已发货。Goal 第一次也会 success，但评测会打回。

    python 03_reflexion.py
"""

from __future__ import annotations

from runtime import AgentRuntime, Decision, Goal, RunResult, StopPolicy
from shop import build_tools, evaluate_resolution, new_db


class NaivePlanner:
    """反面教材：看到投诉就退款。"""

    def decide(self, state) -> Decision:
        if state.slots.get("case_resolved"):
            return Decision("finish", "已经退款，自认为做完了")
        order_id = state.goal.constraints["order_id"]
        return Decision(
            "call_tool",
            "用户很着急，直接退款安抚，不必查物流。",
            "refund_order",
            {"order_id": order_id},
        )


class LessonAwarePlanner:
    def __init__(self, lessons: list[str]) -> None:
        self.lessons = lessons

    def decide(self, state) -> Decision:
        order_id = state.goal.constraints["order_id"]
        s = state.slots
        if s.get("case_resolved"):
            return Decision("finish", "本轮已结案")
        if not s.get("order_loaded"):
            return Decision("call_tool", f"教训在耳边：{self.lessons[-1]} 先加载订单。", "get_order", {"order_id": order_id})
        if not s.get("shipment_checked"):
            return Decision(
                "call_tool",
                "教训要求：退款前必须查看物流。",
                "get_shipment",
                {"order_id": order_id},
            )
        if s.get("shipped"):
            return Decision(
                "call_tool",
                "已发货，按教训改为通知运单，禁止退款。",
                "notify_user",
                {"order_id": order_id, "message": f"包裹已发出，运单 {s.get('tracking')}"},
            )
        return Decision("call_tool", "未发货才允许走退款。", "refund_order", {"order_id": order_id})


def reflect(result: RunResult, eval_note: str) -> str:
    if "已发货订单被退款" in eval_note:
        return "退款前必须调用 get_shipment；若 shipped=true，只能 notify_user，禁止 refund_order。"
    return f"上一轮失败：{eval_note} / {result.reason}"


def run_with_reflexion(order_id: str, max_trials: int = 2) -> None:
    lessons: list[str] = []
    for trial in range(1, max_trials + 1):
        db = new_db()
        goal = Goal(
            description=f"用户投诉 {order_id} 一直没发货，请按政策处理。",
            user_id="u1",
            success_when=("case_resolved",),
            constraints={"order_id": order_id, "lessons": list(lessons)},
        )
        planner = LessonAwarePlanner(lessons) if lessons else NaivePlanner()
        print("=" * 60)
        print(f"第 {trial} 次尝试 / 已有教训: {lessons or '无'}")
        result = AgentRuntime(planner, build_tools(db), StopPolicy()).run(goal)
        ok, note = evaluate_resolution(order_id, result.slots, db)
        print(f"\nRunResult: {result.status}  台账退款: {db['refunds']}")
        print(f"政策评测: {ok} / {note}")
        if ok:
            print("Reflexion 结束：评测通过。")
            return
        lesson = reflect(result, note)
        lessons.append(lesson)
        print(f"Reflection: {lesson}")
    print("达到最大尝试次数，仍未通过评测。")


if __name__ == "__main__":
    run_with_reflexion("ORDER-99")
