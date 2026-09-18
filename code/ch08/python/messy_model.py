"""教学模型：按「模式」返回散文、残缺 JSON 或合格 JSON。"""

from __future__ import annotations

from typing import Literal

Mode = Literal["prose", "fenced_bad", "missing_field", "valid", "tool_args"]


def complete(mode: Mode, utterance: str = "") -> str:
    if mode == "prose":
        return "好的呀，林秋大概下周一到周三请三天假去上海，我帮你记下了～"
    if mode == "fenced_bad":
        return (
            "抽取结果如下：\n"
            "```json\n"
            '{"ok": true, "slots": {"employee": "林秋", "start_date": "2026-09-14",'
            ' "end_date": "2026-09-16", "days": 5, "leave_type": "事假"}}\n'
            "```\n"
            "另外我建议她带把伞。"
        )
    if mode == "missing_field":
        return '{"ok": true, "slots": {"employee": "林", "start_date": "2026-09-14"}}'
    if mode == "valid":
        return (
            '{"ok": true, "slots": {"employee": "林秋", "start_date": "2026-09-14",'
            ' "end_date": "2026-09-16", "days": 3, "leave_type": "事假"}}'
        )
    if mode == "tool_args":
        return (
            '{"name": "submit_leave_extract", "arguments": {"ok": true, "slots": {'
            '"employee": "林秋", "start_date": "2026-09-14", "end_date": "2026-09-16",'
            ' "days": 3, "leave_type": "事假"}}}'
        )
    raise ValueError(mode)


class RepairingModel:
    """第一次返回日期和天数对不上的 JSON，收到校验错误后再返回合格 JSON。"""

    def __init__(self) -> None:
        self.calls = 0

    def complete(self, messages: list[dict[str, str]]) -> str:
        self.calls += 1
        if self.calls == 1:
            return complete("fenced_bad")
        last = messages[-1]["content"]
        if "校验失败" in last:
            return complete("valid")
        return complete("prose")
