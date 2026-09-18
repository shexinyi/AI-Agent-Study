"""
第 5 章示例 3：超时、有限重试、流式输出、把 usage 折成钱。

无 Key：回放一段 SSE 教学流，并演示哪些错误可重试。
有 Key：对 OpenAI 兼容接口做流式调用。

    python 03_stream_retry_cost.py
"""

from __future__ import annotations

import os
import sys
import time
from collections.abc import Iterator

from dotenv import load_dotenv
from openai import APIStatusError, OpenAI

from billing import explain, estimate_tokens

TEACHING_SSE = [
    'data: {"choices":[{"delta":{"content":"先"}}]}',
    'data: {"choices":[{"delta":{"content":"出字"}}]}',
    'data: {"choices":[{"delta":{"content":"，再"}}]}',
    'data: {"choices":[{"delta":{"content":"算账。"}}]}',
    "data: [DONE]",
]


def retryable(status_code: int | None) -> bool:
    return status_code in {408, 409, 429, 500, 502, 503, 504}


def play_sse(lines: list[str]) -> str:
    print("=== 教学 SSE 回放 ===")
    chunks: list[str] = []
    for line in lines:
        if not line.startswith("data:"):
            continue
        data = line[5:].strip()
        if data == "[DONE]":
            print("\n[DONE]")
            break
        payload = __import__("json").loads(data)
        piece = payload["choices"][0]["delta"].get("content") or ""
        chunks.append(piece)
        print(piece, end="", flush=True)
    return "".join(chunks)


def stream_live(question: str, attempts: int = 3) -> str:
    client = OpenAI(
        api_key=os.environ["OPENAI_API_KEY"],
        base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        timeout=30.0,
    )
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    last_error: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            print(f"=== 第 {attempt} 次流式请求 ===")
            kwargs = {
                "model": model,
                "temperature": 0.2,
                "stream": True,
                "messages": [
                    {"role": "system", "content": "用中文，两句话回答。"},
                    {"role": "user", "content": question},
                ],
            }
            # include_usage 是 OpenAI 扩展，国产兼容网关可能 400，失败就降级。
            try:
                stream = client.chat.completions.create(**kwargs, stream_options={"include_usage": True})
            except APIStatusError as exc:
                if exc.status_code == 400:
                    stream = client.chat.completions.create(**kwargs)
                else:
                    raise
            parts: list[str] = []
            usage = None
            for event in stream:
                if getattr(event, "usage", None):
                    usage = event.usage
                if not event.choices:
                    continue
                piece = event.choices[0].delta.content or ""
                if piece:
                    parts.append(piece)
                    print(piece, end="", flush=True)
            print()
            text = "".join(parts)
            if usage:
                print(explain(model, usage.prompt_tokens or 0, usage.completion_tokens or 0))
            else:
                print("供应商未回传 usage，改用粗估：")
                print(explain(model, estimate_tokens(question), estimate_tokens(text)))
            return text
        except APIStatusError as exc:
            last_error = exc
            if not retryable(exc.status_code) or attempt == attempts:
                raise
            sleep_for = 0.4 * attempt
            print(f"\nHTTP {exc.status_code}，{sleep_for:.1f}s 后重试（只对超时/限流/5xx）")
            time.sleep(sleep_for)
        except (TimeoutError, OSError) as exc:
            last_error = exc
            if attempt == attempts:
                raise
            time.sleep(0.4 * attempt)
    raise RuntimeError(f"重试耗尽: {last_error}")


if __name__ == "__main__":
    load_dotenv()
    print("可重试状态码:", sorted({408, 409, 429, 500, 502, 503, 504}))
    print("不可重试: 400 参数错误、401 鉴权失败、403 权限不足。重试它们只会重复交学费。\n")

    if not os.getenv("OPENAI_API_KEY"):
        text = play_sse(TEACHING_SSE)
        print(explain("gpt-4o-mini", estimate_tokens("流式输出有什么好处"), estimate_tokens(text)))
        sys.exit(0)

    stream_live("流式输出对客服产品的用户体验有什么好处？")
