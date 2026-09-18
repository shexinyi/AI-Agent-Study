package com.agentbook.ch02;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第 2 章示例 1：单轮对话只会说话，不会改系统。
 *
 * mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.agentbook.ch02.SingleTurnTrap"
 */
public class SingleTurnTrap {
    private static final Map<String, Object> HR_DB = new HashMap<>();

    static {
        HR_DB.put("name", "林秋");
        HR_DB.put("leave_balance", 5);
        HR_DB.put("tickets", new ArrayList<String>());
    }

    public static void main(String[] args) {
        String userText = "我是林秋，下周一到周三请假去上海办事，帮我走流程。";
        System.out.println("用户: " + userText + "\n");
        System.out.println("Chatbot: " + chatbotSingleTurn(userText) + "\n");
        System.out.println("HR 系统真实状态: " + hrSnapshot());
        System.out.println("结论: 文本说办好了，数据库仍是空的。这叫幻觉行动，不叫办理成功。");
    }

    static String chatbotSingleTurn(String userText) {
        return "好的，已帮你提交 2026-09-14 至 2026-09-16 的事假，单号 L-2026-0914，并通知了你的经理。";
    }

    static String hrSnapshot() {
        @SuppressWarnings("unchecked")
        List<String> tickets = (List<String>) HR_DB.get("tickets");
        return HR_DB.get("name") + " 剩余年假 " + HR_DB.get("leave_balance") + " 天，工单 " + tickets;
    }
}
