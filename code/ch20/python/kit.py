"""林秋世界的角色脚本。工具仍是事实源。"""

from __future__ import annotations

from minicrew import Agent

LEAVE = "事假每年累计不超过 10 个工作日。"
ORDERS = {
    "ORDER-88": "ORDER-88 未发货，可整单退款。写退款须人工确认。",
    "ORDER-99": "ORDER-99 已发货，物流 TZ123，须走售后。",
}


def _oid(question: str) -> str:
    q = question.upper()
    if "TZ123" in q or "ORDER-99" in q:
        return "ORDER-99"
    if "ORDER-88" in q:
        return "ORDER-88"
    return ""


def researcher(kind: str) -> Agent:
    def work(desc: str, blob: dict) -> str:
        q = str(blob["question"])
        if kind == "leave":
            if any(k in q for k in ("事假", "请假", "工作日")):
                return LEAVE
            return "【无资料】不是事假问题。"
        oid = _oid(q)
        if oid:
            return ORDERS[oid]
        return "【无资料】没有单号。"

    title = "事假研究员" if kind == "leave" else "订单研究员"
    return Agent(title, "只检索自己那一类资料", "不跨界编造", work)


def writer() -> Agent:
    def work(desc: str, blob: dict) -> str:
        ctx = " ".join(blob["context"].values())
        q = str(blob["question"])
        if "10 个工作日" in ctx and any(k in q for k in ("事假", "请假", "工作日", "几天")):
            return "根据资料，事假每年累计不超过 10 个工作日。"
        if "ORDER-88" in ctx and "未发货" in ctx:
            return "根据工具，ORDER-88 未发货可退，写工具前须确认。"
        if "TZ123" in ctx or "ORDER-99" in ctx:
            return "根据工具，TZ123 对应 ORDER-99，已发货须走售后。"
        if "【无资料】" in ctx or any(k in q for k in ("食堂", "年假")):
            return "【不足】资料不够，不能编。"
        return "【不足】写手看不见能用的上下文。"

    return Agent("写手", "只引用上下文", "不闭卷发挥", work)


def manager() -> Agent:
    def work(desc: str, blob: dict) -> str:
        q = desc
        if any(k in q for k in ("事假", "请假", "工作日", "年假", "食堂", "几天")) and "ORDER" not in q.upper() and "TZ" not in q.upper():
            if any(k in q for k in ("年假", "食堂")):
                return "写手"
            return "事假研究员"
        if "ORDER" in q.upper() or "TZ" in q.upper() or "退" in q:
            return "订单研究员"
        return "写手"

    return Agent("经理", "把任务派给对口专家", "自己不编事实", work)


def expected_ok(output: str, expected: str) -> bool:
    if expected == "must_10":
        return "10 个工作日" in output and "5 天" not in output
    if expected == "must_88":
        return "ORDER-88" in output and "未发货" in output
    if expected == "must_99":
        return "ORDER-99" in output or "TZ123" in output
    if expected == "must_stop":
        return output.startswith("【不足】")
    return True
