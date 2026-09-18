"""示例 1：一次 ORDER-88 问询的 Trace 树和 Token 汇总。"""

from kit import healthy_refund_trace

if __name__ == "__main__":
    print("Trace = 一次请求。Span = 树上的一步。Tool Call 是 kind=tool 的 Span。")
    print("Token 按字符数计，是教具，不是账单。写退款仍停在 HITL，没有 refund_order。\n")
    tr = healthy_refund_trace()
    assert tr.trace is not None
    print(tr.trace.tree())
    inn, out = tr.trace.tokens()
    print(f"\n合计 tokens_in={inn} tokens_out={out} spans={len(tr.trace.spans)}")
    kinds = [s.kind for s in tr.trace.spans]
    checks = [
        ("有 agent 根", kinds.count("agent") == 1),
        ("有 retrieve / tool / llm / hitl", {"retrieve", "tool", "llm", "hitl"} <= set(kinds)),
        ("没有执行退款", all(s.name != "refund_order" for s in tr.trace.spans)),
        ("get_order 成功", any(s.name == "get_order" and s.status == "ok" for s in tr.trace.spans)),
        ("Token 大于 0", inn > 0 and out > 0),
    ]
    print()
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：先画树，再问红的是哪一类 Span。不要对着对话框猜。")
