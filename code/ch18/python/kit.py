"""订单事实 + 教学路由。模型仍是脚本，不是 API。"""

from __future__ import annotations

import json

ORDERS = {
    "ORDER-88": {"status": "unshipped", "refundable": True, "tracking": None},
    "ORDER-99": {"status": "shipped", "refundable": False, "tracking": "TZ123"},
}


def route_of(question: str) -> str:
    q = question.upper()
    if any(k in question for k in ("事假", "请假", "工作日")):
        return "leave"
    if "ORDER-" in q or "TZ" in q or "退" in question:
        return "order"
    return "unknown"


def get_order(order_id: str) -> dict:
    return {"order_id": order_id, **ORDERS[order_id]} if order_id in ORDERS else {"error": "not_found"}


def order_id_of(question: str) -> str:
    if "TZ123" in question.upper() or "ORDER-99" in question.upper():
        return "ORDER-99"
    return "ORDER-88"


def leave_fact() -> str:
    return "事假每年累计不超过 10 个工作日。"


def draft_from_facts(facts: str) -> str:
    if "10 个工作日" in facts:
        return "根据资料，事假每年累计不超过 10 个工作日。"
    if facts.get("refundable") if isinstance(facts, dict) else False:
        return "根据工具，该单未发货，可申请整单退款。"
    if isinstance(facts, dict) and facts.get("status") == "shipped":
        return "根据工具，该单已发货，对话里不能直接退款，须走售后。"
    text = facts if isinstance(facts, str) else json.dumps(facts, ensure_ascii=False)
    if "unshipped" in text or "refundable\": true" in text:
        return "根据工具，该单未发货，可申请整单退款。"
    if "shipped" in text:
        return "根据工具，该单已发货，对话里不能直接退款，须走售后。"
    return "【不足】资料不够，不能编天数或单号状态。"


def paused(result: dict) -> bool:
    return bool(result.get("__interrupt__"))
