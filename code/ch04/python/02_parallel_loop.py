"""
第 4 章示例 2：一轮并行点两道菜，再根据观察给出最终答复。

MockLLM 返回的 JSON 仍是 OpenAI 形状。无需 API Key。

    python 02_parallel_loop.py
"""

from __future__ import annotations

import json
from typing import Any

from catalog import travel_specs
from protocol import ToolExecutor, extract_tool_calls


class TravelMockLLM:
    """规则模型：第一次并行调用天气和机票，第二次汇总。"""

    def complete(self, messages: list[dict[str, Any]]) -> dict[str, Any]:
        has_tool_result = any(item.get("role") == "tool" for item in messages)
        if not has_tool_result:
            return {
                "role": "assistant",
                "content": None,
                "tool_calls": [
                    {
                        "id": "call_weather",
                        "type": "function",
                        "function": {"name": "get_weather", "arguments": '{"city":"上海"}'},
                    },
                    {
                        "id": "call_price",
                        "type": "function",
                        "function": {
                            "name": "get_flight_price",
                            "arguments": '{"date":"2026-09-14","from_city":"北京","to_city":"上海"}',
                        },
                    },
                ],
            }
        facts = [json.loads(item["content"]) for item in messages if item.get("role") == "tool"]
        weather = next(item["data"] for item in facts if "weather" in item["data"])
        price = next(item["data"] for item in facts if "lowest_price" in item["data"])
        text = (
            f"上海今天{weather['weather']}，{weather['temp_c']}°C；"
            f"北京到上海最低票价约 {price['lowest_price']} 元。"
        )
        return {"role": "assistant", "content": text, "tool_calls": None}


def run_loop(user_text: str, max_rounds: int = 4) -> str:
    specs = travel_specs()
    executor = ToolExecutor(specs)
    llm = TravelMockLLM()
    messages: list[dict[str, Any]] = [
        {"role": "system", "content": "需要事实时并行调用工具，不要编造。"},
        {"role": "user", "content": user_text},
    ]
    for round_id in range(1, max_rounds + 1):
        assistant = llm.complete(messages)
        messages.append(assistant)
        calls = extract_tool_calls(assistant)
        if not calls:
            return assistant.get("content") or ""
        print(f"第 {round_id} 轮：并行 {len(calls)} 个工具")
        for call in calls:
            result = executor.execute(call)
            print(f"  {call.name}{call.arguments} -> {result.data or result.error}")
            messages.append(result.as_openai_message())
    return "达到最大轮次"


if __name__ == "__main__":
    answer = run_loop("明天北京去上海出差，上海天气和最低机票各是多少？")
    print(f"\n最终答复: {answer}")
