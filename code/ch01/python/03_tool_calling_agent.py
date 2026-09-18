"""
第 1 章示例 3：带工具调用的最小 Agent。

模型负责「想」和「选工具」，Python 负责真正执行工具，再把观察结果喂回模型。
这就是 Function Calling / Tool Use 的最小闭环。

环境变量与 02_llm_hello.py 相同。

运行：
    pip install -r requirements.txt
    python 03_tool_calling_agent.py
"""

from __future__ import annotations

import json
import os
import sys
from typing import Any

from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()


TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "查询某城市当前天气，城市用中文名，例如 上海、北京。",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市中文名"},
                },
                "required": ["city"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_flight_price",
            "description": "查询某日从出发地到目的地的最低机票价格（人民币）。",
            "parameters": {
                "type": "object",
                "properties": {
                    "date": {"type": "string", "description": "出发日期，格式 YYYY-MM-DD"},
                    "from_city": {"type": "string"},
                    "to_city": {"type": "string"},
                },
                "required": ["date", "from_city", "to_city"],
            },
        },
    },
]


def get_weather(city: str) -> dict[str, Any]:
    catalog = {
        "上海": {"weather": "小雨", "temp_c": 22, "wind": "东风3级"},
        "北京": {"weather": "晴", "temp_c": 26, "wind": "北风2级"},
    }
    return catalog.get(city, {"weather": "未知", "temp_c": None, "note": "演示数据未覆盖该城市"})


def get_flight_price(date: str, from_city: str, to_city: str) -> dict[str, Any]:
    if from_city == "北京" and to_city == "上海":
        return {"date": date, "from_city": from_city, "to_city": to_city, "lowest_price": 680}
    return {"date": date, "from_city": from_city, "to_city": to_city, "lowest_price": 1099}


DISPATCH = {
    "get_weather": lambda args: get_weather(city=args["city"]),
    "get_flight_price": lambda args: get_flight_price(
        date=args["date"], from_city=args["from_city"], to_city=args["to_city"]
    ),
}


def build_client() -> OpenAI:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("缺少 OPENAI_API_KEY。")
        sys.exit(1)
    return OpenAI(
        api_key=api_key,
        base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
    )


def run_tool_agent(user_goal: str, max_steps: int = 6) -> str:
    client = build_client()
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    messages: list[dict[str, Any]] = [
        {
            "role": "system",
            "content": (
                "你是出行助手。需要事实信息时必须调用工具，不要编造天气和价格。"
                "拿到足够信息后，用中文给出简洁结论。"
            ),
        },
        {"role": "user", "content": user_goal},
    ]

    for step in range(1, max_steps + 1):
        response = client.chat.completions.create(
            model=model,
            temperature=0,
            messages=messages,
            tools=TOOLS,
            tool_choice="auto",
        )
        message = response.choices[0].message
        messages.append(message.model_dump(exclude_unset=True))

        if not message.tool_calls:
            return message.content or ""

        print(f"第 {step} 步：模型决定调用 {len(message.tool_calls)} 个工具")
        for call in message.tool_calls:
            args = json.loads(call.function.arguments or "{}")
            print(f"  - {call.function.name}{args}")
            result = DISPATCH[call.function.name](args)
            messages.append(
                {
                    "role": "tool",
                    "tool_call_id": call.id,
                    "content": json.dumps(result, ensure_ascii=False),
                }
            )

    return "达到最大步数，提前结束，避免无限调用。"


if __name__ == "__main__":
    goal = "我明天要从北京去上海出差，帮我看一眼上海天气，以及最低机票大概多少钱。"
    print(f"目标: {goal}\n")
    print("最终答复:")
    print(run_tool_agent(goal))
