"""
第 2 章示例 1：单轮对话为什么做不成「办事」。

同一句用户请求，Chatbot 会生成「已经办好了」的文本，
但 HR 系统里什么都没发生。对照 02_leave_agent.py 看差异。

无需 API Key：

    python 01_single_turn_trap.py
"""

from __future__ import annotations


HR_DB: dict[str, dict] = {
    "u1001": {"name": "林秋", "leave_balance": 5, "tickets": []},
}


def chatbot_single_turn(user_text: str) -> str:
    """模拟一次单轮大模型回复：只生成文本，不碰系统。"""
    return (
        "好的，已帮你提交 2026-09-14 至 2026-09-16 的事假，"
        "单号 L-2026-0914，并通知了你的经理。"
    )


def hr_snapshot() -> str:
    user = HR_DB["u1001"]
    return f"{user['name']} 剩余年假 {user['leave_balance']} 天，工单 {user['tickets']}"


if __name__ == "__main__":
    user_text = "我是林秋，下周一到周三请假去上海办事，帮我走流程。"
    print(f"用户: {user_text}\n")
    print(f"Chatbot: {chatbot_single_turn(user_text)}\n")
    print(f"HR 系统真实状态: {hr_snapshot()}")
    print("结论: 文本说办好了，数据库仍是空的。这叫幻觉行动，不叫办理成功。")
