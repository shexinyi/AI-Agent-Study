"""
第 5 章示例 2：Anthropic Messages API 与 OpenAI 的字段对照。

无 ANTHROPIC_API_KEY：打印原生请求，并演示如何把同一轮对话映射成两种协议。
有 Key：用 httpx 直接打官方 HTTP，方便你看清 header，而不是只看见 SDK。

    python 02_anthropic_shape.py
"""

from __future__ import annotations

import json
import os
import sys

import httpx
from dotenv import load_dotenv

from billing import explain

SAMPLE_ANTHROPIC = {
    "id": "msg_teaching",
    "type": "message",
    "role": "assistant",
    "model": "claude-sonnet-4-5",
    "content": [{"type": "text", "text": "结论：Anthropic 把 system 放在顶层，messages 里通常只有 user/assistant。"}],
    "stop_reason": "end_turn",
    "usage": {"input_tokens": 36, "output_tokens": 24},
}


def same_conversation() -> dict:
    return {
        "system": "你是技术导师。用中文，先结论后理由，三句以内。",
        "user": "为什么 Anthropic 不把 system 放进 messages 数组？",
    }


def as_openai(conv: dict, model: str) -> dict:
    return {
        "model": model,
        "messages": [
            {"role": "system", "content": conv["system"]},
            {"role": "user", "content": conv["user"]},
        ],
    }


def as_anthropic(conv: dict, model: str) -> dict:
    return {
        "model": model,
        "max_tokens": 256,
        "system": conv["system"],
        "messages": [{"role": "user", "content": conv["user"]}],
    }


def parse_anthropic(raw: dict) -> None:
    texts = [block["text"] for block in raw.get("content", []) if block.get("type") == "text"]
    usage = raw.get("usage") or {}
    print(f"模型回复: {''.join(texts)}")
    print(f"stop_reason: {raw.get('stop_reason')}")
    print(explain(raw.get("model", "claude-sonnet-4-5"), int(usage.get("input_tokens") or 0), int(usage.get("output_tokens") or 0)))


def live_call(body: dict) -> dict:
    headers = {
        "x-api-key": os.environ["ANTHROPIC_API_KEY"],
        "anthropic-version": os.getenv("ANTHROPIC_VERSION", "2023-06-01"),
        "content-type": "application/json",
    }
    with httpx.Client(timeout=30.0) as client:
        response = client.post("https://api.anthropic.com/v1/messages", headers=headers, json=body)
        if response.status_code >= 400:
            raise RuntimeError(f"HTTP {response.status_code}: {response.text}")
        return response.json()


if __name__ == "__main__":
    load_dotenv()
    conv = same_conversation()
    openai_body = as_openai(conv, os.getenv("OPENAI_MODEL", "gpt-4o-mini"))
    anthropic_body = as_anthropic(conv, os.getenv("ANTHROPIC_MODEL", "claude-sonnet-4-5"))

    print("=== 同一轮对话：OpenAI 形状 ===")
    print(json.dumps(openai_body, ensure_ascii=False, indent=2))
    print("\n=== 同一轮对话：Anthropic 形状 ===")
    print(json.dumps(anthropic_body, ensure_ascii=False, indent=2))
    print("\n差异要点：system 在 Anthropic 是顶层字段；messages 一般不放 system。")
    print("鉴权头是 x-api-key + anthropic-version，不是 Authorization: Bearer。")

    if not os.getenv("ANTHROPIC_API_KEY"):
        print("\n未检测到 ANTHROPIC_API_KEY，解析教学响应。")
        parse_anthropic(SAMPLE_ANTHROPIC)
        sys.exit(0)

    print("\n检测到 Key，发起真实 Messages 请求...\n")
    parse_anthropic(live_call(anthropic_body))
