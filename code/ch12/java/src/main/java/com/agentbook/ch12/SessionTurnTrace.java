package com.agentbook.ch12;

public class SessionTurnTrace {
    public static void main(String[] args) {
        SessionKit.SessionService svc = new SessionKit.SessionService(new SessionKit.MemoryStore());
        SessionKit.Session sess = svc.start("linqiu", "sess-leave-1");
        System.out.println("新会话: " + sess.sessionId + " phase=" + sess.phase);
        for (String line : new String[]{"我是林秋", "下周一到周三请假去上海", "事假，通知经理就行"}) {
            sess = svc.handle("linqiu", sess.sessionId, line);
            System.out.println("\n=== 用户: " + line + " ===");
            System.out.println(sess.packForPrompt());
            System.out.println("缺失: " + (sess.missing().isEmpty() ? "无" : sess.missing()));
        }
        System.out.println("\n轨迹:");
        sess.trace.forEach(ev -> System.out.println("  [" + ev.kind + "] " + ev.text));
        System.out.println("\n读法：轮次是原文，槽位是准星，轨迹是审计。");
    }
}
