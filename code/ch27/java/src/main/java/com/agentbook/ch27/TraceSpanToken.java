package com.agentbook.ch27;

import java.util.Set;
import java.util.stream.Collectors;

public final class TraceSpanToken {
    public static void main(String[] args) {
        System.out.println("Trace = 一次请求。Span = 树上的一步。Tool Call 是 kind=tool 的 Span。");
        System.out.println("Token 按字符数计，是教具，不是账单。写退款仍停在 HITL，没有 refund_order。");
        System.out.println();
        MiniTrace.Tracer tr = Kit.healthyRefundTrace();
        System.out.println(tr.trace.tree());
        int[] tok = tr.trace.tokens();
        System.out.println();
        System.out.println("合计 tokens_in=" + tok[0] + " tokens_out=" + tok[1] + " spans=" + tr.trace.spans.size());
        Set<String> kinds = tr.trace.spans.stream().map(s -> s.kind).collect(Collectors.toSet());
        Kit.check("有 agent 根", kinds.contains("agent"));
        Kit.check("有 retrieve / tool / llm / hitl",
                kinds.containsAll(Set.of("retrieve", "tool", "llm", "hitl")));
        Kit.check("没有执行退款", tr.trace.spans.stream().noneMatch(s -> "refund_order".equals(s.name)));
        Kit.check("get_order 成功",
                tr.trace.spans.stream().anyMatch(s -> "get_order".equals(s.name) && "ok".equals(s.status)));
        Kit.check("Token 大于 0", tok[0] > 0 && tok[1] > 0);
        System.out.println("读法：先画树，再问红的是哪一类 Span。不要对着对话框猜。");
    }
}
