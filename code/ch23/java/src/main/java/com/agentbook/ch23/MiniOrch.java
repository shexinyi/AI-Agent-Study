package com.agentbook.ch23;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** 主编排教具。拆解是规则；工人串行；黑板在进程里。 */
public final class MiniOrch {
    public static final String ISOLATE = "isolate";
    public static final String FAIL_FAST = "fail_fast";

    public static final class Task {
        public final String id;
        public final String kind;
        public final String query;
        public String status = "pending";
        public String result = "";
        public String error = "";

        public Task(String id, String kind, String query) {
            this.id = id;
            this.kind = kind;
            this.query = query;
        }
    }

    public static final class Blackboard {
        public final Map<String, Map<String, String>> slots = new LinkedHashMap<>();
        public final List<String> log = new ArrayList<>();

        public void write(String taskId, Map<String, String> fields) {
            Map<String, String> slot = slots.computeIfAbsent(taskId, k -> new LinkedHashMap<>());
            slot.putAll(fields);
            log.add(taskId + ": " + fields);
        }
    }

    public static final class Worker {
        public final String name;
        public final String kind;
        public final Function<Task, String> fn;

        public Worker(String name, String kind, Function<Task, String> fn) {
            this.name = name;
            this.kind = kind;
            this.fn = fn;
        }

        public String run(Task task) {
            return fn.apply(task);
        }
    }

    public static final class Orchestrator {
        public final Map<String, Worker> workers = new LinkedHashMap<>();
        public final String policy;
        public Blackboard board = new Blackboard();

        public Orchestrator(List<Worker> list, String policy) {
            for (Worker worker : list) {
                workers.put(worker.kind, worker);
            }
            this.policy = policy;
        }

        public List<Task> decompose(String goal) {
            List<Task> tasks = new ArrayList<>();
            int n = 1;
            if (goal.contains("事假") || goal.contains("请假") || goal.contains("工作日")) {
                tasks.add(new Task("t" + n, "leave", goal));
                n++;
            }
            String upper = goal.toUpperCase(Locale.ROOT);
            if (upper.contains("ORDER") || upper.contains("TZ123") || goal.contains("退")) {
                tasks.add(new Task("t" + n, "order", goal));
                n++;
            }
            if (goal.contains("食堂")) {
                tasks.add(new Task("t" + n, "cafe", goal));
            }
            if (tasks.isEmpty()) {
                tasks.add(new Task("t1", "unknown", goal));
            }
            return tasks;
        }

        public String run(String goal) {
            board = new Blackboard();
            List<Task> tasks = decompose(goal);
            boolean failed = false;
            for (Task task : tasks) {
                if (FAIL_FAST.equals(policy) && failed) {
                    task.status = "skipped";
                    board.write(task.id, Map.of("kind", task.kind, "status", "skipped"));
                    continue;
                }
                Worker worker = workers.get(task.kind);
                if (worker == null) {
                    task.status = "failed";
                    task.error = "没有 " + task.kind + " 工人";
                    board.write(task.id, Map.of("kind", task.kind, "status", "failed", "error", task.error));
                    failed = true;
                    continue;
                }
                try {
                    String text = worker.run(task);
                    task.status = "ok";
                    task.result = text;
                    board.write(task.id, Map.of(
                            "kind", task.kind,
                            "status", "ok",
                            "worker", worker.name,
                            "result", text));
                } catch (RuntimeException exc) {
                    task.status = "failed";
                    task.error = exc.getClass().getSimpleName() + ": " + exc.getMessage();
                    board.write(task.id, Map.of("kind", task.kind, "status", "failed", "error", task.error));
                    failed = true;
                }
            }
            return gather(tasks);
        }

        public String gather(List<Task> tasks) {
            if (FAIL_FAST.equals(policy) && tasks.stream().anyMatch(t -> "failed".equals(t.status))) {
                return "【整单失败】有工人未交卷，按快速失败不返回部分结果。";
            }
            StringBuilder sb = new StringBuilder("林秋工单汇总：");
            for (Task task : tasks) {
                sb.append('\n');
                if ("ok".equals(task.status)) {
                    sb.append("- [").append(task.kind).append('/').append(task.id).append("] ").append(task.result);
                } else if ("failed".equals(task.status)) {
                    sb.append("- [").append(task.kind).append('/').append(task.id).append("] 【失败隔离】").append(task.error);
                } else {
                    sb.append("- [").append(task.kind).append('/').append(task.id).append("] 【跳过】未派工");
                }
            }
            return sb.toString();
        }
    }
}
