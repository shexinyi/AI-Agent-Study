"""示例 3：白板题——解析 tool call、写执行器；附行为面 STAR。"""

from kit import BEHAVIOR, Executor, parse_tool_calls

RAW_GET = """
{"choices":[{"message":{"tool_calls":[
  {"id":"call_1","type":"function","function":{"name":"get_order","arguments":"{\\"order_id\\":\\"ORDER-88\\"}"}}
]}}]}
"""

RAW_REFUND = """
{"choices":[{"message":{"tool_calls":[
  {"id":"call_2","type":"function","function":{"name":"refund_order","arguments":{"order_id":"ORDER-88"}}}
]}}]}
"""

RAW_BAD = """
{"choices":[{"message":{"tool_calls":[
  {"id":"call_3","type":"function","function":{"name":"get_order","arguments":"{not-json"}}
]}}]}
"""

RAW_UNKNOWN = """
{"message":{"tool_calls":[{"id":"call_4","function":{"name":"drop_table","arguments":"{}"}}]}}
"""


if __name__ == "__main__":
    print("白板：模型吐出 JSON，你写出解析和执行。模型输出是意图。\n")
    intern = Executor("intern")
    ops = Executor("agent_ops")

    get88 = intern.run(parse_tool_calls(RAW_GET)[0])
    refund_i = intern.run(parse_tool_calls(RAW_REFUND)[0])
    refund_o = ops.run(parse_tool_calls(RAW_REFUND)[0])
    bad = intern.run(parse_tool_calls(RAW_BAD)[0])
    unknown = intern.run(parse_tool_calls(RAW_UNKNOWN)[0])
    empty = parse_tool_calls("not-json")[0]

    print(f"get ORDER-88 → ok={get88.ok} {get88.data}")
    print(f"intern 退款 → {refund_i.error}")
    print(f"ops 退款 → {refund_o.error}（仍须 HITL，不执行）")
    print(f"坏 JSON → {bad.error}")
    print(f"未知工具 → {unknown.error}")
    print(f"整包坏掉 → {empty.error}")
    print()

    print("=== 行为面三句 ===")
    for key, (title, star) in BEHAVIOR.items():
        print(f"[{title}] {star}")
    print()

    checks = [
        ("解析字符串 arguments", get88.ok and get88.data.get("shipped") is False),
        ("解析对象 arguments", parse_tool_calls(RAW_REFUND)[0].arguments.get("order_id") == "ORDER-88"),
        ("实习生拒绝退款", refund_i.error == "PERMISSION_DENIED"),
        ("ops 也不自动执行", refund_o.error == "HITL_REQUIRED"),
        ("坏参数不崩溃", bad.error == "BAD_ARGUMENTS"),
        ("未知工具拒绝", unknown.error == "UNKNOWN_TOOL"),
        ("整包 PARSE_ERROR", empty.error == "PARSE_ERROR"),
        ("行为面含 HITL 与 MODEL_LIE", "HITL" in BEHAVIOR["incident"][1] and "MODEL_LIE" in BEHAVIOR["learn"][1]),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：代码题先写拒绝分支，再写 get_order 成功分支。")
