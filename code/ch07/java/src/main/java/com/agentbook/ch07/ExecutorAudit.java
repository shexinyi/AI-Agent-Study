package com.agentbook.ch07;

import java.util.Map;
import java.util.Set;

public class ExecutorAudit {
    public static void main(String[] args) {
        Map<String, ToolSpec> specs = Catalog.goodSpecs();
        ProductionExecutor executor = new ProductionExecutor(specs, Set.of("refund_order"));

        show("只读查单", executor.execute(new ToolCall("c1", "get_order", Map.of("order_id", "ORDER-88"), null)), executor);
        show("第一次退款", executor.execute(new ToolCall("c2", "refund_order", Map.of("order_id", "ORDER-88"), "refund-88-user")), executor);
        show("同一幂等键再退", executor.execute(new ToolCall("c3", "refund_order", Map.of("order_id", "ORDER-88"), "refund-88-user")), executor);

        ToolResult denied = new ProductionExecutor(specs, Set.of())
                .execute(new ToolCall("c4", "refund_order", Map.of("order_id", "ORDER-88"), "other"));
        System.out.println("\n### 未授权退款\n" + denied.error());

        show("超时", executor.execute(new ToolCall("c5", "slow_lookup", Map.of("order_id", "ORDER-88"), null)), executor);

        System.out.println("\n台账 REFUNDS = " + Catalog.REFUNDS + "  （长度应为 1）");
        System.out.println("本执行器审计:");
        for (AuditEvent ev : executor.audit) {
            System.out.println(" - " + ev.tool() + " ok=" + ev.ok() + " replay=" + ev.replayed()
                    + " " + ev.latencyMs() + "ms " + (ev.error() == null ? "" : ev.error()));
        }
    }

    static void show(String title, ToolResult result, ProductionExecutor executor) {
        System.out.println("\n### " + title);
        System.out.println("ok=" + result.ok() + " replayed=" + result.replayed()
                + " data=" + result.data() + " error=" + result.error());
        AuditEvent ev = executor.audit.get(executor.audit.size() - 1);
        System.out.println("审计: tool=" + ev.tool() + " risk=" + ev.risk()
                + " " + ev.latencyMs() + "ms replayed=" + ev.replayed());
    }
}
