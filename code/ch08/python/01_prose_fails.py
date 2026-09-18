"""
第 8 章示例 1：散文进不了下游。

    python -m pip install -r requirements.txt
    python 01_prose_fails.py
"""

from __future__ import annotations

from messy_model import complete
from parseutil import try_load
from schemas import LeaveExtract


def try_parse(title: str, raw: str) -> None:
    print(f"\n### {title}")
    print("模型原文:")
    print(raw)
    try:
        data = try_load(raw)
        print("json.loads 成功:", data)
    except Exception as exc:
        print(f"json.loads 失败: {exc}")
        return
    try:
        model = LeaveExtract.model_validate(data)
        print("Pydantic 通过:", model.model_dump())
    except Exception as exc:
        print(f"Pydantic 拒绝: {exc}")


if __name__ == "__main__":
    try_parse("A 纯散文", complete("prose"))
    try_parse("B 围栏 JSON，但 days=5 与日期矛盾", complete("fenced_bad"))
    try_parse("C 字段残缺", complete("missing_field"))
    try_parse("D 合格 JSON", complete("valid"))
    print("\n读法：能 json.loads 不等于能进 HR 系统。B 会被天数校验拦住。")
