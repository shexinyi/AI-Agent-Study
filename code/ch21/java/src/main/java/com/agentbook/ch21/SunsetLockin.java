package com.agentbook.ch21;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SunsetLockin {
    public static void main(String[] args) throws IOException {
        HostedRuntime api = new HostedRuntime();
        HostedRuntime.Assistant asst = api.createAssistant(List.of("get_order"));
        HostedRuntime.Thread th = api.createThread();
        api.addMessage(th.id, "user", "ORDER-88 能不能退");
        HostedRuntime.Run run = api.createRun(th.id, asst.id);
        if ("requires_action".equals(run.status)) {
            api.submitToolOutputs(run.id, LocalRuntime.getOrder(run.requiredAction.get("order_id")));
        }
        System.out.println("云端 Thread " + th.id);
        System.out.println("偷懒的人只把这个 id 写进数据库。");
        StringBuilder json = new StringBuilder("[\n");
        List<HostedRuntime.Message> snap = api.exportThread(th.id);
        for (int i = 0; i < snap.size(); i++) {
            HostedRuntime.Message m = snap.get(i);
            json.append("  {\"role\":\"").append(m.role).append("\",\"content\":\"").append(m.content).append("\"}");
            json.append(i < snap.size() - 1 ? ",\n" : "\n");
        }
        json.append("]\n");
        Path export = Path.of("exported_thread.json");
        Files.writeString(export, json.toString(), StandardCharsets.UTF_8);
        System.out.println("勤快的人导出消息到 " + export);

        System.out.println("\n--- 模拟 " + HostedRuntime.SUNSET + " 日落 ---");
        api.sunset();
        System.out.println("云端字典已空。用旧 thread_id 取值：404");
        try {
            api.addMessage(th.id, "user", "还在吗");
        } catch (IllegalStateException exc) {
            System.out.println("再调 API: " + exc.getMessage());
        }
        System.out.println("本地导出还在:\n" + Files.readString(export, StandardCharsets.UTF_8));
        System.out.println("读法：没拷走的消息，不是你的。");
    }
}
