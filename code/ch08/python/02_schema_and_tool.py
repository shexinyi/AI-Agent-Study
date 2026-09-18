"""
第 8 章示例 2：同一份 Pydantic 契约，接住「说话」和「点菜」。

    python 02_schema_and_tool.py
"""

from __future__ import annotations

import json

from messy_model import complete
from parseutil import extract_json_text
from schemas import LeaveExtract


def from_chat_content(raw: str) -> LeaveExtract:
    return LeaveExtract.model_validate_json(extract_json_text(raw))


def from_tool_call(raw: str) -> LeaveExtract:
    payload = json.loads(raw)
    return LeaveExtract.model_validate(payload["arguments"])


if __name__ == "__main__":
    schema = LeaveExtract.model_json_schema()
    print("=== 发给模型或 OpenAI structured output 的 JSON Schema（节选）===")
    print(json.dumps(schema, ensure_ascii=False, indent=2)[:700], "...\n")

    chat = from_chat_content(complete("valid"))
    tool = from_tool_call(complete("tool_args"))
    print("聊天内容解析:", chat.model_dump())
    print("工具参数解析:", tool.model_dump())
    print("两份对象相等:", chat == tool)
    print("\n差异不在 Schema，在意图：聊天结构是「答案形状」；工具参数后面往往还要执行副作用。")
