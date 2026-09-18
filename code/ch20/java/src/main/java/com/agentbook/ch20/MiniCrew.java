package com.agentbook.ch20;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class MiniCrew {
    public static final String SEQUENTIAL = "sequential";
    public static final String HIERARCHICAL = "hierarchical";

    public static final class Agent {
        public final String role;
        public final BiFunction<String, Map<String, Object>, String> work;

        public Agent(String role, BiFunction<String, Map<String, Object>, String> work) {
            this.role = role;
            this.work = work;
        }
    }

    public static final class Task {
        public final String name;
        public final String description;
        public final Agent bound;
        public String output = "";

        public Task(String name, String description, Agent bound) {
            this.name = name;
            this.description = description;
            this.bound = bound;
        }
    }

    public static final class Crew {
        public final List<Agent> agents;
        public final List<Task> tasks;
        public final String process;
        public final Agent manager;
        public final List<String> log = new ArrayList<>();

        public Crew(List<Agent> agents, List<Task> tasks, String process, Agent manager) {
            this.agents = agents;
            this.tasks = tasks;
            this.process = process;
            this.manager = manager;
        }

        public String kickoff(String question) {
            Map<String, String> context = new LinkedHashMap<>();
            log.clear();
            for (Task task : tasks) {
                Agent worker = task.bound;
                if (HIERARCHICAL.equals(process)) {
                    if (manager == null) {
                        throw new IllegalStateException("层级流必须有 manager");
                    }
                    if (worker != null) {
                        log.add("经理尊重预绑定 " + task.name + " → " + worker.role);
                    } else {
                        String pick = manager.work.apply(task.description + "\n问:" + question, Map.of());
                        log.add("经理指派 " + task.name + " → " + pick);
                        worker = agents.stream().filter(a -> a.role.equals(pick)).findFirst().orElse(null);
                        if (worker == null) {
                            task.output = "【不足】经理指派了不存在的角色: " + pick;
                            context.put(task.name, task.output);
                            continue;
                        }
                    }
                } else {
                    if (worker == null) {
                        throw new IllegalStateException("顺序流任务必须预绑定: " + task.name);
                    }
                    log.add("顺序执行 " + task.name + " / " + worker.role);
                }
                Map<String, Object> blob = new LinkedHashMap<>();
                blob.put("question", question);
                blob.put("context", new LinkedHashMap<>(context));
                task.output = worker.work.apply(task.description, blob);
                context.put(task.name, task.output);
            }
            return tasks.get(tasks.size() - 1).output;
        }
    }
}
