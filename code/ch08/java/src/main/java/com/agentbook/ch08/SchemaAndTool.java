package com.agentbook.ch08;

import com.fasterxml.jackson.databind.JsonNode;

public class SchemaAndTool {
    public static void main(String[] args) throws Exception {
        LeaveExtract chat = ParseUtil.parse(MessyModel.complete("valid"));
        JsonNode toolRaw = ParseUtil.MAPPER.readTree(MessyModel.complete("tool_args"));
        LeaveExtract tool = ParseUtil.MAPPER.treeToValue(toolRaw.get("arguments"), LeaveExtract.class);
        tool.validate();
        System.out.println("聊天内容解析: " + chat);
        System.out.println("工具参数解析: " + tool);
        System.out.println("两份 slots.days 相等: " + chat.slots().days() + " == " + tool.slots().days());
        System.out.println("\n差异不在 Schema，在意图：结构是答案形状；工具参数后面往往还要执行副作用。");
    }
}
