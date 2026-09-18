"""林秋世界的蜂群角色。专家是脚本，不是 GPT。"""

from __future__ import annotations

from miniswarm import Agent, Reply, Swarm

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


def _has_leave(text: str) -> bool:
    return any(k in text for k in ("事假", "请假", "工作日"))


def _has_order(text: str) -> bool:
    return "ORDER" in text.upper() or "TZ123" in text.upper() or "退" in text


def receptionist() -> Agent:
    def fn(text: str) -> Reply:
        if "食堂" in text:
            return Reply("请食堂专家接一下。", handoff="食堂专家")
        if _has_order(text):
            return Reply("请订单专家接一下。", handoff="订单专家")
        if _has_leave(text):
            return Reply("请事假专家接一下。", handoff="事假专家")
        return Reply("【不足】前台听不清要找哪一岗。")

    return Agent("前台", (), fn)


def leave_expert(*, bounce: bool = False) -> Agent:
    def fn(text: str) -> Reply:
        if bounce and _has_order(text):
            return Reply(text, handoff="订单专家")
        if _has_leave(text):
            return Reply(LEAVE)
        return Reply("【无资料】不是事假问题。")

    return Agent("事假专家", ("leave", "all"), fn)


def order_expert(*, bounce: bool = False, sneak_refund: bool = False) -> Agent:
    def fn(text: str) -> Reply:
        if sneak_refund and "ORDER-88" in text.upper() and "退" in text:
            return Reply("退款单我来办。", handoff="退款机器人")
        if bounce and _has_leave(text):
            return Reply(text, handoff="事假专家")
        oid = _oid(text)
        if oid:
            return Reply(ORDERS[oid])
        return Reply("【无资料】没有单号。")

    return Agent("订单专家", ("orders", "all"), fn)


def cafe_expert(*, hallucinate: bool = False) -> Agent:
    def fn(text: str) -> Reply:
        if hallucinate and (_has_order(text) or _has_leave(text)):
            return Reply("食堂今天有红烧肉。")
        if "食堂" in text:
            return Reply("【无资料】食堂菜单不在客服工具里，不能编。")
        return Reply("【无资料】食堂岗不接这句。")

    return Agent("食堂专家", ("cafe", "all"), fn)


def refund_bot() -> Agent:
    def fn(text: str) -> Reply:
        return Reply("已为 ORDER-88 办理退款，三天到账。")

    return Agent("退款机器人", (), fn)


STRICT_ALLOW = {
    "前台": ("事假专家", "订单专家", "食堂专家"),
    "事假专家": ("前台",),
    "订单专家": ("前台",),
    "食堂专家": ("前台",),
}

OPEN_ALLOW = {
    "前台": ("事假专家", "订单专家", "食堂专家", "退款机器人"),
    "事假专家": ("前台", "订单专家", "食堂专家"),
    "订单专家": ("前台", "事假专家", "退款机器人"),
    "食堂专家": ("前台",),
    "退款机器人": (),
}


def make_swarm(*, bounce: bool = False, sneak_refund: bool = False, open_route: bool | None = None) -> Swarm:
    if open_route is None:
        open_route = bounce
    allow = OPEN_ALLOW if open_route else STRICT_ALLOW
    return Swarm(
        [
            receptionist(),
            leave_expert(bounce=bounce),
            order_expert(bounce=bounce, sneak_refund=sneak_refund),
            cafe_expert(),
            refund_bot(),
        ],
        allow,
        max_hops=6,
    )


def expect_ok(text: str, tag: str) -> bool:
    if tag == "has_leave":
        return "10 个工作日" in text
    if tag == "has_88":
        return "ORDER-88" in text and "未发货" in text
    if tag == "has_99":
        return "ORDER-99" in text or "TZ123" in text
    if tag == "no_pork":
        return "红烧肉" not in text
    if tag == "has_pork":
        return "红烧肉" in text
    if tag == "no_leave":
        return "10 个工作日" not in text
    if tag == "max_hops":
        return text.startswith("【超过 max_hops")
    if tag == "illegal":
        return text.startswith("【非法交接】")
    if tag == "fake_refund":
        return "已为 ORDER-88 办理退款" in text
    if tag == "must_stop":
        return "【无资料】" in text or "【不足】" in text
    return False
