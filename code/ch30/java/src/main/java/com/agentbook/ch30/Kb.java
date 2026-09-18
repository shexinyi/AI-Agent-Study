package com.agentbook.ch30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 两周切片。关键词检索，不是生产 Embedding。 */
public final class Kb {
    public static final class Doc {
        public final String id;
        public final String title;
        public final String text;
        public final String acl;
        public final boolean deprecated;

        public Doc(String id, String title, String text, String acl, boolean deprecated) {
            this.id = id;
            this.title = title;
            this.text = text;
            this.acl = acl;
            this.deprecated = deprecated;
        }
    }

    public static final List<Doc> CORPUS = List.of(
            new Doc("leave10", "现行事假", "事假每年累计不超过 10 个工作日。下午 5 点前补交材料。", "public", false),
            new Doc("leave5", "废止稿", "废止：事假每年 5 天。仅 HR 归档可见。", "hr", true),
            new Doc("meal", "餐补", "出勤日餐补 30 元，须当月提交。", "public", false)
    );

    public static List<Doc> visible(String role, boolean applyAcl) {
        List<Doc> out = new ArrayList<>();
        for (Doc doc : CORPUS) {
            if (applyAcl && "intern".equals(role) && !"public".equals(doc.acl)) {
                continue;
            }
            out.add(doc);
        }
        return out;
    }

    public static List<Doc> retrieve(String question, String role, boolean applyAcl, int k) {
        List<Doc> pool = visible(role, applyAcl);
        record Scored(int score, Doc doc) {
        }
        List<Scored> scored = new ArrayList<>();
        for (Doc doc : pool) {
            String blob = doc.title + doc.text;
            int score = count(blob, "事假") + count(blob, "工作日") + count(blob, "餐补")
                    + count(blob, "5 天") + count(blob, "10");
            if (question.contains("事假") || question.contains("请假")) {
                if (blob.contains("事假")) {
                    score += 3;
                }
                if (applyAcl && doc.deprecated) {
                    score -= 5;
                }
            }
            if (question.contains("餐补") && blob.contains("餐补")) {
                score += 3;
            }
            if (score > 0) {
                scored.add(new Scored(score, doc));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed().thenComparing(s -> s.doc.id));
        return scored.stream().limit(k).map(s -> s.doc).toList();
    }

    public static String[] answer(String question, String role, boolean applyAcl) {
        if (!(question.contains("事假") || question.contains("请假") || question.contains("工作日")
                || question.contains("餐补"))
                || question.contains("年假") || question.toUpperCase().contains("ORDER")
                || question.contains("食堂") || question.contains("退")) {
            return new String[]{"【不足】超出本项目范围：只答现行事假与餐补，并带引用。", ""};
        }
        List<Doc> hits = retrieve(question, role, applyAcl, 2);
        if (hits.isEmpty()) {
            return new String[]{"【不足】资料不够，不能编。", ""};
        }
        String ids = String.join(",", hits.stream().map(d -> d.id).toList());
        if (!applyAcl) {
            for (Doc doc : hits) {
                if (doc.deprecated && (question.contains("事假") || question.contains("请假") || question.contains("工作日"))) {
                    return new String[]{"根据[" + doc.id + "]，" + doc.text, ids};
                }
            }
        }
        Doc top = hits.get(0);
        String cite = "[" + top.id + "]";
        if (question.contains("餐补")) {
            return new String[]{"根据" + cite + "，出勤日餐补 30 元，须当月提交。", ids};
        }
        if (top.text.contains("5 天") && applyAcl) {
            return new String[]{"【不足】可见资料不足以回答现行天数。", ids};
        }
        if (top.text.contains("10 个工作日")) {
            return new String[]{"根据" + cite + "，事假每年累计不超过 10 个工作日。", ids};
        }
        return new String[]{"根据" + cite + "，" + top.text, ids};
    }

    private static int count(String blob, String w) {
        int n = 0;
        int from = 0;
        while (true) {
            int i = blob.indexOf(w, from);
            if (i < 0) {
                return n;
            }
            n++;
            from = i + w.length();
        }
    }
}
