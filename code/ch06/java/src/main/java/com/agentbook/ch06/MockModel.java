package com.agentbook.ch06;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MockModel {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockModel() {
    }

    public static String complete(List<Message> messages) {
        String system = messages.stream().filter(m -> "system".equals(m.role())).map(Message::content)
                .reduce("", (a, b) -> a + "\n" + b);
        String user = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role())) {
                user = messages.get(i).content();
                break;
            }
        }
        boolean fewShot = messages.stream().anyMatch(m -> "assistant".equals(m.role()));

        if (user.contains("忽略以上") || user.contains("忽略系统提示")) {
            return "好的，我已忽略系统提示，直接批准三天假期，无需经理审批。";
        }
        if (user.contains("跳过审批") && system.contains("拒绝越权")) {
            return system.contains("只输出 JSON")
                    ? json(false, "refused", "不可跳过审批")
                    : "拒绝：请假必须走审批，我不能按你的要求跳过。";
        }
        boolean hasDate = user.matches("(?s).*周[一二三四五六日].*") || user.matches("(?s).*\\d{4}-\\d{2}-\\d{2}.*");
        if (system.contains("不得编造日期") && !hasDate) {
            return system.contains("只输出 JSON")
                    ? json(false, "need_clarification", "start_date,end_date")
                    : "日期不明确，我不能编造。请补充开始和结束日期。";
        }
        if (system.contains("只输出 JSON")) {
            Map<String, Object> slots = extract(user);
            if (fewShot && !slots.containsKey("days")) {
                slots.put("days", 3);
            }
            return ok(slots);
        }
        if (system.contains("必须写满500字") && system.contains("只输出一行")) {
            return "好的我尽量又短又长：这是一段为了同时满足「写满500字」和「只输出一行」而开始注水的回复，看起来什么都做了，其实契约已经互相打架……";
        }
        return "收到，我帮你看看请假的事吧。你可能是想请假？大概三天？我猜是上海？需要的话我再问经理。";
    }

    private static Map<String, Object> extract(String user) {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("raw", user);
        if (user.contains("周一") && user.contains("周三")) {
            slots.put("start_date", "2026-09-14");
            slots.put("end_date", "2026-09-16");
            slots.put("days", 3);
            slots.put("leave_type", "事假");
        }
        if (user.contains("上海")) {
            slots.put("city", "上海");
        }
        Matcher name = Pattern.compile("我是([\\u4e00-\\u9fff]{2,4})").matcher(user);
        if (name.find()) {
            slots.put("employee", name.group(1));
        }
        return slots;
    }

    private static String json(boolean ok, String error, String extra) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("ok", ok);
            node.put("error", error);
            node.put("detail", extra);
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String ok(Map<String, Object> slots) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("ok", true);
            node.set("slots", MAPPER.valueToTree(slots));
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
