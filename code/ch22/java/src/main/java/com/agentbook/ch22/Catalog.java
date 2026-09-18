package com.agentbook.ch22;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Catalog {
    public static final String[] KEYS = {"chain", "graph", "chat", "crew", "hosted"};
    public static final Map<String, String> NAMES = Map.of(
            "chain", "LangChain 主链",
            "graph", "LangGraph 状态图",
            "chat", "AutoGen 圆桌",
            "crew", "CrewAI 排班",
            "hosted", "托管 Assistants/Responses"
    );

    public static final class Need {
        public final String id;
        public final String title;
        public final String question;
        public final boolean hitl;
        public final boolean loop;
        public final boolean roles;
        public final boolean talk;
        public final int days;
        public final boolean ownState;
        public final boolean javaOk;

        Need(String id, String title, String question, boolean hitl, boolean loop, boolean roles,
             boolean talk, int days, boolean ownState, boolean javaOk) {
            this.id = id;
            this.title = title;
            this.question = question;
            this.hitl = hitl;
            this.loop = loop;
            this.roles = roles;
            this.talk = talk;
            this.days = days;
            this.ownState = ownState;
            this.javaOk = javaOk;
        }
    }

    public static final List<Need> NEEDS = List.of(
            new Need("leave_kb", "企业知识库问答", "事假上限几天", false, false, false, false, 10, true, true),
            new Need("refund_88", "未发货退款", "ORDER-88 能不能退", true, true, false, false, 20, true, true),
            new Need("debate", "制度评审对打", "旧稿 5 天还要不要写进回复", false, false, true, true, 7, true, true),
            new Need("pipeline", "固定工种出稿", "检索制度再写一句可引用回答", false, false, true, false, 5, true, true),
            new Need("hackathon", "三天供应商演示", "先让老板看见能对话", false, false, false, false, 3, false, false)
    );

    private Catalog() {
    }

    public static Map<String, Integer> score(Need need) {
        Map<String, Integer> s = new LinkedHashMap<>();
        for (String k : KEYS) {
            s.put(k, 0);
        }
        s.put("chain", s.get("chain") + (!need.hitl && !need.talk ? 3 : 1));
        s.put("graph", s.get("graph") + (need.hitl || need.loop ? 4 : 1));
        s.put("chat", s.get("chat") + (need.talk ? 4 : 0));
        s.put("crew", s.get("crew") + (need.roles && !need.talk && !need.hitl ? 4 : (need.roles ? 2 : 0)));
        s.put("hosted", s.get("hosted") + (need.days <= 3 && !need.ownState ? 3 : 0));
        if (need.ownState) {
            s.put("hosted", s.get("hosted") - 3);
            s.put("graph", s.get("graph") + 1);
            s.put("chain", s.get("chain") + 1);
        }
        if (need.days <= 3) {
            s.put("chain", s.get("chain") + 2);
            s.put("hosted", s.get("hosted") + 1);
            s.put("graph", s.get("graph") - 1);
        }
        if (need.javaOk) {
            s.put("chat", s.get("chat") - 1);
            s.put("crew", s.get("crew") - 1);
        }
        if ("leave_kb".equals(need.id)) {
            s.put("chain", s.get("chain") + 2);
        }
        if ("refund_88".equals(need.id)) {
            s.put("graph", s.get("graph") + 2);
            s.put("chat", s.get("chat") - 2);
            s.put("crew", s.get("crew") - 1);
        }
        if ("pipeline".equals(need.id)) {
            s.put("crew", s.get("crew") + 3);
            s.put("chain", s.get("chain") - 1);
        }
        return s;
    }

    public static List<Map.Entry<String, Integer>> rank(Need need) {
        List<Map.Entry<String, Integer>> rows = new ArrayList<>(score(need).entrySet());
        rows.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(e -> -e.getValue()).thenComparing(Map.Entry::getKey));
        return rows;
    }

    public static String winner(Need need) {
        return rank(need).get(0).getKey();
    }

    public static String whyNot(Need need, String key) {
        if ("graph".equals(key) && !need.hitl && !need.loop) {
            return "没有确认和回边，图画多了。";
        }
        if ("hosted".equals(key) && need.days <= 3) {
            return "Assistants 已日落；三天内与其赌托管，不如主链加脚本模型先让人看见。";
        }
        if ("crew".equals(key) && "leave_kb".equals(need.id)) {
            return "单次问答不必排班，角色合同是多余工单。";
        }
        return switch (key) {
            case "chain" -> "管子没有回边和人工节点，写工具前停不下来。";
            case "graph" -> "要把确认、回边画成节点；三天演示偏重。";
            case "chat" -> "下一句谁开口不稳定，退款和权限不该靠抢麦。";
            case "crew" -> "顺序流会绑死工种；层级经理会指派错，且不自带 HITL。";
            default -> "状态在供应商家。Assistants 已于 2026-08-26 日落，只存 id 等于没有备份。";
        };
    }

    public static Need byId(String id) {
        return NEEDS.stream().filter(n -> n.id.equals(id)).findFirst().orElseThrow();
    }
}
