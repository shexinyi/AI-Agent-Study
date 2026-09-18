"""自建 Runtime：状态在你这边。对照第 2 章对象，不依赖供应商 Thread。"""

from __future__ import annotations

ORDERS = {
    "ORDER-88": "ORDER-88 未发货，可整单退款。写退款须人工确认。",
    "ORDER-99": "ORDER-99 已发货，物流 TZ123，须走售后。",
}


def get_order(order_id: str) -> str:
    return ORDERS.get(order_id, f"{order_id} 查无此单。")


def answer(question: str) -> dict:
    session = {"question": question, "tool": None, "answer": ""}
    if "食堂" in question or "年假" in question:
        session["answer"] = "【不足】资料不够。"
        return session
    if any(k in question for k in ("事假", "请假", "工作日")):
        session["answer"] = "根据制度，事假每年累计不超过 10 个工作日。"
        return session
    oid = "ORDER-99" if ("TZ123" in question.upper() or "ORDER-99" in question.upper()) else "ORDER-88"
    if "ORDER" in question.upper() or "TZ" in question.upper() or "退" in question:
        session["tool"] = get_order(oid)
        if "未发货" in session["tool"]:
            session["answer"] = "根据工具，ORDER-88 未发货可退。写工具前须人工确认。"
        else:
            session["answer"] = "根据工具，TZ123 对应 ORDER-99，已发货须走售后。"
        return session
    session["answer"] = "【不足】资料不够。"
    return session
