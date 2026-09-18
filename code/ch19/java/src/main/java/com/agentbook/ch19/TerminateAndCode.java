package com.agentbook.ch19;

import java.util.List;

public class TerminateAndCode {
    public static void main(String[] args) {
        MiniChat.GroupChat chat = new MiniChat.GroupChat(
                List.of(Kit.coder(), Kit.proxy(), Kit.policy()),
                (msgs, agents) -> {
                    MiniChat.Message last = msgs.get(msgs.size() - 1);
                    if ("林秋".equals(last.name)) {
                        return Kit.byName(agents, "代码岗");
                    }
                    if (last.content.contains("```")) {
                        return Kit.byName(agents, "用户代理");
                    }
                    if ("用户代理".equals(last.name)) {
                        return Kit.byName(agents, "政策岗");
                    }
                    return null;
                },
                6);

        System.out.println("安全脚本：计算 2026-09-14 到 16 是几天（含首尾=3）。");
        System.out.println("--------");
        chat.run("从 2026-09-14 请到 16 号，一共几天？对照事假上限。");

        System.out.println("\n危险脚本：模型想跑 os.system。执行器必须拒绝。");
        System.out.println("--------");
        chat.run("帮我跑一段危险代码 pwn");

        System.out.println("\n没有 TERMINATE 的闲聊会撞上 max_round。");
        MiniChat.GroupChat endless = new MiniChat.GroupChat(
                List.of(new MiniChat.Agent("复读", m -> "再聊一句。")),
                (msgs, agents) -> agents.get(0),
                3,
                t -> false);
        System.out.println("--------");
        endless.run("你好");
        System.out.println("\n读法：终止是产品开关。代码执行是攻击面，默认不 exec。");
    }
}
