package com.agentbook.ch24;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 蜂群教具。交接只换人，不改写用户原话。总线在进程里。 */
public final class MiniSwarm {

    public static final class Reply {
        public final String text;
        public final String handoff;

        public Reply(String text) {
            this(text, "");
        }

        public Reply(String text, String handoff) {
            this.text = text;
            this.handoff = handoff == null ? "" : handoff;
        }
    }

    public static final class Message {
        public final String text;
        public final String sender;
        public final String to;
        public final String topic;

        public Message(String text, String sender, String to, String topic) {
            this.text = text;
            this.sender = sender;
            this.to = to;
            this.topic = topic;
        }
    }

    public static final class Agent {
        public final String name;
        public final List<String> topics;
        public final Function<String, Reply> fn;

        public Agent(String name, List<String> topics, Function<String, Reply> fn) {
            this.name = name;
            this.topics = topics;
            this.fn = fn;
        }
    }

    public static final class Bus {
        public final Map<String, List<Message>> inbox = new LinkedHashMap<>();
        public final Map<String, List<String>> subs = new LinkedHashMap<>();
        public final List<String> log = new ArrayList<>();

        public void register(String name, List<String> topics) {
            inbox.computeIfAbsent(name, k -> new ArrayList<>());
            for (String topic : topics) {
                subs.computeIfAbsent(topic, k -> new ArrayList<>()).add(name);
            }
        }

        public void sendP2p(String to, String text, String sender) {
            inbox.computeIfAbsent(to, k -> new ArrayList<>())
                    .add(new Message(text, sender, to, ""));
            log.add("p2p " + sender + " → " + to + ": " + text);
        }

        public void publish(String topic, String text, String sender) {
            List<String> targets = new ArrayList<>(subs.getOrDefault(topic, List.of()));
            log.add("pub " + sender + " → #" + topic + " ["
                    + (targets.isEmpty() ? "无人订阅" : String.join(", ", targets)) + "]: " + text);
            for (String name : targets) {
                inbox.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(new Message(text, sender, "", topic));
            }
        }

        public List<Message> drain(String name) {
            List<Message> items = new ArrayList<>(inbox.getOrDefault(name, List.of()));
            inbox.put(name, new ArrayList<>());
            return items;
        }
    }

    public static final class Swarm {
        public final Map<String, Agent> agents = new LinkedHashMap<>();
        public final Map<String, List<String>> allow;
        public final int maxHops;
        public final List<String> hops = new ArrayList<>();
        public final List<String> log = new ArrayList<>();

        public Swarm(List<Agent> list, Map<String, List<String>> allow, int maxHops) {
            for (Agent agent : list) {
                agents.put(agent.name, agent);
            }
            this.allow = allow;
            this.maxHops = maxHops;
        }

        public String run(String start, String user) {
            hops.clear();
            log.clear();
            String current = start;
            for (int hop = 0; hop < maxHops; hop++) {
                Agent agent = agents.get(current);
                if (agent == null) {
                    return "【路由失败】没有名叫 " + current + " 的人。";
                }
                hops.add(current);
                Reply reply = agent.fn.apply(user);
                log.add("hop" + hop + " " + current + ": handoff="
                        + (reply.handoff.isEmpty() ? "-" : reply.handoff) + " | " + reply.text);
                if (reply.handoff.isEmpty()) {
                    return reply.text;
                }
                List<String> allowed = allow.getOrDefault(current, List.of());
                if (!allowed.contains(reply.handoff)) {
                    return "【非法交接】" + current + " 不能把话筒给 " + reply.handoff + "。白名单=" + allowed;
                }
                if (reply.handoff.equals(current)) {
                    return "【路由失败】" + current + " 把话筒交给自己。";
                }
                current = reply.handoff;
            }
            return "【超过 max_hops=" + maxHops + "】轨迹=" + String.join(" → ", hops);
        }
    }

    public static final class BusSwarm {
        public final Map<String, Agent> agents = new LinkedHashMap<>();
        public final Bus bus = new Bus();

        public void add(Agent agent) {
            agents.put(agent.name, agent);
            bus.register(agent.name, agent.topics);
        }

        public List<String> deliver() {
            List<String> answers = new ArrayList<>();
            for (Agent agent : agents.values()) {
                for (Message msg : bus.drain(agent.name)) {
                    Reply reply = agent.fn.apply(msg.text);
                    answers.add(agent.name + ": " + reply.text);
                }
            }
            return answers;
        }
    }
}
