"""售后域的真业务函数 + 好/坏两套工具包装。"""

from __future__ import annotations

import time
from typing import Any

from toolkit import ToolSpec

ORDERS = {
    "ORDER-88": {"shipped": False, "sku": "SKU-9", "user": "林秋"},
    "ORDER-99": {"shipped": True, "sku": "SKU-9", "tracking": "TZ123", "user": "林秋"},
}
REFUNDS: list[str] = []


def get_order(order_id: str) -> dict[str, Any]:
    order = ORDERS.get(order_id)
    if not order:
        raise ValueError(f"订单不存在: {order_id}")
    return {"order_id": order_id, **order}


def search_policy(topic: str) -> dict[str, Any]:
    return {"topic": topic, "text": "已发货禁止直接退款；未发货缺货超 3 天可退。"}


def refund_order(order_id: str) -> dict[str, Any]:
    if order_id not in ORDERS:
        raise ValueError(f"订单不存在: {order_id}")
    REFUNDS.append(order_id)
    return {"order_id": order_id, "refunded": True, "amount": 199}


def slow_lookup(order_id: str) -> dict[str, Any]:
    time.sleep(0.35)
    return get_order(order_id)


def bad_specs() -> dict[str, ToolSpec]:
    return {
        "do_stuff": ToolSpec(
            name="do_stuff",
            description="处理用户的问题，必要时看看数据。",
            when_to_use="随便",
            when_not="不清楚",
            parameters={"type": "object", "properties": {"q": {"type": "string"}}, "required": ["q"]},
            risk="write",
            timeout_seconds=5,
            idempotent=False,
            handler=lambda q: {"echo": q},
            keywords=("处理", "看看"),
        )
    }


def good_specs() -> dict[str, ToolSpec]:
    order_id = {
        "type": "object",
        "properties": {"order_id": {"type": "string", "description": "订单号，例如 ORDER-88"}},
        "required": ["order_id"],
    }
    return {
        "get_order": ToolSpec(
            name="get_order",
            description="按订单号查询支付、发货、运单。只读，无副作用。",
            when_to_use="用户提到具体订单号，需要核对状态。",
            when_not="用户只是问政策，或还没有订单号。",
            parameters=order_id,
            risk="read",
            timeout_seconds=1.0,
            idempotent=True,
            handler=get_order,
            keywords=("订单", "查单", "发货了吗", "ORDER-"),
        ),
        "search_policy": ToolSpec(
            name="search_policy",
            description="检索售后政策条文，返回一段只读说明。",
            when_to_use="用户问能不能退、要不要承担运费、规则是什么。",
            when_not="用户已经要求执行退款或改地址。",
            parameters={
                "type": "object",
                "properties": {"topic": {"type": "string", "description": "政策主题，如 退款、运费"}},
                "required": ["topic"],
            },
            risk="read",
            timeout_seconds=1.0,
            idempotent=True,
            handler=search_policy,
            keywords=("政策", "能不能退", "可以退吗", "规定", "规则"),
        ),
        "refund_order": ToolSpec(
            name="refund_order",
            description="对指定订单执行退款。资金动作，必须已授权且已核对物流。",
            when_to_use="已确认未发货且政策允许，并且调用方带来幂等键。",
            when_not="只是咨询、已发货、缺少订单号、用户情绪激动但未确认。",
            parameters=order_id,
            risk="money",
            timeout_seconds=2.0,
            idempotent=True,
            handler=refund_order,
            keywords=("给我退", "退钱", "马上退", "执行退款"),
        ),
        "slow_lookup": ToolSpec(
            name="slow_lookup",
            description="演示超时：故意 sleep 的查单。",
            when_to_use="只在超时教学里使用。",
            when_not="任何生产路径。",
            parameters=order_id,
            risk="read",
            timeout_seconds=0.1,
            idempotent=True,
            handler=slow_lookup,
            keywords=("慢查询",),
        ),
    }
