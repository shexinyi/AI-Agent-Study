"""项目 3 服务内核：鉴权 → 配额 → 办理 → 审计 / Trace / 账单。

教学假设：不拉 HTTP 端口，语义按 FastAPI / Spring 过滤器链。
Token 按字符数计；单价是课堂教学币，不是官网价。
可退 ≠ 已退。intern 不在 refund_order 白名单。
日志留 ORDER-88 / trace_id，不留手机号。
"""

from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass, field

RATES = {
    "large_in": 3.0,
    "large_out": 15.0,
    "small_in": 0.2,
    "small_out": 0.8,
    "retrieve": 1.0,
}

TOKENS = {
    "intern-lin": {"role": "intern", "daily_quota": 3, "label": "intern"},
    "ops-key": {"role": "agent_ops", "daily_quota": 20, "label": "ops"},
}

# FastAPI 做深时的中间件名；Spring Boot 对照见表。
PIPELINE = ["request_id", "auth_bearer", "quota_daily", "handler", "audit_redact", "bill_span"]

FAT_SYS = "请结合全部历史、制度全文、去年会议纪要与全部检索结果后再回答。\n" * 12

PHONE_RE = re.compile(r"1\d{10}")
EMAIL_RE = re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+")
ID_RE = re.compile(r"\b\d{17}[\dXx]\b")


def tokens(text: str) -> int:
    return max(1, len(text))


def fee(kind: str, n: int) -> float:
    if kind == "retrieve":
        return RATES["retrieve"] * n
    return RATES[kind] * n / 1000.0


def redact(text: str) -> str:
    text = PHONE_RE.sub("[PHONE]", text)
    text = EMAIL_RE.sub("[EMAIL]", text)
    return ID_RE.sub("[ID]", text)


def actor_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()[:8]


def bearer(headers: dict[str, str]) -> str:
    raw = headers.get("Authorization") or headers.get("authorization") or ""
    if raw.lower().startswith("bearer "):
        return raw[7:].strip()
    return raw.strip()


def handle_biz(question: str, role: str, confirm: str | None) -> dict:
    executed: list[str] = []
    parts: list[str] = []
    if any(k in question for k in ("事假", "请假", "工作日")):
        parts.append("事假每年累计不超过 10 个工作日。林秋 2026-09-14..16 共 3 天，未超上限。")
    u = question.upper()
    if "ORDER-99" in u or "TZ123" in u:
        parts.append("ORDER-99 已发货，物流 TZ123，须走售后。")
    elif "ORDER-88" in u or "退" in question:
        if "ORDER-88" in u:
            parts.append("ORDER-88 未发货，可整单退款。写退款须人工确认。")
            if "退" in question:
                if role != "agent_ops":
                    parts.append("【拒绝】intern 不在 refund_order 白名单。")
                elif confirm == "approve":
                    executed.append("refund_order:ORDER-88")
                    parts.append("已执行 refund_order(ORDER-88)。")
                elif confirm == "reject":
                    parts.append("【已拒绝】不执行写工具。")
                else:
                    parts.append("审批单 pending，等待确认。")
        else:
            parts.append("【澄清】请提供订单号。")
    if "食堂" in question:
        parts.append("【不足】食堂不在本服务范围。")
    if not parts:
        parts.append("【不足】不是本服务范围内的办理。")
    return {"answer": " ".join(parts), "executed": executed}


def make_bill(mode: str, question: str, answer: str) -> dict:
    lines: list[dict] = []

    def add_llm(model: str, prompt: str, output: str, retry: int = 0) -> None:
        inn, out = tokens(prompt), tokens(output)
        lines.append({"bucket": "input", "model": model, "tokens": inn, "amount": fee(f"{model}_in", inn)})
        lines.append({"bucket": "output", "model": model, "tokens": out, "amount": fee(f"{model}_out", out)})
        for i in range(retry):
            lines.append({"bucket": "retry", "model": model, "tokens": inn, "amount": fee(f"{model}_in", inn)})
            lines.append({"bucket": "retry", "model": model, "tokens": out, "amount": fee(f"{model}_out", out)})

    def add_retrieve(n: int) -> None:
        lines.append({"bucket": "retrieve", "model": "-", "tokens": n, "amount": fee("retrieve", n)})

    if mode == "fat":
        add_llm("large", FAT_SYS + question, answer, retry=2)
        add_retrieve(3)
    else:
        route = "leave" if "事假" in question else ("cafe" if "食堂" in question else "order")
        add_llm("small", "路由:" + question[:24], route)
        if route != "cafe":
            add_retrieve(1)
            add_llm("large", "短上下文\n" + question, answer, retry=0)
    amount = round(sum(x["amount"] for x in lines), 4)
    retrieve = sum(x["tokens"] for x in lines if x["bucket"] == "retrieve")
    retry = sum(1 for x in lines if x["bucket"] == "retry") // 2
    return {"amount": amount, "retrieve": retrieve, "retry": retry, "lines": lines}


@dataclass
class Response:
    status: int
    body: dict
    audit: dict


class AgentService:
    """进程内服务。生产换成 FastAPI 中间件或 Spring Filter，管道名不要改。"""

    def __init__(self) -> None:
        self.used: dict[str, int] = {}
        self.audits: list[dict] = []
        self.n = 0

    def handle(
        self,
        headers: dict[str, str],
        question: str,
        *,
        confirm: str | None = None,
        mode: str = "lean",
    ) -> Response:
        self.n += 1
        trace_id = f"tr-{self.n:04d}"
        token = bearer(headers)
        ident = TOKENS.get(token)
        if ident is None:
            audit = self._audit(trace_id, token or "anon", 401, question, 0.0, "unauthorized")
            return Response(401, {"error": "unauthorized", "trace_id": trace_id}, audit)

        used = self.used.get(token, 0)
        if used >= ident["daily_quota"]:
            audit = self._audit(trace_id, token, 429, question, 0.0, "quota_exceeded")
            return Response(
                429,
                {
                    "error": "quota_exceeded",
                    "trace_id": trace_id,
                    "quota_used": used,
                    "quota_limit": ident["daily_quota"],
                },
                audit,
            )

        self.used[token] = used + 1
        biz = handle_biz(question, ident["role"], confirm)
        bill = make_bill(mode, question, biz["answer"])
        body = {
            "trace_id": trace_id,
            "answer": biz["answer"],
            "executed": biz["executed"],
            "role": ident["role"],
            "quota_used": self.used[token],
            "quota_limit": ident["daily_quota"],
            "bill": {"amount": bill["amount"], "retrieve": bill["retrieve"], "retry": bill["retry"]},
            "spans": [
                {"name": "agent", "kind": "agent"},
                {"name": "auth", "kind": "auth", "status": "ok"},
                {"name": "quota", "kind": "quota", "status": "ok"},
                {"name": "retrieve" if bill["retrieve"] else "route", "kind": "tool"},
                {"name": "llm", "kind": "llm"},
                {"name": "hitl", "kind": "hitl", "status": "pending" if "pending" in biz["answer"] else "n/a"},
            ],
        }
        audit = self._audit(trace_id, token, 200, question, bill["amount"], "ok")
        return Response(200, body, audit)

    def _audit(self, trace_id: str, token: str, status: int, question: str, amount: float, action: str) -> dict:
        rec = {
            "trace_id": trace_id,
            "actor": actor_hash(token),
            "status": status,
            "action": action,
            "question": redact(question),
            "amount": amount,
        }
        self.audits.append(rec)
        return rec


def script_ask(question: str) -> str:
    """反面教材：本机脚本，无鉴权、无配额、审计不脱敏。"""
    biz = handle_biz(question, "intern", None)
    return f"[script] {biz['answer']} raw={question}"
