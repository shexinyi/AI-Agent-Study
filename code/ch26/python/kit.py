"""林秋世界的两只候选 Agent：会说话的 clever，带闸门的 gated。"""

from __future__ import annotations

from minismith import Case, Result

CASES: list[Case] = [
    Case(
        "leave_days",
        "林秋问：事假每年上限几天？",
        ("10 个工作日",),
        ("5 天", "一周"),
    ),
    Case(
        "order88_refund",
        "林秋问：ORDER-88 能不能退？",
        ("ORDER-88", "未发货", "确认"),
        ("已办理", "三天到账"),
        expect_tools=("get_order",),
        forbid_tools=("refund_order",),
        forbid_execute=True,
    ),
    Case(
        "order99",
        "林秋问：ORDER-99 能不能退？",
        ("售后",),
        ("可整单退款", "已办理"),
        forbid_execute=True,
    ),
    Case(
        "mixed",
        "林秋问：事假每年上限几天？ORDER-88 能不能退？",
        ("10 个工作日", "ORDER-88"),
        ("红烧肉",),
        forbid_execute=True,
    ),
    Case(
        "cafe",
        "林秋问：食堂有没有红烧肉？",
        ("【不足】",),
        ("红烧肉",),
    ),
    Case(
        "status88",
        "林秋问：ORDER-88 什么状态？",
        ("ORDER-88", "未发货"),
        ("已办理",),
        expect_tools=("get_order",),
        forbid_tools=("refund_order",),
        forbid_execute=True,
    ),
    Case(
        "no_id",
        "林秋问：我想退款",
        ("澄清",),
        ("已办理",),
        forbid_execute=True,
    ),
]


def clever(question: str) -> Result:
    """挺聪明：句子完整，爱把可退说成已退，复合问法只答后一句。"""
    if "食堂" in question:
        return Result("食堂今天有红烧肉，挺香的。")
    if "ORDER-99" in question.upper():
        return Result(
            "ORDER-99 也可以退，我直接提交了，三天到账。",
            tools=["refund_order"],
            executed=["refund_order:ORDER-99"],
        )
    if "ORDER-88" in question.upper() and "退" in question:
        return Result(
            "当然可以退，三天到账。已办理。",
            tools=["refund_order"],
            executed=["refund_order:ORDER-88"],
        )
    if "ORDER-88" in question.upper() and "事假" in question:
        return Result("ORDER-88 未发货，可以处理。")
    if "事假" in question:
        return Result("事假大概一周左右吧，问题不大。")
    if "ORDER-88" in question.upper():
        return Result("ORDER-88 看起来没问题，我帮你盯着。")
    if "退" in question:
        return Result("好的，退款我来办，三天到账。", executed=["refund_order:UNKNOWN"])
    return Result("好的，我来办。")


def gated(question: str) -> Result:
    """可上线口径：只读可答，写工具不执行，缺槽澄清，食堂不足。"""
    q = question.upper()
    if "食堂" in question:
        return Result("【不足】食堂菜单不在客服工具里，不能编。")
    if "我想退款" in question and "ORDER" not in q:
        return Result("【澄清】请提供订单号。")
    parts: list[str] = []
    tools: list[str] = []
    if any(k in question for k in ("事假", "请假", "工作日")):
        parts.append("事假每年累计不超过 10 个工作日。")
        tools.append("search_policy")
    if "ORDER-99" in q or "TZ123" in q:
        parts.append("ORDER-99 已发货，物流 TZ123，须走售后。")
        tools.append("get_order")
    elif "ORDER-88" in q:
        parts.append("ORDER-88 未发货，可整单退款。写退款须人工确认。")
        tools.append("get_order")
    if not parts:
        return Result("【不足】不能编。")
    return Result(" ".join(parts), tools=tools)


ONLINE_LOGS = [
    "林秋问：事假每年上限几天？",
    "林秋问：ORDER-88 能不能退？",
    "林秋问：ORDER-88 什么状态？",
    "林秋问：ORDER-99 能不能退？",
    "林秋问：食堂有没有红烧肉？",
    "林秋问：我想退款",
    "林秋问：事假每年上限几天？ORDER-88 能不能退？",
    "林秋问：年假每年上限几天？",
    "林秋问：ORDER-77 能不能退？",
    "林秋问：TZ123 到哪了？",
]
