package com.agentbook.ch12;

import java.nio.file.Path;

public class StoreAndIsolation {
    public static void main(String[] args) {
        isolation();
        persist();
        version();
    }

    private static void isolation() {
        System.out.println("=== 隔离：同一进程里两个人 ===");
        SessionKit.MemoryStore store = new SessionKit.MemoryStore();
        SessionKit.SessionService svc = new SessionKit.SessionService(store);
        SessionKit.Session leave = svc.start("linqiu", "sess-leave");
        SessionKit.Session refund = svc.start("zhangsan", "sess-refund");
        svc.handle("linqiu", leave.sessionId, "我是林秋，下周一到周三请假");
        svc.handle("zhangsan", refund.sessionId, "ORDER-88 要退款");
        try {
            svc.handle("zhangsan", leave.sessionId, "把林秋的假单改了");
            System.out.println("  误伤：张三改到了林秋的会话（不该发生）");
        } catch (SecurityException e) {
            System.out.println("  张三拿着林秋的 session_id 被拒绝 " + e.getMessage());
        }
        SessionKit.Session again = store.get("linqiu", leave.sessionId);
        System.out.println("  林秋槽位仍在: " + again.slots.get("employee").value + " " + again.slots.get("days").value);
    }

    private static void persist() {
        System.out.println("\n=== 落盘：进程重启 ===");
        Path data = Path.of("data", "sessions.json");
        try {
            java.nio.file.Files.deleteIfExists(data);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        SessionKit.SessionService svc = new SessionKit.SessionService(new SessionKit.FileStore(data));
        svc.start("linqiu", "sess-disk");
        svc.handle("linqiu", "sess-disk", "我是林秋，下周一到周三请假");
        System.out.println("  写入后 phase=" + new SessionKit.FileStore(data).get("linqiu", "sess-disk").phase);
        SessionKit.Session sess2 = new SessionKit.SessionService(new SessionKit.FileStore(data))
                .handle("linqiu", "sess-disk", "确认");
        System.out.println("  新进程确认后 phase=" + sess2.phase + " employee=" + sess2.slots.get("employee").value);
    }

    private static void version() {
        System.out.println("\n=== 乐观锁：两个请求读到同一版本 ===");
        SessionKit.MemoryStore store = new SessionKit.MemoryStore();
        new SessionKit.SessionService(store).start("linqiu", "sess-race");
        SessionKit.Session a = store.get("linqiu", "sess-race");
        SessionKit.Session b = store.get("linqiu", "sess-race");
        System.out.println("  两个快照都是 v=" + a.version);
        store.save(a, a.version);
        System.out.println("  请求 A 写入成功，现在库版本=" + store.get("linqiu", "sess-race").version);
        try {
            store.save(b, b.version);
            System.out.println("  请求 B 也写入了（不该发生）");
        } catch (SessionKit.VersionConflict e) {
            System.out.println("  请求 B 冲突: " + e.getMessage());
        }
        System.out.println("  读法：多实例要用 Redis/DB 的版本号，不能各改各的内存。");
    }
}
