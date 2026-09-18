"""售后 / 出行共用的工具目录。handler 是真函数，不是模型幻觉。"""

from __future__ import annotations

from protocol import TransientError, ToolSpec

INVENTORY_HITS = {"SKU-9": 0}
_FLAKY_LEFT = {"n": 1}


def reset_flaky(fail_times: int = 1) -> None:
    _FLAKY_LEFT["n"] = fail_times


def get_weather(city: str) -> dict:
    catalog = {"上海": {"weather": "小雨", "temp_c": 22}, "北京": {"weather": "晴", "temp_c": 26}}
    return catalog.get(city, {"weather": "未知", "temp_c": None, "city": city})


def get_flight_price(date: str, from_city: str, to_city: str) -> dict:
    price = 680 if from_city == "北京" and to_city == "上海" else 1099
    return {"date": date, "from_city": from_city, "to_city": to_city, "lowest_price": price}


def get_order(order_id: str) -> dict:
    if order_id == "ORDER-99":
        return {"order_id": order_id, "shipped": True, "tracking": "TZ123"}
    if order_id == "ORDER-88":
        return {"order_id": order_id, "shipped": False, "sku": "SKU-9", "days_waiting": 4}
    raise ValueError(f"订单不存在: {order_id}")


def get_inventory(sku: str) -> dict:
    if _FLAKY_LEFT["n"] > 0:
        _FLAKY_LEFT["n"] -= 1
        raise TransientError("库存服务超时")
    return {"sku": sku, "stock": INVENTORY_HITS.get(sku, 3)}


def refund_order(order_id: str) -> dict:
    return {"order_id": order_id, "refunded": True, "amount": 199}


def notify_user(order_id: str, message: str) -> dict:
    return {"order_id": order_id, "notified": True, "message": message}


def travel_specs() -> dict[str, ToolSpec]:
    return {
        "get_weather": ToolSpec(
            "get_weather",
            "查询某城市当前天气，城市用中文名。",
            {
                "type": "object",
                "properties": {"city": {"type": "string", "description": "城市中文名"}},
                "required": ["city"],
            },
            "read",
            get_weather,
        ),
        "get_flight_price": ToolSpec(
            "get_flight_price",
            "查询某日最低机票价格（人民币）。",
            {
                "type": "object",
                "properties": {
                    "date": {"type": "string"},
                    "from_city": {"type": "string"},
                    "to_city": {"type": "string"},
                },
                "required": ["date", "from_city", "to_city"],
            },
            "read",
            get_flight_price,
        ),
    }


def support_specs() -> dict[str, ToolSpec]:
    return {
        "get_order": ToolSpec(
            "get_order",
            "查询订单支付与发货状态。",
            {
                "type": "object",
                "properties": {"order_id": {"type": "string"}},
                "required": ["order_id"],
            },
            "read",
            get_order,
        ),
        "get_inventory": ToolSpec(
            "get_inventory",
            "查询 SKU 库存。下游可能超时，执行器应重试。",
            {
                "type": "object",
                "properties": {"sku": {"type": "string"}},
                "required": ["sku"],
            },
            "read",
            get_inventory,
        ),
        "refund_order": ToolSpec(
            "refund_order",
            "对订单执行退款。涉及资金，必须显式授权。",
            {
                "type": "object",
                "properties": {"order_id": {"type": "string"}},
                "required": ["order_id"],
            },
            "money",
            refund_order,
        ),
        "notify_user": ToolSpec(
            "notify_user",
            "向用户发送一条售后通知，不涉及资金。",
            {
                "type": "object",
                "properties": {
                    "order_id": {"type": "string"},
                    "message": {"type": "string"},
                },
                "required": ["order_id", "message"],
            },
            "write",
            notify_user,
        ),
    }
