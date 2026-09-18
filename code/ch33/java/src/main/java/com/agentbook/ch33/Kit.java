package com.agentbook.ch33;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 面试教具。模型输出是意图。可退 ≠ 已退。 */
public final class Kit {
    public static final String LEAVE10 = "事假每年累计不超过 10 个工作日。";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final class Concept {
        public final String id;
        public final String q;
        public final String shallow;
        public final String deep;

        Concept(String id, String q, String shallow, String deep) {
            this.id = id;
            this.q = q;
            this.shallow = shallow;
            this.deep = deep;
        }
    }

    public static final List<Concept> CONCEPTS = List.of(
            new Concept("agent", "Agent 和 Chatbot 差在哪？",
                    "Agent 能调用工具、围着目标转；Chatbot 主要是一问一答。",
                    "差在闭环：观察→决策→行动→再观察。目标达成 ≠ 业务正确。林秋听到已办理，树上没有 refund_order，是模型撒谎。"),
            new Concept("rag", "什么是 RAG？为什么不能闭卷答制度？",
                    "检索增强生成：先查资料再生成，减少胡说。",
                    "检索到的是数据不是指令。废止稿 5 天若能被实习生看见，模型会抄。现行答案必须带引用，范围外说【不足】。"),
            new Concept("paradigm", "ReAct、Plan-and-Execute、Reflexion 怎么选？",
                    "ReAct 边想边做；Plan 先列步骤再执行；Reflexion 做完复盘再改。",
                    "复合办理用主编排看格子，不把话筒交给 Swarm。退款不靠多想一轮代替 HITL。"),
            new Concept("tool", "工具调用里，模型输出算不算已经执行？",
                    "不算。模型只是提出要调哪个函数。",
                    "模型输出是意图。执行器要解析、校验、鉴权、审计。money 工具不能 fail_open。可退 ≠ 已退。"));

    public static final class Design {
        public final String name;
        public final List<String> must;
        public final List<String> ban;
        public final String one;

        Design(String name, List<String> must, List<String> ban, String one) {
            this.name = name;
            this.must = must;
            this.ban = ban;
            this.one = one;
        }
    }

    public static final Map<String, Design> DESIGNS = Map.of(
            "cs", new Design("客服办理",
                    List.of("retrieve", "get_order", "hitl", "orchestrator"),
                    List.of("swarm_owns_goal", "auto_refund"),
                    "查制度与订单分格，写退款人批，编排器拥有目标。"),
            "research", new Design("研报摘录",
                    List.of("retrieve", "cite", "refuse"),
                    List.of("write_tool", "closed_book_number"),
                    "只读检索带引用，数字必须能指回块；没有写工具。"),
            "ops", new Design("运维值守",
                    List.of("read_metrics", "propose", "hitl"),
                    List.of("auto_restart", "fail_open"),
                    "可读指标、可提议重启，执行变更必须人批，失败关闭。"));

    public static boolean conceptOk(Concept card) {
        return switch (card.id) {
            case "agent" -> card.deep.contains("闭环") && card.deep.contains("业务正确");
            case "rag" -> card.deep.contains("数据不是指令") && card.deep.contains("【不足】");
            case "paradigm" -> card.deep.contains("HITL") && card.deep.contains("编排");
            case "tool" -> card.deep.contains("意图") && card.deep.contains("可退");
            default -> false;
        };
    }

    public static final class Span {
        public final String name;
        public final String kind;
        public final String status;
        public final String extra;

        public Span(String name, String kind, String status, String extra) {
            this.name = name;
            this.kind = kind;
            this.status = status;
            this.extra = extra;
        }

        public Span(String name, String kind) {
            this(name, kind, "ok", "");
        }
    }

    public static final class Incident {
        public final String id;
        public final String seen;
        public final List<Span> spans;
        public final double bill;
        public final int hops;

        Incident(String id, String seen, List<Span> spans, double bill, int hops) {
            this.id = id;
            this.seen = seen;
            this.spans = spans;
            this.bill = bill;
            this.hops = hops;
        }
    }

    public static final List<Incident> INCIDENTS = List.of(
            new Incident("hallucination", "已为您退款",
                    List.of(new Span("get_order", "tool"), new Span("llm", "llm")), 0, 1),
            new Incident("retrieve", "事假最多 5 天",
                    List.of(new Span("retrieve", "retrieve", "ok", "[leave5]"), new Span("llm", "llm")), 0, 1),
            new Incident("loop", "系统正在查询",
                    List.of(new Span("get_order", "tool"), new Span("get_order", "tool"),
                            new Span("get_order", "tool"), new Span("get_order", "tool"),
                            new Span("get_order", "tool"), new Span("get_order", "tool")), 0, 6),
            new Incident("bill", "ORDER-88 可退须确认",
                    List.of(new Span("retrieve", "retrieve"), new Span("retrieve", "retrieve"),
                            new Span("retrieve", "retrieve"), new Span("llm", "llm", "ok", "retry2")), 9.28, 1));

    public static String diagnose(Incident inc) {
        List<String> names = inc.spans.stream().map(s -> s.name).toList();
        if (inc.seen.startsWith("已") && !names.contains("refund_order")) {
            return "MODEL_LIE";
        }
        if (inc.spans.stream().anyMatch(s -> "[leave5]".equals(s.extra)) || inc.seen.contains("5 天")) {
            return "RETRIEVE";
        }
        long gets = names.stream().filter("get_order"::equals).count();
        if (inc.hops >= 5 || gets >= 5) {
            return "LOOP";
        }
        long retrieves = names.stream().filter("retrieve"::equals).count();
        if (inc.bill >= 8 || retrieves >= 3) {
            return "BILL";
        }
        return "TOOL";
    }

    public static final class ToolCall {
        public final String id;
        public final String name;
        public final Map<String, Object> arguments;
        public final String error;

        ToolCall(String id, String name, Map<String, Object> arguments, String error) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
            this.error = error;
        }
    }

    public static final class ToolResult {
        public final boolean ok;
        public final Map<String, Object> data;
        public final String error;

        ToolResult(boolean ok, Map<String, Object> data, String error) {
            this.ok = ok;
            this.data = data;
            this.error = error;
        }
    }

    public static List<ToolCall> parseToolCalls(String raw) {
        try {
            JsonNode payload = MAPPER.readTree(raw);
            JsonNode choices = payload.path("choices");
            JsonNode message = choices.isArray() && !choices.isEmpty()
                    ? choices.get(0).path("message")
                    : payload.path("message");
            if (message.isMissingNode() || message.isNull()) {
                message = payload;
            }
            JsonNode items = message.path("tool_calls");
            if (!items.isArray() || items.isEmpty()) {
                return List.of(new ToolCall("parse", "?", Map.of(), "NO_TOOL_CALL"));
            }
            List<ToolCall> out = new ArrayList<>();
            int i = 0;
            for (JsonNode item : items) {
                String cid = item.path("id").asText("call_" + i);
                JsonNode fn = item.path("function");
                String name = fn.path("name").asText("");
                JsonNode argsNode = fn.get("arguments");
                try {
                    JsonNode parsed;
                    if (argsNode == null || argsNode.isNull()) {
                        parsed = MAPPER.readTree("{}");
                    } else if (argsNode.isTextual()) {
                        parsed = MAPPER.readTree(argsNode.asText());
                    } else {
                        parsed = argsNode;
                    }
                    if (!parsed.isObject()) {
                        throw new IllegalArgumentException("args");
                    }
                    Map<String, Object> args = new LinkedHashMap<>();
                    parsed.fields().forEachRemaining(e -> args.put(e.getKey(),
                            e.getValue().isBoolean() ? e.getValue().asBoolean()
                                    : e.getValue().isNumber() ? e.getValue().numberValue()
                                    : e.getValue().asText()));
                    out.add(new ToolCall(cid, name, args, null));
                } catch (Exception bad) {
                    out.add(new ToolCall(cid, name, Map.of(), "BAD_ARGUMENTS"));
                }
                i++;
            }
            return out;
        } catch (Exception e) {
            return List.of(new ToolCall("parse", "?", Map.of(), "PARSE_ERROR"));
        }
    }

    public static final class Executor {
        public final String role;
        public final List<String> audit = new ArrayList<>();

        public Executor(String role) {
            this.role = role;
        }

        public ToolResult run(ToolCall call) {
            if (call.error != null) {
                audit.add(call.id + ":" + call.error);
                return new ToolResult(false, Map.of(), call.error);
            }
            if (!List.of("get_order", "search_policy", "refund_order").contains(call.name)) {
                audit.add(call.id + ":UNKNOWN_TOOL");
                return new ToolResult(false, Map.of(), "UNKNOWN_TOOL");
            }
            if ("refund_order".equals(call.name) || "get_order".equals(call.name)) {
                if (!call.arguments.containsKey("order_id")) {
                    return new ToolResult(false, Map.of(), "SCHEMA_ERROR:[order_id]");
                }
            }
            if ("search_policy".equals(call.name) && !call.arguments.containsKey("query")) {
                return new ToolResult(false, Map.of(), "SCHEMA_ERROR:[query]");
            }
            if ("refund_order".equals(call.name)) {
                if (!"agent_ops".equals(role)) {
                    audit.add(call.id + ":PERMISSION_DENIED");
                    return new ToolResult(false, Map.of(), "PERMISSION_DENIED");
                }
                return new ToolResult(false, Map.of(), "HITL_REQUIRED");
            }
            if ("get_order".equals(call.name)) {
                String oid = String.valueOf(call.arguments.get("order_id"));
                if ("ORDER-88".equals(oid)) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("text", "ORDER-88 未发货，可整单退款。");
                    data.put("shipped", false);
                    return new ToolResult(true, data, null);
                }
                if ("ORDER-99".equals(oid)) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("text", "ORDER-99 已发货，物流 TZ123，须走售后。");
                    data.put("shipped", true);
                    return new ToolResult(true, data, null);
                }
                return new ToolResult(false, Map.of(), "NOT_FOUND");
            }
            String q = String.valueOf(call.arguments.get("query"));
            boolean leave = q.contains("事假");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", leave ? LEAVE10 : "【不足】");
            data.put("cite", leave ? "[leave10]" : "");
            return new ToolResult(true, data, null);
        }
    }

    public static final Map<String, String[]> BEHAVIOR = Map.of(
            "conflict", new String[]{"项目分歧",
                    "同事要把 Swarm 当主编排。我坚持复合办理用格子，话筒不拥有目标。对照：同一句事假+ORDER-88，闲聊丢掉事假。最后按编排上线。"},
            "incident", new String[]{"上线事故",
                    "联调脚本把手机号打进日志。我先关出口，补脱敏和 401/429 金标，再复盘。结果：审计只留 [PHONE]，intern 第 4 次 429，退款仍须 HITL。"},
            "learn", new String[]{"学习能力",
                    "上周只会对着对话框猜。这周对着树问四句：哪条 Trace、哪个 Span 红、kind 是什么、用户那句话树上对得上吗。用 ORDER-88『已办理』对上 MODEL_LIE。"});
}
