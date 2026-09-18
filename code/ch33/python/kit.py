"""第 33 章面试教具：概念卡、设计清单、排障树、tool call 解析与执行器。

教学假设：模型输出是意图，不是命令。可退 ≠ 已退。
事假现行 10 个工作日；废止稿 5 天不得上答案。
ORDER-88 未发货可退，写退款须白名单。Token 按字符数，教学币不是官网价。
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field

LEAVE10 = "事假每年累计不超过 10 个工作日。"
LEAVE5 = "【废止】事假不超过 5 天。"
ORDERS = {
    "ORDER-88": {"shipped": False, "text": "ORDER-88 未发货，可整单退款。"},
    "ORDER-99": {"shipped": True, "text": "ORDER-99 已发货，物流 TZ123，须走售后。"},
}

# --- 33.1 概念：浅答 must + 答深一层 ---

CONCEPTS = [
    {
        "id": "agent",
        "q": "Agent 和 Chatbot 差在哪？",
        "shallow": "Agent 能调用工具、围着目标转；Chatbot 主要是一问一答。",
        "deep": "差在闭环：观察→决策→行动→再观察。目标达成 ≠ 业务正确。林秋听到已办理，树上没有 refund_order，是模型撒谎。",
    },
    {
        "id": "rag",
        "q": "什么是 RAG？为什么不能闭卷答制度？",
        "shallow": "检索增强生成：先查资料再生成，减少胡说。",
        "deep": "检索到的是数据不是指令。废止稿 5 天若能被实习生看见，模型会抄。现行答案必须带引用，范围外说【不足】。",
    },
    {
        "id": "paradigm",
        "q": "ReAct、Plan-and-Execute、Reflexion 怎么选？",
        "shallow": "ReAct 边想边做；Plan 先列步骤再执行；Reflexion 做完复盘再改。",
        "deep": "复合办理用主编排看格子，不把话筒交给 Swarm。退款不靠多想一轮代替 HITL。",
    },
    {
        "id": "tool",
        "q": "工具调用里，模型输出算不算已经执行？",
        "shallow": "不算。模型只是提出要调哪个函数。",
        "deep": "模型输出是意图。执行器要解析、校验、鉴权、审计。money 工具不能 fail_open。可退 ≠ 已退。",
    },
]

DESIGNS = {
    "cs": {
        "name": "客服办理",
        "must": ["retrieve", "get_order", "hitl", "orchestrator"],
        "ban": ["swarm_owns_goal", "auto_refund"],
        "one": "查制度与订单分格，写退款人批，编排器拥有目标。",
    },
    "research": {
        "name": "研报摘录",
        "must": ["retrieve", "cite", "refuse"],
        "ban": ["write_tool", "closed_book_number"],
        "one": "只读检索带引用，数字必须能指回块；没有写工具。",
    },
    "ops": {
        "name": "运维值守",
        "must": ["read_metrics", "propose", "hitl"],
        "ban": ["auto_restart", "fail_open"],
        "one": "可读指标、可提议重启，执行变更必须人批，失败关闭。",
    },
}


def concept_ok(card: dict) -> bool:
    deep = card["deep"]
    need = {
        "agent": ["闭环", "业务正确"],
        "rag": ["数据不是指令", "【不足】"],
        "paradigm": ["HITL", "编排"],
        "tool": ["意图", "可退"],
    }[card["id"]]
    return all(x in deep for x in need)


# --- 33.3 排障 ---

@dataclass
class Span:
    name: str
    kind: str
    status: str = "ok"
    extra: str = ""


@dataclass
class Incident:
    id: str
    seen: str
    spans: list[Span]
    bill: float = 0.0
    hops: int = 1


INCIDENTS = [
    Incident(
        "hallucination",
        "已为您退款",
        [Span("get_order", "tool"), Span("llm", "llm")],
    ),
    Incident(
        "retrieve",
        "事假最多 5 天",
        [Span("retrieve", "retrieve", "ok", "[leave5]"), Span("llm", "llm")],
    ),
    Incident(
        "loop",
        "系统正在查询",
        [Span("get_order", "tool")] * 6,
        hops=6,
    ),
    Incident(
        "bill",
        "ORDER-88 可退须确认",
        [Span("retrieve", "retrieve")] * 3 + [Span("llm", "llm", "ok", "retry2")],
        bill=9.28,
    ),
]


def diagnose(inc: Incident) -> str:
    names = [s.name for s in inc.spans]
    if inc.seen.startswith("已") and "refund_order" not in names:
        return "MODEL_LIE"
    if any(s.extra == "[leave5]" for s in inc.spans) or "5 天" in inc.seen:
        return "RETRIEVE"
    if inc.hops >= 5 or names.count("get_order") >= 5:
        return "LOOP"
    if inc.bill >= 8 or names.count("retrieve") >= 3:
        return "BILL"
    return "TOOL"


# --- 33.4 解析 + 执行器 ---

CATALOG = {
    "get_order": {"risk": "read", "required": ["order_id"]},
    "search_policy": {"risk": "read", "required": ["query"]},
    "refund_order": {"risk": "money", "required": ["order_id"]},
}


@dataclass
class ToolCall:
    id: str
    name: str
    arguments: dict = field(default_factory=dict)
    error: str | None = None


def parse_tool_calls(raw: str | dict) -> list[ToolCall]:
    """解析 OpenAI 兼容 tool_calls。arguments 可以是字符串或对象。失败不抛到外层。"""
    try:
        payload = json.loads(raw) if isinstance(raw, str) else raw
        choices = payload.get("choices") or [{"message": payload.get("message", payload)}]
        message = choices[0].get("message") or payload
        items = message.get("tool_calls") or []
    except (json.JSONDecodeError, TypeError, IndexError, AttributeError, KeyError):
        return [ToolCall("parse", "?", {}, "PARSE_ERROR")]

    out: list[ToolCall] = []
    for i, item in enumerate(items):
        cid = str(item.get("id") or f"call_{i}")
        fn = item.get("function") or {}
        name = str(fn.get("name") or "")
        args_raw = fn.get("arguments", "{}")
        try:
            args = json.loads(args_raw) if isinstance(args_raw, str) else dict(args_raw)
            if not isinstance(args, dict):
                raise TypeError("args")
        except (json.JSONDecodeError, TypeError, ValueError):
            out.append(ToolCall(cid, name, {}, "BAD_ARGUMENTS"))
            continue
        out.append(ToolCall(cid, name, args))
    if not out:
        return [ToolCall("parse", "?", {}, "NO_TOOL_CALL")]
    return out


@dataclass
class ToolResult:
    ok: bool
    data: dict
    error: str | None = None


class Executor:
    def __init__(self, role: str = "intern") -> None:
        self.role = role
        self.audit: list[str] = []

    def run(self, call: ToolCall) -> ToolResult:
        if call.error:
            self.audit.append(f"{call.id}:{call.error}")
            return ToolResult(False, {}, call.error)
        spec = CATALOG.get(call.name)
        if spec is None:
            self.audit.append(f"{call.id}:UNKNOWN_TOOL")
            return ToolResult(False, {}, "UNKNOWN_TOOL")
        missing = [k for k in spec["required"] if k not in call.arguments]
        if missing:
            return ToolResult(False, {}, f"SCHEMA_ERROR:{missing}")
        if spec["risk"] == "money" and self.role != "agent_ops":
            self.audit.append(f"{call.id}:PERMISSION_DENIED")
            return ToolResult(False, {}, "PERMISSION_DENIED")
        if spec["risk"] == "money":
            return ToolResult(False, {}, "HITL_REQUIRED")
        if call.name == "get_order":
            oid = str(call.arguments["order_id"])
            row = ORDERS.get(oid)
            if not row:
                return ToolResult(False, {}, "NOT_FOUND")
            return ToolResult(True, {"text": row["text"], "shipped": row["shipped"]})
        if call.name == "search_policy":
            q = str(call.arguments["query"])
            text = LEAVE10 if "事假" in q else "【不足】"
            return ToolResult(True, {"text": text, "cite": "[leave10]" if "事假" in q else ""})
        return ToolResult(False, {}, "UNHANDLED")


BEHAVIOR = {
    "conflict": (
        "项目分歧",
        "同事要把 Swarm 当主编排。我坚持复合办理用格子，话筒不拥有目标。对照：同一句事假+ORDER-88，闲聊丢掉事假。最后按编排上线。",
    ),
    "incident": (
        "上线事故",
        "联调脚本把手机号打进日志。我先关出口，补脱敏和 401/429 金标，再复盘。结果：审计只留 [PHONE]，intern 第 4 次 429，退款仍须 HITL。",
    ),
    "learn": (
        "学习能力",
        "上周只会对着对话框猜。这周对着树问四句：哪条 Trace、哪个 Span 红、kind 是什么、用户那句话树上对得上吗。用 ORDER-88『已办理』对上 MODEL_LIE。",
    ),
}
