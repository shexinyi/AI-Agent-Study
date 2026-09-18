"""林秋世界：事假、订单、食堂。工人是脚本，不是 GPT。"""

from __future__ import annotations

from miniorch import Orchestrator, Task, Worker

LEAVE = "事假每年累计不超过 10 个工作日。"
ORDERS = {
    "ORDER-88": "ORDER-88 未发货，可整单退款。写退款须人工确认。",
    "ORDER-99": "ORDER-99 已发货，物流 TZ123，须走售后。",
}


def _oid(text: str) -> str:
    q = text.upper()
    if "TZ123" in q or "ORDER-99" in q:
        return "ORDER-99"
    if "ORDER-88" in q:
        return "ORDER-88"
    return ""


def solo_answer(goal: str) -> str:
    """单 Agent 天花板：后写的 if 覆盖前面的意图。这是事故，不是特性。"""
    text = "【不足】听不清要办哪一件。"
    if any(k in goal for k in ("事假", "请假", "工作日")):
        text = LEAVE
    if "ORDER" in goal.upper() or "TZ123" in goal.upper() or "退" in goal:
        oid = _oid(goal)
        text = ORDERS.get(oid, "【无资料】没有能对上的单号。")
    if "食堂" in goal:
        text = "食堂今天有红烧肉。"
    return text


def leave_worker() -> Worker:
    def fn(task: Task) -> str:
        if any(k in task.query for k in ("事假", "请假", "工作日")):
            if "2026-09-14" in task.query and "3" in task.query:
                return LEAVE + " 林秋 2026-09-14..16 共 3 天，未超上限，可请。"
            return LEAVE
        return "【无资料】不是事假问题。"

    return Worker("事假工人", "leave", fn)


def order_worker() -> Worker:
    def fn(task: Task) -> str:
        oid = _oid(task.query)
        if oid:
            return ORDERS[oid]
        return "【无资料】没有单号。"

    return Worker("订单工人", "order", fn)


def cafe_worker(*, explode: bool = False) -> Worker:
    def fn(task: Task) -> str:
        if explode:
            raise TimeoutError("食堂菜单服务超时 3000ms")
        return "【无资料】食堂菜单不在客服工具里，不能编。"

    return Worker("食堂工人", "cafe", fn)


def unknown_worker() -> Worker:
    def fn(task: Task) -> str:
        return "【不足】编排器拆不出对口任务，不能编。"

    return Worker("兜底工人", "unknown", fn)


def make_orch(*, policy: str = "isolate", cafe_explode: bool = False) -> Orchestrator:
    return Orchestrator(
        [
            leave_worker(),
            order_worker(),
            cafe_worker(explode=cafe_explode),
            unknown_worker(),
        ],
        policy=policy,  # type: ignore[arg-type]
    )


def expect_ok(report: str, tag: str) -> bool:
    if tag == "has_leave":
        return "10 个工作日" in report
    if tag == "has_88":
        return "ORDER-88" in report and "未发货" in report
    if tag == "has_99":
        return "ORDER-99" in report or "TZ123" in report
    if tag == "no_pork":
        return "红烧肉" not in report
    if tag == "isolated":
        return "【失败隔离】" in report
    if tag == "all_fail":
        return report.startswith("【整单失败】")
    if tag == "no_leave":
        return "10 个工作日" not in report
    return False
