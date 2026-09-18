package com.agentbook.ch19;

import java.util.List;

public class GroupRoles {
    public static void main(String[] args) {
        MiniChat.GroupChat chat = new MiniChat.GroupChat(
                List.of(Kit.policy(), Kit.clerk(), Kit.proxy()),
                (msgs, agents) -> {
                    MiniChat.Message last = msgs.get(msgs.size() - 1);
                    String q = MiniChat.lastUser(msgs);
                    if ("林秋".equals(last.name)) {
                        if (q.contains("事假") || q.contains("请假") || q.contains("年假") || q.contains("食堂")) {
                            return Kit.byName(agents, "政策岗");
                        }
                        return Kit.byName(agents, "订单岗");
                    }
                    if (last.content.startsWith("请用户代理") || last.content.startsWith("TOOL:")) {
                        return Kit.byName(agents, "用户代理");
                    }
                    if ("用户代理".equals(last.name)) {
                        return Kit.byName(agents, "订单岗");
                    }
                    return null;
                },
                8);
        System.out.println("群聊不是大家抢麦。发言人选择是产品规则。\n");
        for (String q : List.of("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单")) {
            System.out.println("--------");
            chat.run(q);
            System.out.println();
        }
        System.out.println("读法：政策岗不碰单号，订单岗不编库存，用户代理只跑工具。");
    }
}
