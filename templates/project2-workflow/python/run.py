"""项目 2 最小演示：分格 + HITL。教学假设见 README。"""


def run(question: str, role: str, confirm: str | None = None) -> dict:
    slots = []
    executed: list[str] = []
    if "事假" in question:
        slots.append("leave:10 个工作日 / 3 天未超")
    if "ORDER-99" in question.upper():
        slots.append("order:已发货须售后")
    elif "ORDER-88" in question.upper():
        slots.append("order:未发货可退")
        if "退" in question:
            if role != "agent_ops":
                slots.append("hitl:拒绝 intern")
            elif confirm == "approve":
                executed.append("refund_order:ORDER-88")
                slots.append("hitl:已执行")
            else:
                slots.append("hitl:pending")
    return {"slots": slots, "executed": executed}


if __name__ == "__main__":
    q = "事假 3 天，ORDER-88 能退吗"
    intern = run(q, "intern")
    ops = run(q, "agent_ops")
    after = run("ORDER-99 退款", "agent_ops", "approve")
    print(intern)
    print(ops)
    print(after)
    assert intern["executed"] == [] and "leave:" in intern["slots"][0]
    assert "pending" in ops["slots"][-1]
    assert after["executed"] == []
    print("验收 OK（教学）")
