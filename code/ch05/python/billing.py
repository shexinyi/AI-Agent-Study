"""教学用 Token 估算与费用试算。真实账单以供应商 usage 字段和控制台为准。"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Price:
    name: str
    input_per_million: float
    output_per_million: float
    unit: str = "USD"


# 示意价，不是报价单。假设读取日期：2026-09。上线前请改成你账号里的数字。
TEACHING_PRICES = {
    "gpt-4o-mini": Price("gpt-4o-mini", 0.15, 0.60, "USD"),
    "deepseek-chat": Price("deepseek-chat", 0.14, 0.28, "USD"),
    "qwen-plus": Price("qwen-plus", 0.11, 0.28, "USD"),
    "claude-sonnet-4-5": Price("claude-sonnet-4-5", 3.00, 15.00, "USD"),
}


def estimate_tokens(text: str) -> int:
    """
    极粗估算：ASCII 按 ~4 字符/Token，其他（含中文）按 ~1.6 字符/Token。
    不能替代 tokenizer，更不能替代响应里的 usage。
    """
    if not text:
        return 0
    ascii_chars = sum(1 for ch in text if ord(ch) < 128)
    other_chars = len(text) - ascii_chars
    return max(1, int(ascii_chars / 4 + other_chars / 1.6))


def cost_usd(model: str, input_tokens: int, output_tokens: int) -> float:
    price = TEACHING_PRICES.get(model) or Price(model, 1.00, 3.00, "USD")
    return (input_tokens * price.input_per_million + output_tokens * price.output_per_million) / 1_000_000


def explain(model: str, input_tokens: int, output_tokens: int) -> str:
    price = TEACHING_PRICES.get(model)
    usd = cost_usd(model, input_tokens, output_tokens)
    tag = "教学示意价" if price else "未收录模型，按 1/3 USD per 1M 示意"
    return (
        f"模型={model} 输入={input_tokens} 输出={output_tokens} "
        f"合计={input_tokens + output_tokens} Token，约 {usd:.6f} USD（{tag}）"
    )
