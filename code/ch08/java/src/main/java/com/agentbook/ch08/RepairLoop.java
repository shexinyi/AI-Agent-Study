package com.agentbook.ch08;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RepairLoop {
    public static void main(String[] args) {
        LeaveExtract result = runRepair(3);
        System.out.println("\n最终对象: " + result);
        System.out.println("slots.days = " + result.slots().days());
    }

    static LeaveExtract runRepair(int maxAttempts) {
        int calls = 0;
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "只输出 LeaveExtract JSON。天数必须等于日期区间。"));
        messages.add(Map.of("role", "user", "content", "我是林秋，下周一到周三请假。"));
        String lastError = "";
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            calls++;
            String raw = calls == 1 ? MessyModel.complete("fenced_bad") : MessyModel.complete("valid");
            System.out.println("\n第 " + attempt + " 次模型输出:\n" + raw.substring(0, Math.min(180, raw.length())) + "...");
            try {
                LeaveExtract parsed = ParseUtil.parse(raw);
                System.out.println("校验通过，调用次数=" + calls);
                return parsed;
            } catch (Exception e) {
                lastError = e.getMessage();
                System.out.println("校验失败: " + e.getClass().getSimpleName());
                messages.add(Map.of("role", "assistant", "content", raw));
                messages.add(Map.of("role", "user", "content", "校验失败，请只返回纠正后的 JSON。错误：" + lastError));
            }
        }
        throw new IllegalStateException("修复循环耗尽: " + lastError);
    }
}
