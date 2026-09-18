package com.agentbook.ch21;

import java.util.List;
import java.util.Map;

public class HostedVsLocal {
    public static void main(String[] args) {
        HostedRuntime api = new HostedRuntime();
        HostedRuntime.Assistant asst = api.createAssistant(List.of("get_order"));
        System.out.printf("%-20s %-16s %s%n", "问法", "托管status", "自建拥有状态");
        for (String q : List.of("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单", "年假能卖吗")) {
            HostedRuntime.Thread th = api.createThread();
            api.addMessage(th.id, "user", q);
            HostedRuntime.Run run = api.createRun(th.id, asst.id);
            if ("requires_action".equals(run.status)) {
                run = api.submitToolOutputs(run.id, LocalRuntime.getOrder(run.requiredAction.get("order_id")));
            }
            String hosted = api.threads.get(th.id).messages.get(api.threads.get(th.id).messages.size() - 1).content;
            Map<String, String> local = LocalRuntime.answer(q);
            System.out.printf("%-20s %-16s 答案一致=%s%n", q, run.status, hosted.equals(local.get("answer")));
            System.out.println("  托管最后一句: " + hosted);
            System.out.println("  自建 session : " + local);
        }
        System.out.println("\n对照：托管省样板；自建在日落后天仍能答。");
    }
}
