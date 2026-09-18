"""
第 4 章示例 1：看清楚一轮 Function Calling 在电线上长什么样。

不调用真实模型。我们手工构造一条「模型已经点菜」的 assistant 消息，
走：校验参数 → 执行 → 拼 tool 回传消息。

    python 01_protocol_roundtrip.py
"""

from __future__ import annotations

import json

from catalog import travel_specs
from protocol import ToolCall, ToolExecutor, extract_tool_calls


def build_request(user_text: str) -> dict:
    specs = travel_specs()
    return {
        "model": "gpt-4o-mini",
        "tool_choice": "auto",
        "messages": [
            {"role": "system", "content": "需要事实时必须调用工具，不要编造天气和价格。"},
            {"role": "user", "content": user_text},
        ],
        "tools": [spec.as_openai_tool() for spec in specs.values()],
    }


def fake_model_response() -> dict:
    """形状与 Chat Completions 的 choices[0].message 一致。arguments 必须是 JSON 字符串。"""
    return {
        "role": "assistant",
        "content": None,
        "tool_calls": [
            {
                "id": "call_weather_1",
                "type": "function",
                "function": {
                    "name": "get_weather",
                    "arguments": '{"city":"上海"}',
                },
            }
        ],
    }


if __name__ == "__main__":
    request = build_request("上海今天天气怎样？")
    print("=== 发给模型的请求（节选 tools + messages）===")
    print(json.dumps({"messages": request["messages"], "tools": request["tools"]}, ensure_ascii=False, indent=2))

    assistant = fake_model_response()
    print("\n=== 模型返回的 assistant 消息 ===")
    print(json.dumps(assistant, ensure_ascii=False, indent=2))

    executor = ToolExecutor(travel_specs())
    calls = extract_tool_calls(assistant)
    print("\n=== 执行器观察 ===")
    tool_messages = []
    for call in calls:
        result = executor.execute(call)
        print(f"{call.name}{call.arguments} -> ok={result.ok} data={result.data} error={result.error}")
        tool_messages.append(result.as_openai_message())

    print("\n=== 追加回 messages 的 tool 消息 ===")
    print(json.dumps(tool_messages, ensure_ascii=False, indent=2))

    next_request_messages = request["messages"] + [assistant] + tool_messages
    print(f"\n下一轮 messages 条数: {len(next_request_messages)}（system/user/assistant/tool）")
