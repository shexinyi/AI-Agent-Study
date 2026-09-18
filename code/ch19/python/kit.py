"""角色脚本 + 订单工具 + 代码白名单。不 exec 模型给的源码。"""

from __future__ import annotations

import re

from minichat import Agent, Message, last_user

ORDERS = {
    "ORDER-88": "ORDER-88 未发货，可申请整单退款。写退款必须人工确认。",
    "ORDER-99": "ORDER-99 已发货，物流 TZ123，对话里不能直接退，须走售后。",
}

SAFE_DAYS = """from datetime import date
print((date(2026,9,16)-date(2026,9,14)).days+1)
"""


def _norm(src: str) -> str:
    return re.sub(r"\s+", "", src.strip())


ALLOWED_CODE = {_norm(SAFE_DAYS): "3"}
DANGER = ("os.system", "subprocess", "eval(", "exec(", "__import__", "open(", "socket", "requests")


def get_order(order_id: str) -> str:
    return ORDERS.get(order_id, f"{order_id} 查无此单。")


def order_id_of(text: str) -> str:
    if "TZ123" in text.upper() or "ORDER-99" in text.upper():
        return "ORDER-99"
    if "ORDER-88" in text.upper():
        return "ORDER-88"
    return ""


def run_whitelisted(src: str) -> str:
    """教学执行器：只回放白名单结果。模型源码默认不可信。"""
    if any(tok in src for tok in DANGER):
        return "REJECT: 发现危险调用，拒绝执行。"
    hit = ALLOWED_CODE.get(_norm(src))
    if hit is None:
        return "REJECT: 不在白名单，拒绝执行。"
    return hit


def policy_reply(messages: list[Message]) -> str:
    q = last_user(messages)
    blob = "\n".join(m.content for m in messages)
    if "REJECT" in blob:
        return "【不足】代码未执行，不能拿结果编天数。 TERMINATE"
    if "执行结果: 3" in blob:
        return "工具算出 3 天，未超事假每年 10 个工作日上限。 TERMINATE"
    if any(k in q for k in ("年假", "食堂")):
        return "【不足】资料只覆盖事假。 TERMINATE"
    if any(k in q for k in ("事假", "请假", "工作日")):
        return "根据制度，事假每年累计不超过 10 个工作日。 TERMINATE"
    return "政策岗不处理订单。交给订单岗。"


def clerk_reply(messages: list[Message]) -> str:
    blob = "\n".join(m.content for m in messages)
    tool_done = any(m.name == "用户代理" and ("未发货" in m.content or "已发货" in m.content or "查无" in m.content) for m in messages)
    if not tool_done:
        oid = order_id_of(blob)
        if not oid:
            return "订单岗需要单号。 TERMINATE"
        return f"请用户代理查单。 TOOL:get_order:{oid}"
    if "未发货" in blob:
        return "根据工具，ORDER-88 未发货，用户本人可申请整单退款。写工具前须人工确认。 TERMINATE"
    if "已发货" in blob or "TZ123" in blob:
        return "根据工具，TZ123 对应 ORDER-99，已发货须走售后。 TERMINATE"
    return "根据工具，查无此单。 TERMINATE"


def proxy_reply(messages: list[Message]) -> str:
    last = messages[-1].content
    found = re.search(r"TOOL:get_order:(ORDER-\d+)", last)
    if found:
        return get_order(found.group(1))
    if "```" in last:
        src = last.split("```")[1]
        src = re.sub(r"^python", "", src, flags=re.I).strip()
        return "执行结果: " + run_whitelisted(src)
    return "用户代理无动作。"


def coder_reply(messages: list[Message]) -> str:
    q = last_user(messages)
    if "危险" in q or "pwn" in q.lower():
        return "```python\nimport os\nos.system('echo pwn')\n```"
    return "```python\n" + SAFE_DAYS + "```"


def policy_agent() -> Agent:
    return Agent("政策岗", "只回答请假制度，不编数字。", policy_reply)


def clerk_agent() -> Agent:
    return Agent("订单岗", "订单必须先查工具。", clerk_reply)


def proxy_agent() -> Agent:
    return Agent("用户代理", "只执行 TOOL 与白名单代码。", proxy_reply)


def coder_agent() -> Agent:
    return Agent("代码岗", "只写计算天数的短脚本。", coder_reply)
