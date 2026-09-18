"""第 9 章闸门的本章副本：本章可独立运行。"""

from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass
class Fact:
    key: str
    value: str
    source: str
    confidence: float


def should_remember(text: str) -> tuple[bool, str, Fact | None]:
    if re.search(r"密码|口令|验证码|银行卡", text):
        return False, "敏感信息，禁止写入向量库", None
    if re.search(r"哈哈|嗯嗯|好的|在吗", text) and len(text) < 8:
        return False, "寒暄，不值得长期占用", None
    if "喜欢" in text or "偏好" in text:
        return True, "稳定偏好", Fact("preference", text, "user", 0.8)
    name = re.search(r"我是([\u4e00-\u9fff]{2,4})", text)
    if name:
        correction = "其实" in text
        return True, "身份更正" if correction else "身份槽位", Fact(
            "employee",
            name.group(1),
            "user_correction" if correction else "user",
            0.95 if correction else 0.9,
        )
    return False, "一次性任务细节，不要向量化", None
