"""最小记忆接口：工作记忆 + 情节事件 + 语义事实。不依赖向量库。"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Literal


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def tokenish(text: str) -> int:
    return max(1, len(text))


@dataclass
class Turn:
    role: str
    content: str


@dataclass
class Episode:
    at: str
    kind: str
    text: str


@dataclass
class Fact:
    key: str
    value: str
    source: str
    confidence: float
    updated_at: str


class WorkingMemory:
    """短期：最近窗口 + 滚动摘要 + 槽位。"""

    def __init__(self, max_turns: int = 4) -> None:
        self.max_turns = max_turns
        self.turns: list[Turn] = []
        self.summary = ""
        self.slots: dict[str, Any] = {}

    def add(self, role: str, content: str) -> None:
        self.turns.append(Turn(role, content))
        self._extract_slots(content)
        while len(self.turns) > self.max_turns:
            evicted = self.turns.pop(0)
            self.summary = summarize(self.summary, evicted)

    def pack_for_prompt(self) -> str:
        parts = []
        if self.summary:
            parts.append(f"[摘要] {self.summary}")
        if self.slots:
            parts.append("[槽位] " + ", ".join(f"{k}={v}" for k, v in self.slots.items()))
        for turn in self.turns:
            parts.append(f"[{turn.role}] {turn.content}")
        return "\n".join(parts)

    def size(self) -> int:
        return tokenish(self.pack_for_prompt())

    def _extract_slots(self, content: str) -> None:
        if "周一" in content and "周三" in content:
            self.slots["start_date"] = "2026-09-14"
            self.slots["end_date"] = "2026-09-16"
            self.slots["days"] = 3
        name = re.search(r"我是([\u4e00-\u9fff]{2,4})", content)
        if name:
            self.slots["employee"] = name.group(1)
        if "上海" in content:
            self.slots["city"] = "上海"


def summarize(old: str, evicted: Turn) -> str:
    piece = f"{evicted.role}:{evicted.content[:20]}"
    if not old:
        return f"已折叠 {piece}"
    return f"{old}；+{piece}"


class EpisodicMemory:
    def __init__(self) -> None:
        self.events: list[Episode] = []

    def record(self, kind: str, text: str) -> None:
        self.events.append(Episode(now_iso(), kind, text))

    def recent(self, limit: int = 5) -> list[Episode]:
        return self.events[-limit:]


ConflictPolicy = Literal["newer_wins", "higher_confidence", "keep_old"]


class SemanticMemory:
    def __init__(self) -> None:
        self.facts: dict[str, Fact] = {}

    def upsert(self, fact: Fact, policy: ConflictPolicy = "newer_wins") -> str:
        old = self.facts.get(fact.key)
        if old is None:
            self.facts[fact.key] = fact
            return "inserted"
        if old.value == fact.value:
            self.facts[fact.key] = fact
            return "refreshed"
        if policy == "keep_old":
            return "conflict_kept_old"
        if policy == "higher_confidence" and fact.confidence < old.confidence:
            return "conflict_kept_old"
        self.facts[fact.key] = fact
        return "conflict_replaced"

    def get(self, key: str) -> Fact | None:
        return self.facts.get(key)

    def search(self, query: str) -> list[Fact]:
        return [fact for fact in self.facts.values() if query in fact.key or query in fact.value]

    def forget(self, key: str) -> bool:
        return self.facts.pop(key, None) is not None


def should_remember(text: str) -> tuple[bool, str, Fact | None]:
    """该不该写入长期记忆。返回 (是否写, 理由, 事实或空)。"""
    if re.search(r"密码|口令|验证码|银行卡", text):
        return False, "敏感信息，禁止写入长期记忆", None
    if re.search(r"哈哈|嗯嗯|好的|在吗", text) and len(text) < 8:
        return False, "寒暄，不值得长期占用", None
    if "喜欢" in text or "偏好" in text:
        return True, "稳定偏好", Fact("preference", text, "user", 0.8, now_iso())
    name = re.search(r"我是([\u4e00-\u9fff]{2,4})", text)
    if name:
        source = "user_correction" if "其实" in text else "user"
        confidence = 0.95 if "其实" in text else 0.9
        return True, "身份槽位" if source == "user" else "身份更正", Fact(
            "employee", name.group(1), source, confidence, now_iso()
        )
    return False, "一次性任务细节，留给工作记忆即可", None
