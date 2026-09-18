package com.agentbook.appendix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiCheat {
    public static void main(String[] args) throws Exception {
        String text = Files.readString(Root.repo().resolve("docs/附录B-常用模型与接口速查.md"), StandardCharsets.UTF_8);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Assistant", "Prompt");
        map.put("Thread", "Conversation");
        map.put("Run", "Response");
        map.put("Run Step", "Item");
        System.out.println("接口速查（2026-09 教学快照）\n");
        map.forEach((o, n) -> System.out.println("  " + o + " → " + n));
        System.out.println("  Assistants 关停：2026-08-26");
        System.out.println("  新项目：POST /v1/responses");
        System.out.println();
        Object[][] checks = {
                {"写明日落日", text.contains("2026-08-26")},
                {"Responses 是首选", text.contains("Responses") && text.contains("新项目首选")},
                {"四组映射都在", map.values().stream().allMatch(text::contains)},
                {"教学币不对账单", text.contains("不能写进报价单")},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：简历不要写线上还在跑 Assistants。");
    }
}
