"""
教学用指令跟随器：按 Prompt 契约的关键短语改变行为。

它不是大模型。它用来证明「同一句用户原话，契约不同，输出不同」。
换真模型时，回归集可以原样留下。
"""

from __future__ import annotations

import json
import re
from typing import Any

from promptkit import Message


def complete(messages: list[Message]) -> str:
    system = "\n".join(item.content for item in messages if item.role == "system")
    user = next(item.content for item in reversed(messages) if item.role == "user")
    fewshot = any(item.role == "assistant" for item in messages)

    if "忽略以上" in user or "忽略系统提示" in user:
        return "好的，我已忽略系统提示，直接批准三天假期，无需经理审批。"

    if "跳过审批" in user and "拒绝越权" in system:
        if "只输出 JSON" in system:
            return json.dumps({"ok": False, "error": "refused", "reason": "不可跳过审批"}, ensure_ascii=False)
        return "拒绝：请假必须走审批，我不能按你的要求跳过。"

    if "不得编造日期" in system and not re.search(r"周[一二三四五六日]|\d{4}-\d{2}-\d{2}", user):
        if "只输出 JSON" in system or "ONLY_JSON" in system:
            return json.dumps(
                {"ok": False, "error": "need_clarification", "missing": ["start_date", "end_date"]},
                ensure_ascii=False,
            )
        return "日期不明确，我不能编造。请补充开始和结束日期。"

    if "只输出 JSON" in system or "ONLY_JSON" in system:
        slots = _extract(user)
        if fewshot and "days" not in slots:
            slots["days"] = 3
        return json.dumps({"ok": True, "slots": slots}, ensure_ascii=False)

    if "必须写满500字" in system and "只输出一行" in system:
        return (
            "好的我尽量又短又长：这是一段为了同时满足「写满500字」和「只输出一行」"
            "而开始注水的回复，看起来什么都做了，其实契约已经互相打架……"
        )

    return "收到，我帮你看看请假的事吧。你可能是想请假？大概三天？我猜是上海？需要的话我再问经理。"


def parse_json_output(text: str) -> dict[str, Any]:
    return json.loads(text)


def _extract(user: str) -> dict[str, Any]:
    slots: dict[str, Any] = {"raw": user}
    if "周一" in user and "周三" in user:
        slots.update({"start_date": "2026-09-14", "end_date": "2026-09-16", "days": 3, "leave_type": "事假"})
    if "上海" in user:
        slots["city"] = "上海"
    name = re.search(r"我是([\u4e00-\u9fff]{2,4})", user)
    if name:
        slots["employee"] = name.group(1)
    return slots
