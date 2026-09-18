"""林秋世界：只读自动，写工具须人点头。"""

from __future__ import annotations

from minihitl import MONEY, READ, WRITE, Action

ORDERS = {
    "ORDER-88": "ORDER-88 未发货，可整单退款。写退款须人工确认。",
    "ORDER-99": "ORDER-99 已发货，物流 TZ123，须走售后。",
}
LEAVE = "事假每年累计不超过 10 个工作日。"


def get_order(order_id: str) -> Action:
    return Action("get_order", {"order_id": order_id}, READ)


def search_policy() -> Action:
    return Action("search_policy", {}, READ)


def refund(order_id: str) -> Action:
    return Action("refund_order", {"order_id": order_id}, MONEY)


def create_leave() -> Action:
    return Action("create_leave", {"who": "林秋", "days": 3}, WRITE)


def expect_ok(text: str, tag: str) -> bool:
    if tag == "executed_88":
        return "已执行 refund_order(ORDER-88)" in text
    if tag == "idempotent":
        return "【幂等】" in text
    if tag == "rejected":
        return "【已拒绝】" in text
    if tag == "timed_out":
        return "【超时关闭】" in text
    if tag == "escalated":
        return "【已升级】" in text
    if tag == "takeover":
        return "【已接管】" in text
    if tag == "need_approve":
        return "仍须审批" in text
    if tag == "no_exec":
        return "已执行 refund_order" not in text
    if tag == "policy":
        return "资金动作禁止 fail_open" in text
    return False
