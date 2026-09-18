package com.agentbook.ch21;

import java.util.List;

public class ObjectModel {
    public static void main(String[] args) {
        HostedRuntime api = new HostedRuntime();
        HostedRuntime.Assistant asst = api.createAssistant(List.of("get_order"));
        System.out.println("Assistant " + asst.id + " tools= " + asst.tools);
        for (String q : List.of("事假上限几天", "ORDER-88 能不能退")) {
            HostedRuntime.Thread th = api.createThread();
            api.addMessage(th.id, "user", q);
            HostedRuntime.Run run = api.createRun(th.id, asst.id);
            System.out.println("\n问: " + q);
            System.out.println("Run " + run.id + " status= " + run.status + " required= " + run.requiredAction);
            if ("requires_action".equals(run.status)) {
                run = api.submitToolOutputs(run.id, LocalRuntime.getOrder(run.requiredAction.get("order_id")));
                System.out.println("submit_tool_outputs → " + run.status);
            }
            System.out.println("Thread 消息:");
            for (HostedRuntime.Message m : api.threads.get(th.id).messages) {
                System.out.println("  " + m.role + ": " + m.content);
            }
        }
        System.out.println("\n读法：requires_action 仍是意图，不是事实。");
    }
}
