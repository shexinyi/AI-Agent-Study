package com.agentbook.ch19;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MiniChat {
    public static final class Message {
        public final String name;
        public final String content;

        public Message(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    public static final class Agent {
        public final String name;
        public final Function<List<Message>, String> reply;

        public Agent(String name, Function<List<Message>, String> reply) {
            this.name = name;
            this.reply = reply;
        }
    }

    public static String lastUser(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("林秋".equals(messages.get(i).name) || "user".equalsIgnoreCase(messages.get(i).name)) {
                return messages.get(i).content;
            }
        }
        return messages.isEmpty() ? "" : messages.get(0).content;
    }

    public static boolean isTerminate(String text) {
        return text.contains("TERMINATE") || text.startsWith("【不足】");
    }

    public static final class GroupChat {
        private final List<Agent> agents;
        private final BiFunction<List<Message>, List<Agent>, Agent> select;
        private final int maxRound;
        private final Predicate<String> term;

        public GroupChat(List<Agent> agents, BiFunction<List<Message>, List<Agent>, Agent> select, int maxRound) {
            this(agents, select, maxRound, MiniChat::isTerminate);
        }

        public GroupChat(List<Agent> agents, BiFunction<List<Message>, List<Agent>, Agent> select,
                         int maxRound, Predicate<String> term) {
            this.agents = agents;
            this.select = select;
            this.maxRound = maxRound;
            this.term = term;
        }

        public List<Message> run(String opening) {
            List<Message> history = new ArrayList<>();
            history.add(new Message("林秋", opening));
            System.out.println("林秋: " + opening);
            for (int i = 0; i < maxRound; i++) {
                Agent speaker = select.apply(history, agents);
                if (speaker == null) {
                    System.out.println("（无人接话，停）");
                    break;
                }
                String text = speaker.reply.apply(history);
                history.add(new Message(speaker.name, text));
                System.out.println(speaker.name + ": " + text);
                if (term.test(text)) {
                    return history;
                }
            }
            if (!history.isEmpty() && !term.test(history.get(history.size() - 1).content)) {
                System.out.println("（触及 max_round=" + maxRound + "，强制停）");
            }
            return history;
        }
    }
}
