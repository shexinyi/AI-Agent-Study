"""教学脚本模型 + 检索 + 订单工具。不是 API，也不是 LangChain 官方组件。"""

from __future__ import annotations

import json
import re
from pathlib import Path

from langchain_core.language_models.chat_models import SimpleChatModel
from langchain_core.messages import BaseMessage

KB = Path(__file__).resolve().parent / "kb"

ORDERS = {
    "ORDER-88": {"status": "unshipped", "refundable": True, "tracking": None},
    "ORDER-99": {"status": "shipped", "refundable": False, "tracking": "TZ123"},
}


def _text(message: BaseMessage) -> str:
    content = message.content
    return content if isinstance(content, str) else str(content)


class PolicyChat(SimpleChatModel):
    """
    按 Prompt 里已经出现的原文做抽取式回答。
    用来演示「链」而不是演示模型智力。生产里换成 ChatOpenAI 等。
    """

    def _call(
        self,
        messages: list[BaseMessage],
        stop: list[str] | None = None,
        run_manager=None,
        **kwargs,
    ) -> str:
        blob = "\n".join(_text(m) for m in messages)
        question = _text(messages[-1]) if messages else ""
        if "只输出 JSON" in blob:
            if "TZ123" in blob or "ORDER-99" in blob:
                return '{"tool":"get_order","order_id":"ORDER-99"}'
            return '{"tool":"get_order","order_id":"ORDER-88"}'
        if "【无资料】" in blob:
            return "【不足】资料不够，不能编天数或单号状态。"
        if any(k in question for k in ("年假", "食堂")) and "年假" not in blob.replace(question, ""):
            return "【不足】资料不够，不能编天数或单号状态。"
        if "refundable" in blob and "false" in blob.lower():
            return "根据工具，该单已发货，对话里不能直接退款，须走售后。"
        if "refundable" in blob and "true" in blob.lower():
            return "根据工具，该单未发货，用户本人可申请整单退款。"
        if "10 个工作日" in blob:
            return "根据资料，事假每年累计不超过 10 个工作日。"
        if "未发货" in blob and "ORDER-88" in blob:
            return "根据资料，ORDER-88 未发货时可申请整单退款。"
        if "TZ123" in blob or ("已发货" in blob and "ORDER-99" in blob):
            return "根据资料，TZ123 对应 ORDER-99，已发货须走售后。"
        return "【不足】资料里没有可引用的句子。"

    @property
    def _llm_type(self) -> str:
        return "policy-script"


def retrieve(question: str) -> str:
    """按问法抽一小节。教学用，不是第 15 章混合检索。"""
    chunks: list[tuple[str, str]] = []
    for path in sorted(KB.glob("*.md")):
        heading = path.stem
        buf: list[str] = []
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.startswith("## "):
                if buf:
                    chunks.append((heading, "\n".join(buf).strip()))
                heading = line[3:].strip()
                buf = []
            elif not line.startswith("# "):
                buf.append(line)
        if buf:
            chunks.append((heading, "\n".join(buf).strip()))

    q = question.upper()
    scored: list[tuple[int, str, str]] = []
    for heading, body in chunks:
        text = f"{heading}。{body}"
        score = 0
        if "ORDER-88" in q and "ORDER-88" in text.upper():
            score += 5
        if ("TZ123" in q or "ORDER-99" in q) and "TZ123" in text.upper():
            score += 5
        if any(k in question for k in ("事假", "请假", "工作日")) and "事假" in text:
            score += 4
        if "退" in question and "退" in text:
            score += 1
        if score:
            scored.append((score, heading, text))
    scored.sort(key=lambda x: x[0], reverse=True)
    return scored[0][2] if scored else ""


def get_order(order_id: str) -> str:
    row = ORDERS.get(order_id.upper())
    if not row:
        return json.dumps({"error": "not_found", "order_id": order_id}, ensure_ascii=False)
    return json.dumps({"order_id": order_id.upper(), **row}, ensure_ascii=False)


def run_tool(payload: dict) -> str:
    if payload.get("tool") != "get_order":
        raise ValueError(f"未知工具: {payload}")
    return get_order(str(payload.get("order_id", "")))


def graph_names(chain) -> list[str]:
    return [node.name for node in chain.get_graph().nodes.values()]


def order_id_of(question: str) -> str:
    found = re.findall(r"ORDER-\d+|TZ\d+", question.upper())
    if any(x.startswith("TZ") or x == "ORDER-99" for x in found):
        return "ORDER-99"
    return "ORDER-88"
