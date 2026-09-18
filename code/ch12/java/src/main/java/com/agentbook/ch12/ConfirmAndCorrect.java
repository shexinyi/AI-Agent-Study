package com.agentbook.ch12;

public class ConfirmAndCorrect {
    public static void main(String[] args) {
        SessionKit.SessionService svc = new SessionKit.SessionService(new SessionKit.MemoryStore());
        String sid = "sess-leave-2";
        svc.start("linqiu", sid);
        svc.handle("linqiu", sid, "我是林秋，下周一到周三请假，事假");
        show(svc.handle("linqiu", sid, "确认"), "确认提交");
        show(svc.handle("linqiu", sid, "其实只要两天"), "提出修改（旧值还在，新值是 proposed）");
        show(svc.handle("linqiu", sid, "还是原来的"), "拒绝修改");
        show(svc.handle("linqiu", sid, "改成周四结束"), "再次提出改结束日");
        show(svc.handle("linqiu", sid, "确认"), "接受修改");
        System.out.println("\n读法：已确认的天数不是聊天记录里最后一句说了算，要过确认策略。");
    }

    private static void show(SessionKit.Session sess, String title) {
        System.out.println("\n=== " + title + "  phase=" + sess.phase + " ===");
        System.out.println("  " + String.join(", ", sess.slots.values().stream().map(SessionKit.Slot::show).toList()));
    }
}
