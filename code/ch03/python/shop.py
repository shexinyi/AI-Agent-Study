"""售后工单用的假系统：订单、库存、物流、退款台账。"""

from __future__ import annotations

from copy import deepcopy
from typing import Any

from runtime import Observation, ToolRegistry

SEED: dict[str, Any] = {
    "orders": {
        "ORDER-88": {
            "user_id": "u1",
            "status": "paid",
            "shipped": False,
            "sku": "SKU-9",
            "days_waiting": 4,
        },
        "ORDER-99": {
            "user_id": "u1",
            "status": "paid",
            "shipped": True,
            "sku": "SKU-9",
            "tracking": "TZ123",
            "days_waiting": 1,
        },
    },
    "inventory": {"SKU-9": 0},
    "refunds": [],
}


def new_db() -> dict[str, Any]:
    return deepcopy(SEED)


def build_tools(db: dict[str, Any]) -> ToolRegistry:
    tools = ToolRegistry()

    def get_order(order_id: str) -> Observation:
        order = db["orders"].get(order_id)
        if not order:
            return Observation(False, {}, f"订单不存在: {order_id}")
        data = {"order_loaded": True, "order_id": order_id, **order}
        return Observation(True, data, f"订单 {order_id}: 已支付, 已发货={order['shipped']}, SKU={order['sku']}")

    def get_shipment(order_id: str) -> Observation:
        order = db["orders"][order_id]
        shipped = bool(order.get("shipped"))
        tracking = order.get("tracking")
        return Observation(
            True,
            {"shipment_checked": True, "shipped": shipped, "tracking": tracking},
            f"物流: {'在途 ' + tracking if shipped else '尚未发出'}",
        )

    def get_inventory(sku: str) -> Observation:
        stock = db["inventory"].get(sku, 0)
        return Observation(True, {"inventory_checked": True, "sku": sku, "stock": stock}, f"{sku} 库存={stock}")

    def search_policy(topic: str) -> Observation:
        text = "未发货且缺货等待超过 3 天，可退款或协商换货。已发货订单禁止直接退款，应提供运单并引导催件。"
        return Observation(True, {"policy_checked": True, "policy": text}, text)

    def refund_order(order_id: str) -> Observation:
        db["refunds"].append(order_id)
        return Observation(
            True,
            {"refunded": True, "case_resolved": True, "resolution": "refund"},
            f"已退款 {order_id}",
        )

    def notify_user(order_id: str, message: str) -> Observation:
        return Observation(
            True,
            {"notified": True, "case_resolved": True, "resolution": "notify", "notify_message": message},
            f"已通知用户: {message}",
        )

    tools.register("get_order", get_order)
    tools.register("get_shipment", get_shipment)
    tools.register("get_inventory", get_inventory)
    tools.register("search_policy", search_policy)
    tools.register("refund_order", refund_order)
    tools.register("notify_user", notify_user)
    return tools


def evaluate_resolution(order_id: str, slots: dict[str, Any], db: dict[str, Any]) -> tuple[bool, str]:
    """业务评测：Goal 满足不等于政策正确。"""
    order = db["orders"][order_id]
    if order["shipped"] and slots.get("refunded") is True:
        return False, "已发货订单被退款，违反售后政策"
    if not slots.get("case_resolved"):
        return False, "工单未结案"
    if order["shipped"] and slots.get("resolution") != "notify":
        return False, "已发货订单应通知运单，而不是其他动作"
    if (not order["shipped"]) and order["days_waiting"] >= 3 and slots.get("resolution") != "refund":
        return False, "长时缺货未发货应按政策退款"
    return True, "政策校验通过"
