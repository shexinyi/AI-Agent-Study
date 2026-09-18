package com.agentbook.ch19;

import java.util.List;

public class TwoAgentChat {
    public static void main(String[] args) {
        MiniChat.GroupChat chat = new MiniChat.GroupChat(
                List.of(Kit.policy()),
                (msgs, agents) -> msgs.stream().noneMatch(m -> "政策岗".equals(m.name)) ? agents.get(0) : null,
                4);
        System.out.println("两人对打：用户一问，政策岗一答就停。这是 AutoGen 最薄的形状。\n");
        for (String q : List.of("事假上限几天", "年假能卖吗")) {
            System.out.println("--------");
            chat.run(q);
            System.out.println();
        }
        System.out.println("读法：多智能体首先是一场对话，不是一条链，也不是一张图。");
    }
}
