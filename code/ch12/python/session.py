"""
对话状态：Session / Turn / Trace + 带确认的槽位。
只从用户轮次抽槽，避免第 9 章「我是请假助手」污染。
"""

from __future__ import annotations

import copy
import json
import re
import uuid
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Literal

REQUIRED = ("employee", "start_date", "end_date", "days")
SlotStatus = Literal["empty", "filled", "proposed", "confirmed"]
Phase = Literal["collecting", "awaiting_confirm", "confirmed", "cancelled"]


def now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


@dataclass
class Slot:
    name: str
    value: Any = None
    status: SlotStatus = "empty"
    proposed: Any = None

    def show(self) -> str:
        if self.status == "proposed":
            return f"{self.name}={self.value}->{self.proposed}(待确认)"
        if self.status == "empty":
            return f"{self.name}=∅"
        return f"{self.name}={self.value}({self.status})"


@dataclass
class Turn:
    no: int
    role: str
    content: str
    at: str


@dataclass
class Event:
    at: str
    kind: str
    text: str


@dataclass
class Session:
    session_id: str
    user_id: str
    goal: str = "submit_leave"
    phase: Phase = "collecting"
    version: int = 1
    slots: dict[str, Slot] = field(default_factory=dict)
    turns: list[Turn] = field(default_factory=list)
    trace: list[Event] = field(default_factory=list)
    ttl_turns: int = 20

    def __post_init__(self) -> None:
        if not self.slots:
            for name in (*REQUIRED, "leave_type"):
                self.slots[name] = Slot(name)

    def emit(self, kind: str, text: str) -> None:
        self.trace.append(Event(now_iso(), kind, text))

    def missing(self) -> list[str]:
        return [n for n in REQUIRED if self.slots[n].status == "empty"]

    def pack_for_prompt(self) -> str:
        lines = [
            f"[session {self.session_id} user={self.user_id} phase={self.phase} v={self.version}]",
            "[槽位] " + ", ".join(s.show() for s in self.slots.values()),
        ]
        if self.missing():
            lines.append("[缺失] " + ", ".join(self.missing()))
        for turn in self.turns[-4:]:
            lines.append(f"[t{turn.no} {turn.role}] {turn.content}")
        return "\n".join(lines)


class SessionService:
    def __init__(self, store: "StateStore") -> None:
        self.store = store

    def start(self, user_id: str, session_id: str | None = None) -> Session:
        sid = session_id or ("sess-" + uuid.uuid4().hex[:8])
        sess = Session(sid, user_id)
        sess.emit("created", f"user={user_id}")
        self.store.save(sess, expected_version=0)
        return sess

    def handle(self, user_id: str, session_id: str, text: str) -> Session:
        sess = self.store.get(user_id, session_id)
        if sess is None:
            raise PermissionError("isolation_or_missing")
        if len(sess.turns) >= sess.ttl_turns:
            sess.phase = "cancelled"
            sess.emit("expired", "超过 ttl_turns")
            self.store.save(sess, sess.version)
            return sess
        expected = sess.version
        sess.turns.append(Turn(len(sess.turns) + 1, "user", text, now_iso()))
        sess.emit("turn", text)
        if sess.phase == "cancelled":
            return sess
        if _is_cancel(text):
            sess.phase = "cancelled"
            sess.emit("cancelled", text)
        elif _is_confirm(text) and sess.phase in ("awaiting_confirm", "collecting"):
            _apply_confirm(sess)
        elif _is_reject(text):
            _reject_proposed(sess)
        else:
            _fill_from_user(sess, text)
            if not sess.missing() and sess.phase == "collecting":
                sess.phase = "awaiting_confirm"
                sess.emit("ready", "必填已齐，等待确认")
        self.store.save(sess, expected)
        return sess


class VersionConflict(RuntimeError):
    pass


class StateStore:
    def get(self, user_id: str, session_id: str) -> Session | None:
        raise NotImplementedError

    def save(self, sess: Session, expected_version: int) -> None:
        raise NotImplementedError


class MemoryStore(StateStore):
    def __init__(self) -> None:
        self._data: dict[str, Session] = {}

    def get(self, user_id: str, session_id: str) -> Session | None:
        sess = self._data.get(session_id)
        if sess is None:
            return None
        if sess.user_id != user_id:
            return None
        return copy.deepcopy(sess)

    def save(self, sess: Session, expected_version: int) -> None:
        old = self._data.get(sess.session_id)
        current = old.version if old else 0
        if current != expected_version:
            raise VersionConflict(f"expected v={expected_version} actual v={current}")
        if old and old.user_id != sess.user_id:
            raise PermissionError("cannot overwrite another user")
        sess.version = expected_version + 1
        self._data[sess.session_id] = sess


class FileStore(MemoryStore):
    """教学替身：文件 = Redis / 业务库的「重启还在」。"""

    def __init__(self, path: Path) -> None:
        super().__init__()
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        if self.path.exists():
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            for sid, blob in raw.items():
                self._data[sid] = _session_from_dict(blob)

    def save(self, sess: Session, expected_version: int) -> None:
        super().save(sess, expected_version)
        blob = {sid: _session_to_dict(s) for sid, s in self._data.items()}
        self.path.write_text(json.dumps(blob, ensure_ascii=False, indent=2), encoding="utf-8")


def _is_confirm(text: str) -> bool:
    return bool(re.search(r"^(对|确认|没问题|就这样|好的，提交)", text.strip()))


def _is_reject(text: str) -> bool:
    return "还是原来" in text or text.strip() in {"不要改", "算了改"}


def _is_cancel(text: str) -> bool:
    return text.strip() in {"不用了", "取消"}


def _fill_from_user(sess: Session, text: str) -> None:
    updates: dict[str, Any] = {}
    name = re.search(r"我是([\u4e00-\u9fff]{2,4})", text)
    if name:
        updates["employee"] = name.group(1)
    if "周一" in text and "周三" in text:
        updates["start_date"] = "2026-09-14"
        updates["end_date"] = "2026-09-16"
        updates["days"] = 3
    if "周四" in text and "结束" in text:
        updates["end_date"] = "2026-09-17"
        start = sess.slots["start_date"].value or "2026-09-14"
        updates["start_date"] = start
        updates["days"] = (date.fromisoformat(updates["end_date"]) - date.fromisoformat(start)).days + 1
    if re.search(r"两天|2天", text):
        start = sess.slots["start_date"].value or updates.get("start_date") or "2026-09-14"
        updates["start_date"] = start
        updates["end_date"] = (date.fromisoformat(start) + timedelta(days=1)).isoformat()
        updates["days"] = 2
    if "事假" in text:
        updates["leave_type"] = "事假"
    if "年假" in text:
        updates["leave_type"] = "年假"
    for key, value in updates.items():
        _put_slot(sess, key, value)


def _put_slot(sess: Session, key: str, value: Any) -> None:
    slot = sess.slots[key]
    if slot.status == "confirmed" and slot.value != value:
        slot.proposed = value
        slot.status = "proposed"
        sess.phase = "awaiting_confirm"
        sess.emit("proposed", f"{key}: {slot.value} -> {value}")
        return
    if slot.value == value and slot.status != "empty":
        return
    slot.value = value
    slot.status = "filled"
    slot.proposed = None
    sess.emit("filled", f"{key}={value}")


def _apply_confirm(sess: Session) -> None:
    for slot in sess.slots.values():
        if slot.status == "proposed":
            slot.value = slot.proposed
            slot.proposed = None
        if slot.status in ("filled", "proposed"):
            slot.status = "confirmed"
    if sess.missing():
        sess.phase = "collecting"
        sess.emit("confirm_partial", "还有缺失，继续收集")
        return
    sess.phase = "confirmed"
    sess.emit("confirmed", "用户确认，可提交")


def _reject_proposed(sess: Session) -> None:
    for slot in sess.slots.values():
        if slot.status == "proposed":
            slot.proposed = None
            slot.status = "confirmed"
            sess.emit("rejected", f"{slot.name} 保持 {slot.value}")
    sess.phase = "confirmed" if not sess.missing() else "collecting"


def _session_to_dict(s: Session) -> dict[str, Any]:
    return {
        "session_id": s.session_id,
        "user_id": s.user_id,
        "goal": s.goal,
        "phase": s.phase,
        "version": s.version,
        "ttl_turns": s.ttl_turns,
        "slots": {k: vars(v) for k, v in s.slots.items()},
        "turns": [vars(t) for t in s.turns],
        "trace": [vars(e) for e in s.trace],
    }


def _session_from_dict(d: dict[str, Any]) -> Session:
    sess = Session(d["session_id"], d["user_id"], d["goal"], d["phase"], d["version"], ttl_turns=d.get("ttl_turns", 20))
    sess.slots = {k: Slot(**v) for k, v in d["slots"].items()}
    sess.turns = [Turn(**t) for t in d["turns"]]
    sess.trace = [Event(**e) for e in d["trace"]]
    return sess
