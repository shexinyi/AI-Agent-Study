package com.agentbook.ch27;

import java.util.ArrayList;
import java.util.List;

public final class FiveMinLocate {
    public static void main(String[] args) {
        System.out.println("五分钟四问：哪条 Trace？哪个 Span 红？kind 是工具/模型/检索？用户那句话对得上吗？");
        System.out.println("APM 对接：每个 Span 一行 JSON，字段名对齐 OpenTelemetry。");
        System.out.println();

        Object[][] incidents = {
                {"用户看见系统出错", Kit.incidentToolTimeout(), "TOOL"},
                {"用户看见已办理", Kit.incidentModelLie(), "MODEL_LIE"},
                {"用户看见 5 天", Kit.incidentStaleDoc(), "RETRIEVE"},
        };
        List<String> got = new ArrayList<>();
        List<String> evidences = new ArrayList<>();
        for (Object[] row : incidents) {
            MiniTrace.Tracer tr = (MiniTrace.Tracer) row[1];
            String[] loc = MiniTrace.locate(tr.trace);
            got.add(loc[0]);
            evidences.add(loc[1]);
            System.out.println("=== " + row[0] + " / " + tr.trace.traceId + " ===");
            System.out.println(tr.trace.tree());
            System.out.println("定位 " + loc[0] + " | " + loc[1]);
            System.out.println("期望 " + row[2] + "  " + (loc[0].equals(row[2]) ? "OK" : "FAIL"));
            System.out.println();
        }

        MiniTrace.Trace sample = Kit.incidentToolTimeout().trace;
        List<String> lines = MiniTrace.otelLines(sample);
        System.out.println("=== OTEL 示意（timeout 那条）===");
        System.out.println(lines.get(0));
        System.out.println("... 共 " + lines.size() + " 行，可丢给 Zipkin / Jaeger / 现有日志平台");
        System.out.println();

        Kit.check("三条根因对上", got.equals(List.of("TOOL", "MODEL_LIE", "RETRIEVE")));
        Kit.check("超时钉在 refund_order",
                evidences.get(0).contains("refund_order") && evidences.get(0).contains("3000"));
        Kit.check("导出带 trace_id", lines.get(0).contains("tr-timeout") && lines.get(0).contains("span_id"));
        Kit.check("导出带 parent", lines.stream().anyMatch(x -> x.contains("parent_span_id")));
        System.out.println("读法：LangSmith 是 LLM 专用 UI。现有 APM 收同一套 trace_id，不要另起炉灶。");
    }
}
