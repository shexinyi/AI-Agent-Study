"""用关键词给工具打分，模拟「模型只靠 description / 使用时机 点菜」。"""

from __future__ import annotations

from toolkit import ToolCall, ToolSpec


def pick(user_text: str, specs: dict[str, ToolSpec], top_k: int = 2) -> list[tuple[str, int]]:
    scored: list[tuple[str, int]] = []
    for spec in specs.values():
        score = sum(3 for word in spec.keywords if word in user_text)
        scored.append((spec.name, score))
    scored.sort(key=lambda item: (-item[1], item[0]))
    return [(name, score) for name, score in scored if score > 0][:top_k]


def to_calls(user_text: str, specs: dict[str, ToolSpec]) -> list[ToolCall]:
    calls: list[ToolCall] = []
    for index, (name, _score) in enumerate(pick(user_text, specs), start=1):
        args: dict = {}
        properties = specs[name].parameters.get("properties", {})
        if "order_id" in properties:
            if "99" in user_text:
                args["order_id"] = "ORDER-99"
            else:
                args["order_id"] = "ORDER-88"
        if "topic" in properties:
            args["topic"] = "退款"
        calls.append(
            ToolCall(
                id=f"call_{index}",
                name=name,
                arguments=args,
                idempotency_key=f"user-demo-{name}-{args.get('order_id', args.get('topic', 'x'))}",
            )
        )
    return calls
