package com.agentbook.ch21;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HostedRuntime {
    public static final String SUNSET = "2026-08-26";

    public static final class Assistant {
        public final String id;
        public final List<String> tools;

        Assistant(String id, List<String> tools) {
            this.id = id;
            this.tools = tools;
        }
    }

    public static final class Message {
        public final String role;
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static final class Run {
        public final String id;
        public final String threadId;
        public final String assistantId;
        public String status;
        public Map<String, String> requiredAction;

        Run(String id, String threadId, String assistantId) {
            this.id = id;
            this.threadId = threadId;
            this.assistantId = assistantId;
            this.status = "in_progress";
        }
    }

    public static final class Thread {
        public final String id;
        public final List<Message> messages = new ArrayList<>();

        Thread(String id) {
            this.id = id;
        }
    }

    public boolean cloudAlive = true;
    public final Map<String, Assistant> assistants = new LinkedHashMap<>();
    public final Map<String, Thread> threads = new LinkedHashMap<>();
    public final Map<String, Run> runs = new LinkedHashMap<>();

    private void live() {
        if (!cloudAlive) {
            throw new IllegalStateException("Assistants API 已于 " + SUNSET + " 日落。thread_id 在供应商侧不再可取。");
        }
    }

    public Assistant createAssistant(List<String> tools) {
        live();
        Assistant a = new Assistant("asst_" + UUID.randomUUID().toString().substring(0, 8), tools);
        assistants.put(a.id, a);
        return a;
    }

    public Thread createThread() {
        live();
        Thread t = new Thread("thread_" + UUID.randomUUID().toString().substring(0, 8));
        threads.put(t.id, t);
        return t;
    }

    public void addMessage(String threadId, String role, String content) {
        live();
        threads.get(threadId).messages.add(new Message(role, content));
    }

    public Run createRun(String threadId, String assistantId) {
        live();
        Run run = new Run("run_" + UUID.randomUUID().toString().substring(0, 8), threadId, assistantId);
        runs.put(run.id, run);
        step(run);
        return run;
    }

    public Run submitToolOutputs(String runId, String output) {
        live();
        Run run = runs.get(runId);
        if (!"requires_action".equals(run.status)) {
            throw new IllegalStateException("当前 Run 不在 requires_action");
        }
        threads.get(run.threadId).messages.add(new Message("tool", output));
        run.requiredAction = null;
        run.status = "in_progress";
        step(run);
        return run;
    }

    public List<Message> exportThread(String threadId) {
        live();
        return List.copyOf(threads.get(threadId).messages);
    }

    public void sunset() {
        cloudAlive = false;
        assistants.clear();
        threads.clear();
        runs.clear();
    }

    private void step(Run run) {
        Assistant asst = assistants.get(run.assistantId);
        Thread thread = threads.get(run.threadId);
        String question = "";
        for (int i = thread.messages.size() - 1; i >= 0; i--) {
            if ("user".equals(thread.messages.get(i).role)) {
                question = thread.messages.get(i).content;
                break;
            }
        }
        List<String> toolsDone = thread.messages.stream().filter(m -> "tool".equals(m.role)).map(m -> m.content).toList();
        if (question.contains("食堂") || question.contains("年假")) {
            thread.messages.add(new Message("assistant", "【不足】资料不够。"));
            run.status = "completed";
            return;
        }
        if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
            thread.messages.add(new Message("assistant", "根据制度，事假每年累计不超过 10 个工作日。"));
            run.status = "completed";
            return;
        }
        boolean needOrder = question.toUpperCase().contains("ORDER") || question.toUpperCase().contains("TZ") || question.contains("退");
        if (asst.tools.contains("get_order") && toolsDone.isEmpty() && needOrder) {
            String oid = (question.toUpperCase().contains("TZ123") || question.toUpperCase().contains("ORDER-99"))
                    ? "ORDER-99" : "ORDER-88";
            run.status = "requires_action";
            run.requiredAction = Map.of("tool", "get_order", "order_id", oid);
            return;
        }
        String fact = toolsDone.isEmpty() ? "" : toolsDone.get(toolsDone.size() - 1);
        if (fact.contains("未发货")) {
            thread.messages.add(new Message("assistant", "根据工具，ORDER-88 未发货可退。写工具前须人工确认。"));
        } else if (fact.contains("已发货") || fact.contains("TZ123")) {
            thread.messages.add(new Message("assistant", "根据工具，TZ123 对应 ORDER-99，已发货须走售后。"));
        } else {
            thread.messages.add(new Message("assistant", "【不足】工具没有可用事实。"));
        }
        run.status = "completed";
    }
}
