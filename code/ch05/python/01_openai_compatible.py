"""
第 5 章示例 1：OpenAI 兼容 Chat Completions。

无 Key：打印将要发出的 HTTP 请求，并解析一份教学用响应。
有 Key：真实调用。国产兼容只需改 OPENAI_BASE_URL / OPENAI_MODEL。

    pip install -r requirements.txt
    python 01_openai_compatible.py
"""

from __future__ import annotations

import json
import os
import sys

from dotenv import load_dotenv
from openai import OpenAI

from billing import explain, estimate_tokens

SAMPLE_RESPONSE = {
    "id": "chatcmpl-teaching",
    "model": "gpt-4o-mini",
    "choices": [
        {
            "index": 0,
            "finish_reason": "stop",
            "message": {
                "role": "assistant",
                "content": "结论：API 卖的是 Token 与延迟，不是一个永远在线的同事。",
            },
        }
    ],
    "usage": {"prompt_tokens": 42, "completion_tokens": 28, "total_tokens": 70},
}


def payload(question: str, model: str) -> dict:
    return {
        "model": model,
        "temperature": 0.2,
        "messages": [
            {"role": "system", "content": "你是技术导师。用中文，先结论后理由，三句以内。"},
            {"role": "user", "content": question},
        ],
    }


def print_request(base_url: str, model: str, question: str) -> None:
    body = payload(question, model)
    print("=== 将 POST 的地址 ===")
    print(f"{base_url.rstrip('/')}/chat/completions")
    print("=== 将发送的 JSON ===")
    print(json.dumps(body, ensure_ascii=False, indent=2))
    print("=== 本地粗估输入 Token ===")
    text = "".join(item["content"] for item in body["messages"])
    print(explain(model, estimate_tokens(text), 0))


def parse_usage(raw: dict, model: str) -> None:
    message = raw["choices"][0]["message"]["content"]
    usage = raw.get("usage") or {}
    prompt = int(usage.get("prompt_tokens") or usage.get("input_tokens") or 0)
    completion = int(usage.get("completion_tokens") or usage.get("output_tokens") or 0)
    print(f"\n模型回复: {message}")
    print(explain(model, prompt, completion))
    print(f"finish_reason: {raw['choices'][0].get('finish_reason')}")


def live_ask(question: str) -> None:
    client = OpenAI(
        api_key=os.environ["OPENAI_API_KEY"],
        base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        timeout=30.0,
    )
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    response = client.chat.completions.create(**payload(question, model))
    parse_usage(response.model_dump(), model)


if __name__ == "__main__":
    load_dotenv()
    question = "大模型 API 到底在卖什么？请用买菜来类比。"
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
    print_request(base_url, model, question)
    if not os.getenv("OPENAI_API_KEY"):
        print("\n未检测到 OPENAI_API_KEY，进入教学解析（不发网络请求）。")
        parse_usage(SAMPLE_RESPONSE, SAMPLE_RESPONSE["model"])
        sys.exit(0)
    print("\n检测到 Key，发起真实请求...\n")
    live_ask(question)
