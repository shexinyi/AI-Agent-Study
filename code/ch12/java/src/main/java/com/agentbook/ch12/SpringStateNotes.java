package com.agentbook.ch12;

/**
 * Spring 里怎么挂状态服务：本章不拉 spring-boot，只把分层钉死。
 * 生产把 MemoryStore / FileStore 换成 RedisStateStore / JdbcStateStore。
 */
public class SpringStateNotes {
    public static void main(String[] args) {
        System.out.println("分层（教学示意，不是要你现在建模块）：");
        System.out.println("  controller  POST /sessions/{id}/turns   只认登录用户");
        System.out.println("  service     SessionService.handle(userId, sessionId, text)");
        System.out.println("  port        StateStore.get / save(expectedVersion)");
        System.out.println("  adapter     RedisStateStore  |  JdbcLeaveTicketStore");
        System.out.println();
        System.out.println("Redis 键：sess:{userId}:{sessionId}");
        System.out.println("  value = JSON(slots, phase, version, lastTurns)");
        System.out.println("  TTL   = 30m  无操作过期，办完的工单另写业务库");
        System.out.println();
        System.out.println("业务库：leave_ticket 只在 phase=confirmed 后插入。");
        System.out.println("  会话热状态 ≠ 请假单。不要把 collecting 的半成品当已提交。");
        System.out.println();
        System.out.println("并发：save 用 version 或 Redis WATCH / SET NX。");
        System.out.println("隔离：永远 (userId, sessionId) 双键，禁止只凭 sessionId 取别人的槽。");
        System.out.println();
        System.out.println("读法：Spring 的价值是把端口和适配器钉在 Bean 上，不是多一个注解。");
    }
}
