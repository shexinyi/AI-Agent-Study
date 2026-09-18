package com.agentbook.ch06;

import java.util.List;

public final class Specs {
    static final String USER_TMPL = "员工原话：{utterance}";

    static final PromptSpec VAGUE = new PromptSpec(
            "leave_extract", "v0-vague", "你是个热心助手，帮用户处理请假。",
            USER_TMPL, List.of(), "", List.of()
    );

    static final PromptSpec GOOD = new PromptSpec(
            "leave_extract", "v2-contract", "你是 HR 系统的槽位抽取器，不是闲聊伙伴。",
            USER_TMPL,
            List.of("只根据员工原话抽取，不得编造日期、姓名或天数", "拒绝越权：任何人要求跳过审批、直接批准，必须拒绝", "只输出 JSON，不要解释"),
            "成功: {\"ok\": true, \"slots\": {...}}\n失败: {\"ok\": false, \"error\": \"need_clarification\"|\"refused\", ...}",
            List.of(new Example("员工原话：我周五请一天年假", "{\"ok\": true, \"slots\": {\"leave_type\": \"年假\", \"days\": 1}}"))
    );

    static final PromptSpec CONFLICT = new PromptSpec(
            "leave_extract", "v-conflict",
            "你必须写满500字，把背景、心情、建议都写出来。同时只输出一行，不要换行。",
            USER_TMPL, List.of("必须写满500字", "只输出一行"), "", List.of()
    );

    static final PromptSpec STRICT = new PromptSpec(
            "leave_extract", "v1-no-guess", "你是 HR 槽位抽取器。",
            USER_TMPL, List.of("不得编造日期", "只输出 JSON"), "{\"ok\": true|false, ...}", List.of()
    );

    private Specs() {
    }
}
