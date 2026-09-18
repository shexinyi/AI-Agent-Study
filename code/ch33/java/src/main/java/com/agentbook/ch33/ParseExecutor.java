package com.agentbook.ch33;

public final class ParseExecutor {
    static final String RAW_GET = """
            {"choices":[{"message":{"tool_calls":[
              {"id":"call_1","type":"function","function":{"name":"get_order","arguments":"{\\"order_id\\":\\"ORDER-88\\"}"}}
            ]}}]}
            """;

    static final String RAW_REFUND = """
            {"choices":[{"message":{"tool_calls":[
              {"id":"call_2","type":"function","function":{"name":"refund_order","arguments":{"order_id":"ORDER-88"}}}
            ]}}]}
            """;

    static final String RAW_BAD = """
            {"choices":[{"message":{"tool_calls":[
              {"id":"call_3","type":"function","function":{"name":"get_order","arguments":"{not-json"}}
            ]}}]}
            """;

    static final String RAW_UNKNOWN = """
            {"message":{"tool_calls":[{"id":"call_4","function":{"name":"drop_table","arguments":"{}"}}]}}
            """;

    public static void main(String[] args) {
        System.out.println("白板：模型吐出 JSON，你写出解析和执行。模型输出是意图。\n");
        Kit.Executor intern = new Kit.Executor("intern");
        Kit.Executor ops = new Kit.Executor("agent_ops");

        Kit.ToolResult get88 = intern.run(Kit.parseToolCalls(RAW_GET).get(0));
        Kit.ToolResult refundI = intern.run(Kit.parseToolCalls(RAW_REFUND).get(0));
        Kit.ToolResult refundO = ops.run(Kit.parseToolCalls(RAW_REFUND).get(0));
        Kit.ToolResult bad = intern.run(Kit.parseToolCalls(RAW_BAD).get(0));
        Kit.ToolResult unknown = intern.run(Kit.parseToolCalls(RAW_UNKNOWN).get(0));
        Kit.ToolCall empty = Kit.parseToolCalls("not-json").get(0);

        System.out.println("get ORDER-88 → ok=" + get88.ok + " " + get88.data);
        System.out.println("intern 退款 → " + refundI.error);
        System.out.println("ops 退款 → " + refundO.error + "（仍须 HITL，不执行）");
        System.out.println("坏 JSON → " + bad.error);
        System.out.println("未知工具 → " + unknown.error);
        System.out.println("整包坏掉 → " + empty.error);
        System.out.println();

        System.out.println("=== 行为面三句 ===");
        Kit.BEHAVIOR.forEach((k, v) -> System.out.println("[" + v[0] + "] " + v[1]));
        System.out.println();

        Object orderId = Kit.parseToolCalls(RAW_REFUND).get(0).arguments.get("order_id");
        Object[][] checks = {
                {"解析字符串 arguments", get88.ok && Boolean.FALSE.equals(get88.data.get("shipped"))},
                {"解析对象 arguments", "ORDER-88".equals(orderId)},
                {"实习生拒绝退款", "PERMISSION_DENIED".equals(refundI.error)},
                {"ops 也不自动执行", "HITL_REQUIRED".equals(refundO.error)},
                {"坏参数不崩溃", "BAD_ARGUMENTS".equals(bad.error)},
                {"未知工具拒绝", "UNKNOWN_TOOL".equals(unknown.error)},
                {"整包 PARSE_ERROR", "PARSE_ERROR".equals(empty.error)},
                {"行为面含 HITL 与 MODEL_LIE",
                        Kit.BEHAVIOR.get("incident")[1].contains("HITL")
                                && Kit.BEHAVIOR.get("learn")[1].contains("MODEL_LIE")},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：代码题先写拒绝分支，再写 get_order 成功分支。");
    }
}
