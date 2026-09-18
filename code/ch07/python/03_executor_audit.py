"""
第 7 章示例 3：超时、幂等、权限、审计。

    python 03_executor_audit.py
"""

from __future__ import annotations

from catalog import REFUNDS, good_specs
from executor import ProductionExecutor
from toolkit import ToolCall


def show(title: str, result, executor: ProductionExecutor) -> None:
    print(f"\n### {title}")
    print(f"ok={result.ok} replayed={result.replayed} data={result.data} error={result.error}")
    ev = executor.audit[-1]
    print(f"审计: tool={ev.tool} risk={ev.risk} {ev.latency_ms}ms replayed={ev.replayed} err={ev.error}")


if __name__ == "__main__":
    specs = good_specs()
    executor = ProductionExecutor(specs, granted_write={"refund_order"})

    read = executor.execute(ToolCall("c1", "get_order", {"order_id": "ORDER-88"}))
    show("只读查单", read, executor)

    first = executor.execute(
        ToolCall("c2", "refund_order", {"order_id": "ORDER-88"}, idempotency_key="refund-88-user")
    )
    show("第一次退款（真的写台账）", first, executor)

    second = executor.execute(
        ToolCall("c3", "refund_order", {"order_id": "ORDER-88"}, idempotency_key="refund-88-user")
    )
    show("同一幂等键再退一次（应 replay，台账不加倍）", second, executor)

    denied = ProductionExecutor(specs, granted_write=set()).execute(
        ToolCall("c4", "refund_order", {"order_id": "ORDER-88"}, idempotency_key="other")
    )
    print(f"\n### 未授权退款\n{denied.error}")

    timed = executor.execute(ToolCall("c5", "slow_lookup", {"order_id": "ORDER-88"}))
    show("超时（handler sleep 0.35s，限额 0.1s）", timed, executor)

    print(f"\n台账 REFUNDS = {REFUNDS}  （长度应为 1，证明幂等挡住了第二次）")
    print("本执行器审计:")
    for ev in executor.audit:
        print(f" - {ev.tool:14} ok={ev.ok} replay={ev.replayed} {ev.latency_ms}ms {ev.error or ''}")
