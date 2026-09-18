"""人类介入教具：分类、工单、恢复、超时。

教学假设：人的决定由脚本传入，没有真的 IM / 工单系统。
时钟可拨快。写工具只有在 status=approved 之后才执行，且按单号幂等。
资金动作禁止 fail_open。Java 同构，不是 LangGraph interrupt 的官方 API。
"""

from __future__ import annotations

from dataclasses import dataclass, field


READ = "read"
MONEY = "money"
WRITE = "write"

AUTO = "auto"
APPROVE = "approve"
CLARIFY = "clarify"
TAKEOVER = "takeover"
REFUSE = "refuse"
REPLAY = "replay"

TICKET = "ticket"
IM = "im"
CONSOLE = "console"

FAIL_CLOSED = "fail_closed"
FAIL_OPEN = "fail_open"
ESCALATE = "escalate"


class PolicyError(ValueError):
    pass


@dataclass
class Action:
    name: str
    args: dict
    risk: str


@dataclass
class Ticket:
    id: str
    mode: str
    channel: str
    action: Action
    status: str
    created_ms: int
    deadline_ms: int
    timeout_policy: str
    payload: dict = field(default_factory=dict)
    note: str = ""


class Clock:
    def __init__(self, t: int = 0) -> None:
        self.t = t

    def now(self) -> int:
        return self.t

    def advance(self, ms: int) -> None:
        self.t += ms


class HitlRuntime:
    def __init__(self, clock: Clock | None = None) -> None:
        self.clock = clock or Clock()
        self.tickets: dict[str, Ticket] = {}
        self.seq = 0
        self.executed: dict[str, str] = {}
        self.log: list[str] = []

    def classify(self, user: str, action: Action | None = None) -> str:
        if any(k in user for k in ("律师", "投诉", "报警")):
            return TAKEOVER
        if "食堂" in user:
            return REFUSE
        oid = (action.args.get("order_id") if action else "") or ""
        if (action and action.name == "refund_order" and not oid and not _has_order(user)) or (
            action is None and "退" in user and not _has_order(user)
        ):
            return CLARIFY
        if any(k in user for k in ("请假", "事假")) and "2026-09-14" not in user and (
            action is None or action.name == "create_leave"
        ):
            return CLARIFY
        if action is None:
            if "退" in user and "ORDER-99" in user.upper():
                return REFUSE
            if "退" in user and "ORDER-88" in user.upper():
                return APPROVE
            if any(k in user for k in ("事假", "请假", "ORDER", "TZ123")):
                return AUTO
            return REFUSE
        if action.risk == READ:
            return AUTO
        if action.name == "refund_order" and (oid == "ORDER-99" or "ORDER-99" in user.upper()):
            return REFUSE
        if action.risk in (MONEY, WRITE):
            return APPROVE
        return AUTO

    def submit(
        self,
        mode: str,
        channel: str,
        action: Action,
        *,
        wait_ms: int = 60_000,
        timeout_policy: str = FAIL_CLOSED,
        payload: dict | None = None,
    ) -> Ticket:
        if action.risk == MONEY and timeout_policy == FAIL_OPEN:
            raise PolicyError("资金动作禁止 fail_open")
        self.seq += 1
        now = self.clock.now()
        ticket = Ticket(
            id=f"hitl-{self.seq}",
            mode=mode,
            channel=channel,
            action=action,
            status="pending",
            created_ms=now,
            deadline_ms=now + wait_ms,
            timeout_policy=timeout_policy,
            payload=payload or {},
        )
        self.tickets[ticket.id] = ticket
        self.log.append(f"submit {ticket.id} {mode}/{channel} {action.name}")
        return ticket

    def resume(self, ticket_id: str, decision: str, note: str = "") -> str:
        ticket = self.tickets[ticket_id]
        if ticket.status != "pending":
            return f"【不能恢复】{ticket.id} 已是 {ticket.status}"
        ticket.note = note
        if decision == "approve":
            ticket.status = "approved"
            return self._execute(ticket)
        if decision == "reject":
            ticket.status = "rejected"
            self.log.append(f"reject {ticket.id}")
            return "【已拒绝】不执行写工具。"
        if decision == "clarify":
            ticket.status = "clarified"
            ticket.payload["answer"] = note
            if note.upper().startswith("ORDER-88"):
                ticket.action.args["order_id"] = "ORDER-88"
                ticket.mode = APPROVE
                ticket.channel = TICKET
                ticket.status = "pending"
                self.log.append(f"clarify→approve {ticket.id}")
                return f"{ticket.id} 槽位已补，仍须审批。"
            return f"{ticket.id} 已澄清：{note}"
        if decision == "takeover":
            ticket.status = "taken_over"
            self.log.append(f"takeover {ticket.id}")
            return "【已接管】Agent 停手，由控制台人工继续。"
        return f"【不足】不认识的决定 {decision}"

    def replay(self, ticket_id: str) -> str:
        ticket = self.tickets[ticket_id]
        snap = (
            f"回放 {ticket.id} mode={ticket.mode} status={ticket.status} "
            f"action={ticket.action.name} {ticket.action.args}"
        )
        if ticket.action.risk in (MONEY, WRITE) and ticket.status in ("approved", "executed"):
            return snap + " | " + self._execute(ticket)
        return snap

    def tick(self) -> list[str]:
        reports: list[str] = []
        now = self.clock.now()
        for ticket in list(self.tickets.values()):
            if ticket.status != "pending" or now < ticket.deadline_ms:
                continue
            if ticket.timeout_policy == FAIL_OPEN:
                raise PolicyError("资金动作禁止 fail_open")
            if ticket.timeout_policy == ESCALATE:
                ticket.status = "escalated"
                parent = ticket.id
                self.seq += 1
                boss = Ticket(
                    id=f"hitl-{self.seq}",
                    mode=APPROVE,
                    channel=CONSOLE,
                    action=ticket.action,
                    status="pending",
                    created_ms=now,
                    deadline_ms=now + 60_000,
                    timeout_policy=FAIL_CLOSED,
                    payload={"escalated_from": parent},
                )
                self.tickets[boss.id] = boss
                msg = f"【已升级】{ticket.id} → {boss.id}，仍未执行写工具。"
            else:
                ticket.status = "timed_out"
                msg = f"【超时关闭】{ticket.id} 未执行 {ticket.action.name}。"
            self.log.append(msg)
            reports.append(msg)
        return reports

    def _execute(self, ticket: Ticket) -> str:
        action = ticket.action
        key = f"{action.name}:{action.args.get('order_id') or action.args.get('leave_id') or action.name}"
        if key in self.executed:
            self.log.append(f"idempotent {key}")
            return f"【幂等】{key} 已执行过：{self.executed[key]}"
        if action.name == "refund_order":
            oid = action.args.get("order_id", "")
            if oid != "ORDER-88":
                return "【不足】只能退未发货的 ORDER-88。"
            text = "已执行 refund_order(ORDER-88)。"
        elif action.name == "create_leave":
            text = "已执行 create_leave(林秋, 2026-09-14..16, 3天)。"
        else:
            text = f"已执行 {action.name}。"
        self.executed[key] = text
        ticket.status = "executed"
        self.log.append(f"exec {key}")
        return text


def _has_order(text: str) -> bool:
    q = text.upper()
    return "ORDER" in q or "TZ123" in q
