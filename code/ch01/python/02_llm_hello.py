"""
第 1 章示例 2：第一次调用大模型（OpenAI 兼容接口）。

环境变量：
    OPENAI_API_KEY     必填
    OPENAI_BASE_URL    可选，默认 https://api.openai.com/v1
    OPENAI_MODEL       可选，默认 gpt-4o-mini

国产兼容示例（任选其一，不要同时搞混 Key 和 Base URL）：
    DeepSeek:  OPENAI_BASE_URL=https://api.deepseek.com/v1
    月之暗面:  OPENAI_BASE_URL=https://api.moonshot.cn/v1

运行：
    pip install -r requirements.txt
    python 02_llm_hello.py
"""

from __future__ import annotations

import os
import sys

from dotenv import load_dotenv
from openai import OpenAI


def build_client() -> OpenAI:
    load_dotenv()
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("缺少 OPENAI_API_KEY。可先运行 01_rule_agent.py，再回来接真实模型。")
        sys.exit(1)
    return OpenAI(
        api_key=api_key,
        base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
    )


def ask(question: str) -> str:
    client = build_client()
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    response = client.chat.completions.create(
        model=model,
        temperature=0.2,
        messages=[
            {
                "role": "system",
                "content": "你是一名耐心的技术导师。用中文回答，先给结论，再给理由。",
            },
            {"role": "user", "content": question},
        ],
    )
    choice = response.choices[0]
    if not choice.message.content:
        raise RuntimeError("模型没有返回文本内容")
    return choice.message.content


if __name__ == "__main__":
    user_question = "用三句话解释：AI Agent 和 Chatbot 的核心区别是什么？"
    print(f"用户: {user_question}\n")
    print(f"模型: {ask(user_question)}")
