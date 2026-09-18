"""
第 8 章示例 3：校验失败就把错误喂回模型，再要一次。

    python 03_repair_loop.py
"""

from __future__ import annotations

from pydantic import ValidationError

from messy_model import RepairingModel
from parseutil import extract_json_text
from schemas import LeaveExtract


def parse_or_raise(raw: str) -> LeaveExtract:
    return LeaveExtract.model_validate_json(extract_json_text(raw))


def run_repair(max_attempts: int = 3) -> LeaveExtract:
    model = RepairingModel()
    messages = [
        {
            "role": "system",
            "content": "只输出符合 LeaveExtract 的 JSON。ok=true 必须带 slots，天数必须等于日期区间。",
        },
        {"role": "user", "content": "我是林秋，下周一到周三请假去上海办事。"},
    ]
    last_error = ""
    for attempt in range(1, max_attempts + 1):
        raw = model.complete(messages)
        print(f"\n第 {attempt} 次模型输出:\n{raw[:180]}...")
        try:
            parsed = parse_or_raise(raw)
            print(f"校验通过，调用次数={model.calls}")
            return parsed
        except (ValidationError, ValueError) as exc:
            last_error = str(exc)
            print(f"校验失败: {exc.__class__.__name__}")
            messages.append({"role": "assistant", "content": raw})
            messages.append(
                {
                    "role": "user",
                    "content": f"校验失败，请只返回纠正后的 JSON，不要解释。错误：{last_error[:400]}",
                }
            )
    raise RuntimeError(f"修复循环耗尽: {last_error}")


if __name__ == "__main__":
    result = run_repair()
    print("\n最终对象:")
    print(result.model_dump())
    print("slots.days =", result.slots.days if result.slots else None)
