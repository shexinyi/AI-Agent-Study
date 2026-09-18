package com.agentbook.ch09;

import java.util.List;

public class HistoryIsNotMemory {
    static final List<String[]> DIALOGUE = List.of(
            new String[]{"user", "在吗"},
            new String[]{"assistant", "在的，我是请假助手。"},
            new String[]{"user", "我是凌秋，下周一到周三请假去上海办事，帮我走流程。"},
            new String[]{"assistant", "收到，3 天事假，需要经理审批。"},
            new String[]{"user", "对，通知经理就行。对了天气好像要下雨。"},
            new String[]{"assistant", "这不影响请假单。窗口期已记下。"},
            new String[]{"user", "嗯嗯"},
            new String[]{"assistant", "还需要改日期吗？"},
            new String[]{"user", "不用。就这样。"},
            new String[]{"assistant", "好，我按已有槽位提交。"}
    );

    public static void main(String[] args) {
        String raw = DIALOGUE.stream().map(t -> "[" + t[0] + "] " + t[1]).reduce((a, b) -> a + "\n" + b).orElse("");
        MemoryKit.WorkingMemory wm = new MemoryKit.WorkingMemory(4);
        DIALOGUE.forEach(t -> wm.add(t[0], t[1]));
        System.out.println("=== 全量聊天记录（常被误当成记忆）===");
        System.out.println(raw);
        System.out.println("\n体积: " + MemoryKit.tokenish(raw) + " 字符");
        System.out.println("\n=== 工作记忆打包（摘要 + 槽位 + 最近窗口）===");
        System.out.println(wm.packForPrompt());
        System.out.println("\n体积: " + MemoryKit.tokenish(wm.packForPrompt()) + " 字符  槽位: " + wm.slots);
        System.out.println("\n读法：下游真正需要的是槽位和摘要，不是每一句「嗯嗯」。");
    }
}
