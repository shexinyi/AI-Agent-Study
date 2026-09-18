"""可观测教具：Trace / Span / 脱敏 / 导出。

教学假设：时钟可拨；Token 按字符数计，不是 tiktoken。
不接 LangSmith / OpenTelemetry SDK。导出的 JSON 行只是对接示意。
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, field


@dataclass
class Span:
    span_id: str
    parent_id: str
    name: str
    kind: str  # agent | llm | tool | retrieve | hitl
    status: str = "unset"
    start_ms: int = 0
    end_ms: int = 0
    attrs: dict = field(default_factory=dict)

    @property
    def latency_ms(self) -> int:
        return max(0, self.end_ms - self.start_ms)


@dataclass
class Trace:
    trace_id: str
    user_id: str
    question: str
    spans: list[Span] = field(default_factory=list)

    def tokens(self) -> tuple[int, int]:
        inn = sum(int(s.attrs.get("tokens_in", 0)) for s in self.spans)
        out = sum(int(s.attrs.get("tokens_out", 0)) for s in self.spans)
        return inn, out

    def tree(self) -> str:
        by_parent: dict[str, list[Span]] = {}
        for span in self.spans:
            by_parent.setdefault(span.parent_id, []).append(span)
        lines = [f"trace {self.trace_id} user={self.user_id} q={self.question}"]

        def walk(parent: str, indent: int) -> None:
            for span in by_parent.get(parent, []):
                inn = span.attrs.get("tokens_in", 0)
                out = span.attrs.get("tokens_out", 0)
                extra = ""
                if inn or out:
                    extra = f" tok={inn}/{out}"
                err = span.attrs.get("error", "")
                if err:
                    extra += f" err={err}"
                lines.append(
                    f"{'  ' * indent}- [{span.kind}] {span.name} {span.status} "
                    f"{span.latency_ms}ms{extra}"
                )
                walk(span.span_id, indent + 1)

        walk("", 0)
        return "\n".join(lines)


class Clock:
    def __init__(self, t: int = 0) -> None:
        self.t = t

    def advance(self, ms: int) -> int:
        self.t += ms
        return self.t


class Tracer:
    def __init__(self, clock: Clock | None = None) -> None:
        self.clock = clock or Clock()
        self.trace: Trace | None = None
        self._n = 0

    def begin(self, trace_id: str, user_id: str, question: str) -> Trace:
        self.trace = Trace(trace_id, user_id, question)
        self._n = 0
        return self.trace

    def open(self, name: str, kind: str, parent_id: str = "") -> Span:
        assert self.trace is not None
        self._n += 1
        span = Span(f"s{self._n}", parent_id, name, kind, start_ms=self.clock.t)
        self.trace.spans.append(span)
        return span

    def close(self, span: Span, status: str = "ok", **attrs: object) -> None:
        span.status = status
        span.end_ms = self.clock.advance(int(attrs.pop("took", 10)))
        span.attrs.update(attrs)


PHONE = re.compile(r"1[3-9]\d{9}")
ID18 = re.compile(r"(?<!\d)\d{17}[\dXx](?!\d)")
EMAIL = re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+")
BANK = re.compile(r"6222\d{12,15}")


def redact(text: str) -> str:
    text = PHONE.sub("[PHONE]", text)
    text = BANK.sub("[BANK]", text)
    text = ID18.sub("[ID]", text)
    text = EMAIL.sub("[EMAIL]", text)
    return text


def leak(text: str) -> list[str]:
    found: list[str] = []
    if PHONE.search(text):
        found.append("phone")
    if ID18.search(text):
        found.append("id")
    if EMAIL.search(text):
        found.append("email")
    if BANK.search(text):
        found.append("bank")
    return found


def otel_lines(trace: Trace) -> list[str]:
    """OpenTelemetry 风格的一行一个 Span，给现有 APM 对齐用。"""
    rows = []
    for span in trace.spans:
        rows.append(
            json.dumps(
                {
                    "trace_id": trace.trace_id,
                    "span_id": span.span_id,
                    "parent_span_id": span.parent_id or None,
                    "name": span.name,
                    "kind": span.kind,
                    "status": span.status,
                    "latency_ms": span.latency_ms,
                    "tokens_in": span.attrs.get("tokens_in", 0),
                    "tokens_out": span.attrs.get("tokens_out", 0),
                    "error": span.attrs.get("error", ""),
                },
                ensure_ascii=False,
            )
        )
    return rows


def locate(trace: Trace) -> tuple[str, str]:
    """五分钟定位：先看红 Span 的 kind，再看模型有没有空口说已办理。"""
    for span in trace.spans:
        if span.status == "error" and span.kind != "agent":
            return "TOOL", f"{span.name} error={span.attrs.get('error')}"
        if span.kind == "retrieve" and span.attrs.get("deprecated") in (True, "true"):
            return "RETRIEVE", f"{span.name} deprecated 命中废止稿"
    llm_text = " ".join(str(s.attrs.get("output", "")) for s in trace.spans if s.kind == "llm")
    refunded = any(
        s.kind == "tool" and s.name == "refund_order" and s.status == "ok" for s in trace.spans
    )
    if any(k in llm_text for k in ("已办理", "三天到账")) and not refunded:
        return "MODEL_LIE", "llm 说已办理，但没有成功的 refund_order span"
    return "OK", "树上没有红灯，输出能对上工具"
