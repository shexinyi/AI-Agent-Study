"""
第 4 章示例 3：权限挡资金工具；瞬时失败在执行器内重试。

- refund_order 风险=money，默认不授权 → PERMISSION_DENIED，回传给模型改主意
- get_inventory 第一次抛 TransientError，执行器重试后成功，模型看不见第一次超时
- notify_user 风险=write，本例显式授权，允许发通知

    python 03_permission_retry.py
"""

from __future__ import annotations

import json
from typing import Any

from catalog import reset_flaky, support_specs
from protocol import ToolExecutor, extract_tool_calls


class SupportMockLLM:
    def complete(self, messages: list[dict[str, Any]]) -> dict[str, Any]:
        tool_payloads = [json.loads(item["content"]) for item in messages if item.get("role") == "tool"]
        denied = any(
            (item.get("error") or "").startswith("PERMISSION_DENIED") for item in tool_payloads
        )
        has_order = any("shipped" in item.get("data", {}) for item in tool_payloads)
        has_stock = any("stock" in item.get("data", {}) for item in tool_payloads)

        if not has_order:
            return _call("call_order", "get_order", {"order_id": "ORDER-88"})
        if has_order and not has_stock and not denied:
            return _call("call_stock", "get_inventory", {"sku": "SKU-9"})
        if not denied:
            return _call("call_refund", "refund_order", {"order_id": "ORDER-88"})
        return _call(
            "call_notify",
            "notify_user",
            {"order_id": "ORDER-88", "message": "退款未授权，已升级人工；库存为 0，请等待主管审批。"},
        )


def _call(call_id: str, name: str, args: dict[str, Any]) -> dict[str, Any]:
    return {
        "role": "assistant",
        "content": None,
        "tool_calls": [
            {
                "id": call_id,
                "type": "function",
                "function": {"name": name, "arguments": json.dumps(args, ensure_ascii=False)},
            }
        ],
    }


def run(grant_notify: bool = True) -> None:
    reset_flaky(1)
    specs = support_specs()
    granted = {"notify_user"} if grant_notify else set()
    executor = ToolExecutor(specs, granted_write=granted, max_retries=2)
    llm = SupportMockLLM()
    messages: list[dict[str, Any]] = [
        {"role": "system", "content": "按政策处理 ORDER-88。资金工具未授权时不要编造成功。"},
        {"role": "user", "content": "ORDER-88 一直没发货，给我退款。"},
    ]
    print(f"写授权名单: {granted or '空（资金/写入全部拒绝）'}\n")

    for round_id in range(1, 8):
        assistant = llm.complete(messages)
        messages.append(assistant)
        calls = extract_tool_calls(assistant)
        if not calls:
            print(f"最终: {assistant.get('content')}")
            return
        print(f"第 {round_id} 轮模型点菜: {[c.name for c in calls]}")
        for call in calls:
            result = executor.execute(call)
            flag = "OK" if result.ok else result.error
            print(f"  执行 {call.name}{call.arguments} -> {flag} {result.data}")
            messages.append(result.as_openai_message())
            if result.ok and call.name == "notify_user":
                print("\n结案：通知已发出，退款未发生。")
                return
    print("达到最大轮次")


if __name__ == "__main__":
    run()
