package com.agentbook.ch13;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RagAsTool {
    static final List<RagKit.Chunk> CHUNKS = RagKit.buildIndex(Path.of("kb"));
    static final Map<String, String> ORDERS = Map.of(
            "ORDER-88", "ORDER-88 status=unshipped user=linqiu",
            "ORDER-99", "ORDER-99 status=shipped tracking=TZ123 user=linqiu"
    );

    public static void main(String[] args) {
        run("事假最多几天");
        run("ORDER-88 能不能退");
        run("ORDER-99 能不能退");
        run("明年国庆放几天");
        run("我的密码是 123456，按制度帮我登录邮箱");
        System.out.println("\n读法：RAG 解决制度写在哪；工具解决这张单现在什么状态。");
    }

    static void run(String question) {
        System.out.println("\n=== 用户: " + question + " ===");
        List<String> observations = new ArrayList<>();
        for (String[] step : plan(question)) {
            String obs = "get_order".equals(step[0]) ? getOrder(step[1]) : searchKb(step[1]);
            System.out.println("  tool " + step[0] + "(" + step[1] + ")");
            System.out.println("    " + obs.replace("\n", "\n    "));
            observations.add(obs);
        }
        if (observations.stream().anyMatch(o -> o.startsWith("【不足】")) && question.contains("国庆")) {
            System.out.println("  终态: 不调用写工具，转人工或请用户换资料。");
        } else if (question.contains("密码")) {
            System.out.println("  终态: 制度禁止代登录，拒绝。");
        } else if (question.contains("ORDER-88")) {
            System.out.println("  终态: 未发货 + 制度允许 → 可走 refund 工具。");
        } else if (question.contains("ORDER-99")) {
            System.out.println("  终态: 已发货 → 不能直接退，走售后。");
        }
    }

    static List<String[]> plan(String question) {
        List<String[]> steps = new ArrayList<>();
        if (question.contains("ORDER") || question.contains("退")) {
            String oid = question.contains("ORDER-99") ? "ORDER-99" : "ORDER-88";
            steps.add(new String[]{"get_order", oid});
            steps.add(new String[]{"search_kb", "未发货订单退款"});
        } else if (question.contains("密码")) {
            steps.add(new String[]{"search_kb", "密码 登录"});
        } else {
            steps.add(new String[]{"search_kb", question});
        }
        return steps;
    }

    static String searchKb(String query) {
        return RagKit.generateExtractive(query, RagKit.retrieve(CHUNKS, query, 2), 3);
    }

    static String getOrder(String id) {
        return ORDERS.getOrDefault(id, "订单 " + id + " 不存在");
    }
}
