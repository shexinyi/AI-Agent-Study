"""示例 3：三条事故 5 分钟定位，并导出一条 OTEL 示意行。"""

from kit import incident_model_lie, incident_stale_doc, incident_tool_timeout
from minitrace import locate, otel_lines

INCIDENTS = [
    ("用户看见系统出错", incident_tool_timeout, "TOOL"),
    ("用户看见已办理", incident_model_lie, "MODEL_LIE"),
    ("用户看见 5 天", incident_stale_doc, "RETRIEVE"),
]


if __name__ == "__main__":
    print("五分钟四问：哪条 Trace？哪个 Span 红？kind 是工具/模型/检索？用户那句话对得上吗？")
    print("APM 对接：每个 Span 一行 JSON，字段名对齐 OpenTelemetry。\n")
    got: list[str] = []
    evidences: list[str] = []
    for title, factory, expect in INCIDENTS:
        tr = factory()
        assert tr.trace is not None
        kind, evidence = locate(tr.trace)
        got.append(kind)
        evidences.append(evidence)
        print(f"=== {title} / {tr.trace.trace_id} ===")
        print(tr.trace.tree())
        print(f"定位 {kind} | {evidence}")
        print(f"期望 {expect}  {'OK' if kind == expect else 'FAIL'}")
        print()

    sample = incident_tool_timeout().trace
    assert sample is not None
    print("=== OTEL 示意（timeout 那条）===")
    lines = otel_lines(sample)
    print(lines[0])
    print(f"... 共 {len(lines)} 行，可丢给 Zipkin / Jaeger / 现有日志平台")
    print()

    checks = [
        ("三条根因对上", got == ["TOOL", "MODEL_LIE", "RETRIEVE"]),
        ("超时钉在 refund_order", "refund_order" in evidences[0] and "3000" in evidences[0]),
        ("导出带 trace_id", "tr-timeout" in lines[0] and "span_id" in lines[0]),
        ("导出带 parent", any("parent_span_id" in x for x in lines)),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：LangSmith 是 LLM 专用 UI。现有 APM 收同一套 trace_id，不要另起炉灶。")
