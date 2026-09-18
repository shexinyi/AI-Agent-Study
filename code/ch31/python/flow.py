"""项目 2 办理流：编排器拆格，写工具须 HITL，失败可演示。

教学假设：工人是脚本；没有三个 Bot 互道早安。
ORDER-88 未发货可退，ORDER-99 / TZ123 已发货须售后。
可退 ≠ 已退。intern 不在 refund 白名单。
"""

from __future__ import annotations

from dataclasses import dataclass, field

LEAVE = "事假每年累计不超过 10 个工作日。林秋 2026-09-14..16 共 3 天，未超上限。"
ORDERS = {
    "ORDER-88": "ORDER-88 未发货，可整单退款。写退款须人工确认。",
    "ORDER-99": "ORDER-99 已发货，物流 TZ123，须走售后。",
}


@dataclass
class Slot:
    kind: str
    status: str
    text: str = ""


@dataclass
class Ticket:
    id: str
    status: str
    order_id: str
    executed: bool = False


@dataclass
class Report:
    text: str
    slots: list[Slot] = field(default_factory=list)
    ticket: Ticket | None = None
    executed: list[str] = field(default_factory=list)


def oid(q: str) -> str:
    u = q.upper()
    if "TZ123" in u or "ORDER-99" in u:
        return "ORDER-99"
    if "ORDER-88" in u:
        return "ORDER-88"
    return ""


class Workflow:
    def __init__(self) -> None:
        self.n = 0
        self.tickets: dict[str, Ticket] = {}

    def run(self, question: str, role: str = "intern", *, confirm: str | None = None, cafe_boom: bool = False) -> Report:
        slots: list[Slot] = []
        if any(k in question for k in ("事假", "请假", "工作日")):
            slots.append(Slot("leave", "ok", LEAVE))
        if oid(question) or "退" in question:
            code = oid(question)
            if not code:
                slots.append(Slot("order", "clarify", "【澄清】请提供订单号。"))
            elif code == "ORDER-99":
                slots.append(Slot("order", "ok", ORDERS["ORDER-99"]))
            else:
                slots.append(Slot("order", "ok", ORDERS["ORDER-88"]))
        if "食堂" in question:
            if cafe_boom:
                slots.append(Slot("cafe", "failed", "【失败隔离】食堂菜单超时 3000ms"))
            else:
                slots.append(Slot("cafe", "ok", "【无资料】食堂不在办理范围。"))

        ticket = None
        executed: list[str] = []
        want_refund = "ORDER-88" in question.upper() and "退" in question
        if want_refund and all(s.kind != "order" or "未发货" in s.text for s in slots if s.kind == "order"):
            if role != "agent_ops":
                slots.append(Slot("hitl", "denied", "【拒绝】intern 不在 refund_order 白名单。"))
            elif confirm == "approve":
                self.n += 1
                ticket = Ticket(f"t{self.n}", "executed", "ORDER-88", True)
                self.tickets[ticket.id] = ticket
                executed.append("refund_order:ORDER-88")
                slots.append(Slot("hitl", "executed", "已执行 refund_order(ORDER-88)。"))
            elif confirm == "reject":
                slots.append(Slot("hitl", "rejected", "【已拒绝】不执行写工具。"))
            else:
                self.n += 1
                ticket = Ticket(f"t{self.n}", "pending", "ORDER-88")
                self.tickets[ticket.id] = ticket
                slots.append(Slot("hitl", "pending", f"审批单 {ticket.id} 等待确认。"))

        if not slots:
            return Report("【不足】不是办理范围内的跨系统工单。")
        lines = ["办理汇报："] + [f"- [{s.kind}] {s.text}" for s in slots]
        return Report("\n".join(lines), slots, ticket, executed)


def chitchat(question: str) -> str:
    """反面教材：三个机器人聊天，只留下最后一句。"""
    if "食堂" in question:
        return "客服C：食堂今天有红烧肉。"
    if "ORDER" in question.upper() or "退" in question:
        return "客服B：那您看要不要退款呀～"
    if "事假" in question:
        return "客服A：假期的事情我记下啦。"
    return "客服们：您好欢迎光临。"
