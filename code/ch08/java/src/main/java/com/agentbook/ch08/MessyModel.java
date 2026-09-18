package com.agentbook.ch08;

public final class MessyModel {
    private MessyModel() {
    }

    public static String complete(String mode) {
        return switch (mode) {
            case "prose" -> "好的呀，林秋大概下周一到周三请三天假去上海，我帮你记下了～";
            case "fenced_bad" -> """
                    抽取结果如下：
                    ```json
                    {"ok": true, "slots": {"employee": "林秋", "start_date": "2026-09-14", "end_date": "2026-09-16", "days": 5, "leave_type": "事假"}}
                    ```
                    另外我建议她带把伞。
                    """;
            case "missing_field" -> "{\"ok\": true, \"slots\": {\"employee\": \"林\", \"start_date\": \"2026-09-14\"}}";
            case "valid" -> "{\"ok\": true, \"slots\": {\"employee\": \"林秋\", \"start_date\": \"2026-09-14\", \"end_date\": \"2026-09-16\", \"days\": 3, \"leave_type\": \"事假\"}}";
            case "tool_args" -> "{\"name\": \"submit_leave_extract\", \"arguments\": {\"ok\": true, \"slots\": {\"employee\": \"林秋\", \"start_date\": \"2026-09-14\", \"end_date\": \"2026-09-16\", \"days\": 3, \"leave_type\": \"事假\"}}}";
            default -> throw new IllegalArgumentException(mode);
        };
    }
}
