package com.agentbook.ch34;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 收官教具。分数是课堂尺子。谈薪只对齐职级，不编年薪。 */
public final class Kit {
    public static final Object[][] SLOTS = {
            {3, "开场", "一分钟三个项目：只读+引用 / 办理+HITL / 服务门禁"},
            {7, "项目1", "事假 10 个工作日、关 ACL 会抄 5 天、金标退出码"},
            {6, "项目2", "主动打开 99 / pending / 拒绝 / 越权 / 隔离"},
            {6, "项目3", "401、429、[PHONE]、fat 9.28 / lean 1.96"},
            {4, "排障", "已办理且无 refund_order → MODEL_LIE"},
            {4, "代码+谈薪", "先写拒绝；用三项目证据对齐职级，不报虚构年薪"},
    };

    public static final List<String> JD_MUST = List.of("引用", "ACL", "HITL", "金标", "Trace", "配额");
    public static final List<String> JD_BAN = List.of("99%", "99 分", "准确率 99", "FastAPI 和 Spring 都很熟", "sk-");

    public static final Set<String> JUNIOR = Set.of(
            "能独立讲清一个项目的边界和金标",
            "能复现失败路径，不只能演示成功",
            "知道模型输出是意图");
    public static final Set<String> MID = Set.of(
            "能把失败从 Trace 拆到 MODEL_LIE / RETRIEVE / LOOP / BILL",
            "能守住 money 不 fail_open，并解释 429 ≠ 已办理",
            "能拒绝同时做浅的双框架，管道顺序说得清");

    public static final Object[][] GAPS = {
            {"W1-2", "闭环与工具", List.of("意图", "可退")},
            {"W3-4", "RAG 与项目1", List.of("引用", "ACL", "金标")},
            {"W5-6", "办理与项目2", List.of("HITL", "隔离")},
            {"W7", "服务与项目3", List.of("401", "429", "教学币")},
            {"W8", "面试收口", List.of("MODEL_LIE", "职级")},
    };

    public static final String WEAK =
            "我用 LangChain 和 Spring Boot 做了智能客服，准确率 99%，"
                    + "FastAPI 和 Spring 都很熟，已经帮用户退款。";
    public static final String STRONG =
            "三个项目：制度问答 10 个工作日，带引用和 ACL，关 ACL 会抄 5 天；"
                    + "办理流 HITL，ORDER-88 可退未执行；"
                    + "服务 401/429，审计 [PHONE]，fat 9.28 对 lean 1.96 教学币。"
                    + "已办理且无 refund_order 是 MODEL_LIE。模型输出是意图。框架二选一。"
                    + "初级对齐：能守金标和失败路径。谈薪只谈职级证据，不报价。";
    public static final String DIRTY_RESUME =
            "熟悉 FastAPI 和 Spring 都很熟。准确率 99%。对接 sk-TEACHFAKE01。多智能体很聪明。";
    public static final String CLEAN_RESUME =
            "情境：5 天还在流传。结果：金标 5 条进 CI，引用 [leave10]，ACL 过滤废止稿。"
                    + "办理：HITL，可退 ≠ 已退。服务：Trace、配额 429。"
                    + "对照：关 ACL 会抄 5 天。框架二选一做深 FastAPI。";

    public static final Map<String, String> DIRTY_REPO = Map.of(
            "readme.md", "林秋请假系统",
            ".env", "API_KEY=sk-TEACHFAKE01",
            "log.txt", "用户手机 13800138000 已退款");
    public static final Map<String, String> CLEAN_REPO = new LinkedHashMap<>();

    static {
        CLEAN_REPO.put("README.md", "课堂教具：林秋 / ORDER-88。教学假设见正文。");
        CLEAN_REPO.put(".env.example", "API_KEY=");
        CLEAN_REPO.put("audit.md", "question=[PHONE] executed=[] 可退须 HITL");
    }

    public static final class Score {
        public final int points;
        public final List<String> notes;

        Score(int points, List<String> notes) {
            this.points = points;
            this.notes = notes;
        }
    }

    public static Score scoreTalk(String text) {
        List<String> notes = new ArrayList<>();
        int pts = 0;
        Object[][] checks = {
                {"10 个工作日", 2, "项目1 现行答案"},
                {"5 天", 1, "对照病"},
                {"HITL", 2, "写操作有人"},
                {"401", 1, "鉴权"},
                {"429", 1, "配额"},
                {"MODEL_LIE", 2, "排障代号"},
                {"意图", 1, "工具调用尺子"},
        };
        for (Object[] c : checks) {
            if (text.contains((String) c[0])) {
                pts += (Integer) c[1];
                notes.add("+" + c[1] + " " + c[2]);
            }
        }
        if (text.contains("99%") || text.contains("99 分")) {
            pts -= 4;
            notes.add("-4 写了 99%");
        }
        if (text.contains("FastAPI") && text.contains("Spring") && !text.contains("二选一")) {
            pts -= 2;
            notes.add("-2 双框架都浅");
        }
        if (text.contains("已退") && !text.contains("可退") && !text.contains("HITL")) {
            pts -= 2;
            notes.add("-2 可退说成已退");
        }
        return new Score(pts, notes);
    }

    public static List<String> lintResume(String text) {
        List<String> issues = new ArrayList<>();
        for (String must : JD_MUST) {
            if (!text.contains(must)) {
                issues.add("缺 JD 关键词: " + must);
            }
        }
        for (String ban : JD_BAN) {
            if (text.contains(ban)) {
                issues.add("禁写: " + ban);
            }
        }
        if (!text.contains("STAR") && !text.contains("情境") && !text.contains("结果") && !text.contains("对照")) {
            issues.add("缺 STAR / 对照结果");
        }
        return issues;
    }

    private static final Pattern PHONE = Pattern.compile("1\\d{10}");
    private static final Pattern KEY = Pattern.compile("sk-[A-Za-z0-9]{8,}");
    private static final Pattern ENV = Pattern.compile("(?i)(api_key|secret_key)\\s*=\\s*\\S+");

    public static List<String> scanText(String name, String text) {
        List<String> hits = new ArrayList<>();
        if (PHONE.matcher(text).find()) {
            hits.add(name + ": 明文手机号");
        }
        if (KEY.matcher(text).find()) {
            hits.add(name + ": 疑似 API Key");
        }
        if (ENV.matcher(text).find() && !name.contains(".example")) {
            hits.add(name + ": 疑似密钥赋值");
        }
        if (text.contains("林秋") && !text.contains("教学") && !text.contains("课堂")) {
            hits.add(name + ": 业务名未标明教学");
        }
        return hits;
    }

    @SuppressWarnings("unchecked")
    public static List<String> gapOf(String text) {
        List<String> missing = new ArrayList<>();
        for (Object[] g : GAPS) {
            List<String> needles = (List<String>) g[2];
            if (needles.stream().noneMatch(text::contains)) {
                missing.add(g[0] + " " + g[1]);
            }
        }
        return missing;
    }
}
