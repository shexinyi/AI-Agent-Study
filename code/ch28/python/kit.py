"""林秋世界的费路径：浪费的 fat、分级的 lean。"""

from __future__ import annotations

from minibill import Latency, Run, add_llm, add_retrieve

LEAVE = "事假每年累计不超过 10 个工作日。"
ORDER88 = "ORDER-88 未发货，可整单退款。写退款须人工确认。"
CAFE = "【不足】食堂菜单不在客服工具里，不能编。"
FAT_PROMPT = "你是全能客服。" + ("请结合全部历史与制度全文详细分析。" * 8)


def route(question: str) -> str:
    if "食堂" in question:
        return "cafe"
    if "ORDER" in question.upper() or "TZ123" in question.upper() or "退" in question:
        return "order"
    if any(k in question for k in ("事假", "请假", "工作日")):
        return "leave"
    return "other"


def fat(question: str) -> Run:
    """全程大模型：长 Prompt、检索 3 次、再重试 2 次。"""
    intent = route(question)
    text = { "leave": LEAVE, "order": ORDER88, "cafe": "食堂今天有红烧肉。", "other": "好的。" }[intent]
    run = Run(text, "large")
    prompt = FAT_PROMPT + question
    add_retrieve(run, 3)
    add_llm(run, "large", prompt, text, retry=2)
    run.tools = ["search_policy", "get_order", "search_policy"]
    run.latency = Latency(queue_ms=50, model_ms=240, tool_ms=90, net_ms=40, ttft_ms=50 + 240)
    return run


_cache: dict[str, str] = {}


def lean(question: str, *, use_cache: bool = True) -> Run:
    """小模型分流；只有退款决策上大模型；制度问答可缓存；食堂提前终止。"""
    intent = route(question)
    run = Run("", "small")
    if use_cache and intent == "leave" and question in _cache:
        run.text = _cache[question]
        run.cached = True
        run.model = "cache"
        run.latency = Latency(queue_ms=5, model_ms=0, tool_ms=0, net_ms=2, ttft_ms=5)
        return run

    cls_out = intent
    add_llm(run, "small", question, cls_out)
    run.latency = Latency(queue_ms=20, model_ms=15, tool_ms=0, net_ms=8, ttft_ms=20 + 8)

    if intent == "cafe":
        run.text = CAFE
        return run
    if intent == "leave":
        add_retrieve(run, 1)
        run.tools = ["search_policy"]
        run.text = LEAVE
        run.latency.tool_ms = 12
        run.latency.net_ms += 5
        if use_cache:
            _cache[question] = run.text
        return run
    if intent == "order":
        add_retrieve(run, 1)
        add_llm(run, "large", question + "\n查订单", ORDER88)
        run.model = "large"
        run.tools = ["get_order"]
        run.text = ORDER88
        run.latency.model_ms += 80
        run.latency.tool_ms = 25
        run.latency.net_ms += 10
        run.latency.ttft_ms = run.latency.queue_ms + 20
        return run
    run.text = "【不足】不能编。"
    return run


def all_large(question: str) -> Run:
    """对照：每问都上大模型，但不故意重试。用来单独看分级，不和 fat 的重试搅在一起。"""
    intent = route(question)
    text = {"leave": LEAVE, "order": ORDER88, "cafe": CAFE, "other": "【不足】不能编。"}[intent]
    run = Run(text, "large")
    add_llm(run, "large", question, text)
    if intent != "cafe":
        add_retrieve(run, 1)
        run.tools = ["search_policy"] if intent == "leave" else ["get_order"]
    run.latency = Latency(queue_ms=40, model_ms=90, tool_ms=20 if run.tools else 0, net_ms=12, ttft_ms=40 + 90)
    return run


def reset_cache() -> None:
    _cache.clear()


def batch_leave(questions: list[str]) -> Run:
    """三条事假合成一次小模型，而不是串行三次。"""
    joined = "\n".join(questions)
    run = Run(LEAVE, "small")
    add_llm(run, "small", joined, LEAVE)
    add_retrieve(run, 1)
    run.tools = ["search_policy"]
    run.latency = Latency(queue_ms=20, model_ms=18, tool_ms=12, net_ms=8, ttft_ms=28)
    return run
