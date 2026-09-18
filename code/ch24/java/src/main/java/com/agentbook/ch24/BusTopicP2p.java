package com.agentbook.ch24;

import java.util.List;

public final class BusTopicP2p {
    static final String ORDER_Q = "查一下 ORDER-88 能不能退。";

    public static void main(String[] args) {
        System.out.println("点对点必须知道对方名字。主题按订阅扇出。广播 #all 是污染源。");
        System.out.println("教学总线在进程里，不是 Kafka。");
        System.out.println();

        List<String> p2p = run("p2p");
        List<String> topic = run("topic");
        List<String> broadcast = run("broadcast");

        Kit.check("点对点只有订单岗", p2p.size() == 1 && Kit.expectOk(String.join("\n", p2p), "has_88"));
        Kit.check("主题只有订单岗", topic.size() == 1 && Kit.expectOk(String.join("\n", topic), "has_88"));
        Kit.check("广播食堂也开口", broadcast.stream().anyMatch(x -> x.startsWith("食堂专家:")));
        Kit.check("广播编了红烧肉", Kit.expectOk(String.join("\n", broadcast), "has_pork"));
        Kit.check("点对点不编菜单", Kit.expectOk(String.join("\n", p2p), "no_pork"));
        System.out.println("读法：消息发给谁，比「我们上了多智能体」更重要。默认点对点或窄主题。");
    }

    private static List<String> run(String mode) {
        MiniSwarm.BusSwarm world = new MiniSwarm.BusSwarm();
        boolean hallucinate = "broadcast".equals(mode);
        world.add(Kit.leaveExpert(false));
        world.add(Kit.orderExpert(false, false));
        world.add(Kit.cafeExpert(hallucinate));
        if ("p2p".equals(mode)) {
            world.bus.sendP2p("订单专家", ORDER_Q, "前台");
        } else if ("topic".equals(mode)) {
            world.bus.publish("orders", ORDER_Q, "前台");
        } else {
            world.bus.publish("all", ORDER_Q, "前台");
        }
        List<String> answers = world.deliver();
        System.out.println("=== " + mode + " ===");
        for (String line : world.bus.log) {
            System.out.println("  总线 " + line);
        }
        if (answers.isEmpty()) {
            System.out.println("  无人回");
        }
        for (String line : answers) {
            System.out.println("  " + line);
        }
        System.out.println();
        return answers;
    }
}
