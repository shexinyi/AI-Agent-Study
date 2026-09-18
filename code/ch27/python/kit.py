"""林秋世界的几条轨迹。Token 按字符数计。"""

from __future__ import annotations

from minitrace import Tracer


def tokens(text: str) -> int:
    return max(1, len(text))


def healthy_refund_trace() -> Tracer:
    tr = Tracer()
    tr.begin("tr-88", "usr_linqiu", "林秋问：ORDER-88 能不能退？")
    root = tr.open("agent", "agent")
    ret = tr.open("search_policy", "retrieve", root.span_id)
    tr.close(ret, "ok", took=12, tokens_in=8, tokens_out=0, title="现行事假")
    tool = tr.open("get_order", "tool", root.span_id)
    tr.close(tool, "ok", took=25, order_id="ORDER-88", shipped=False)
    llm = tr.open("draft", "llm", root.span_id)
    out = "ORDER-88 未发货，可整单退款。写退款须人工确认。"
    tr.close(llm, "ok", took=80, tokens_in=tokens("ORDER-88"), tokens_out=tokens(out), output=out)
    hitl = tr.open("approve_refund", "hitl", root.span_id)
    tr.close(hitl, "ok", took=5, decision="pending")
    tr.close(root, "ok", took=1)
    return tr


def incident_tool_timeout() -> Tracer:
    tr = Tracer()
    tr.begin("tr-timeout", "usr_linqiu", "林秋问：ORDER-88 帮我退。")
    root = tr.open("agent", "agent")
    tool = tr.open("get_order", "tool", root.span_id)
    tr.close(tool, "ok", took=20, order_id="ORDER-88")
    refund = tr.open("refund_order", "tool", root.span_id)
    tr.close(refund, "error", took=3000, error="TimeoutError: 3000ms", order_id="ORDER-88")
    llm = tr.open("apology", "llm", root.span_id)
    tr.close(llm, "ok", took=30, tokens_in=10, tokens_out=8, output="系统出错，请稍后重试。")
    tr.close(root, "error", took=1)
    return tr


def incident_model_lie() -> Tracer:
    tr = Tracer()
    tr.begin("tr-lie", "usr_linqiu", "林秋问：ORDER-88 能不能退？")
    root = tr.open("agent", "agent")
    llm = tr.open("draft", "llm", root.span_id)
    out = "当然可以退，三天到账。已办理。"
    tr.close(llm, "ok", took=70, tokens_in=12, tokens_out=tokens(out), output=out)
    tr.close(root, "ok", took=1)
    return tr


def incident_stale_doc() -> Tracer:
    tr = Tracer()
    tr.begin("tr-stale", "usr_linqiu", "林秋问：事假每年上限几天？")
    root = tr.open("agent", "agent")
    ret = tr.open("search_policy", "retrieve", root.span_id)
    tr.close(ret, "ok", took=15, deprecated=True, title="废止稿 5 天")
    llm = tr.open("draft", "llm", root.span_id)
    out = "根据资料，事假不超过 5 天。"
    tr.close(llm, "ok", took=40, tokens_in=20, tokens_out=tokens(out), output=out)
    tr.close(root, "ok", took=1)
    return tr


RAW_LOG = (
    "user=林秋 phone=13800138000 id=110101200001011234 "
    "email=linqiu@example.com bank=6222021234567890123 "
    "问：ORDER-88 能不能退？"
)
