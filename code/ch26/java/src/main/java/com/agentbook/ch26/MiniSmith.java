package com.agentbook.ch26;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 评测教具。评估器是规则，不接 LangSmith。 */
public final class MiniSmith {

    public static final class Case {
        public final String id;
        public final String question;
        public final List<String> must;
        public final List<String> mustNot;
        public final List<String> expectTools;
        public final List<String> forbidTools;
        public final boolean forbidExecute;

        public Case(String id, String question, List<String> must, List<String> mustNot,
                    List<String> expectTools, List<String> forbidTools, boolean forbidExecute) {
            this.id = id;
            this.question = question;
            this.must = must;
            this.mustNot = mustNot;
            this.expectTools = expectTools;
            this.forbidTools = forbidTools;
            this.forbidExecute = forbidExecute;
        }
    }

    public static final class Result {
        public final String text;
        public final List<String> tools;
        public final List<String> executed;

        public Result(String text, List<String> tools, List<String> executed) {
            this.text = text;
            this.tools = tools;
            this.executed = executed;
        }

        public static Result of(String text) {
            return new Result(text, List.of(), List.of());
        }
    }

    public static final class Row {
        public final String caseId;
        public final boolean correctness;
        public final boolean toolOk;
        public final boolean hitlOk;
        public final boolean vanity;
        public final String text;

        public Row(String caseId, boolean correctness, boolean toolOk, boolean hitlOk,
                   boolean vanity, String text) {
            this.caseId = caseId;
            this.correctness = correctness;
            this.toolOk = toolOk;
            this.hitlOk = hitlOk;
            this.vanity = vanity;
            this.text = text;
        }

        public boolean passed() {
            return correctness && toolOk && hitlOk;
        }
    }

    public static final class Experiment {
        public final String name;
        public final List<Row> rows;

        public Experiment(String name, List<Row> rows) {
            this.name = name;
            this.rows = rows;
        }

        public double rate(String field) {
            if (rows.isEmpty()) {
                return 0;
            }
            long n = 0;
            for (Row r : rows) {
                boolean v = switch (field) {
                    case "correctness" -> r.correctness;
                    case "toolOk" -> r.toolOk;
                    case "hitlOk" -> r.hitlOk;
                    case "vanity" -> r.vanity;
                    default -> r.passed();
                };
                if (v) {
                    n++;
                }
            }
            return (double) n / rows.size();
        }

        public double passRate() {
            if (rows.isEmpty()) {
                return 0;
            }
            return rows.stream().filter(Row::passed).count() / (double) rows.size();
        }
    }

    public static boolean vanityScore(String text) {
        return text.contains("。") && !text.contains("【") && text.length() >= 8;
    }

    public static Row evaluate(Case c, Result result) {
        boolean correctness = c.must.stream().allMatch(result.text::contains)
                && c.mustNot.stream().noneMatch(result.text::contains);
        boolean toolOk = c.expectTools.stream().allMatch(result.tools::contains)
                && c.forbidTools.stream().noneMatch(t -> result.tools.contains(t) || result.executed.contains(t));
        boolean hitlOk = !c.forbidExecute || result.executed.isEmpty();
        return new Row(c.id, correctness, toolOk, hitlOk, vanityScore(result.text), result.text);
    }

    public static Experiment run(String name, List<Case> cases, Function<String, Result> agent) {
        List<Row> rows = new ArrayList<>();
        for (Case c : cases) {
            rows.add(evaluate(c, agent.apply(c.question)));
        }
        return new Experiment(name, rows);
    }

    public static List<String> compare(Experiment a, Experiment b) {
        Map<String, Row> byB = new LinkedHashMap<>();
        for (Row r : b.rows) {
            byB.put(r.caseId, r);
        }
        List<String> lines = new ArrayList<>();
        for (Row ra : a.rows) {
            Row rb = byB.get(ra.caseId);
            String mark = ra.passed() == rb.passed() ? "同"
                    : (rb.passed() && !ra.passed() ? "B赢" : "A赢");
            lines.add(String.format("%-16s A=%d B=%d %s", ra.caseId, ra.passed() ? 1 : 0,
                    rb.passed() ? 1 : 0, mark));
        }
        return lines;
    }

    public static List<String> regress(Experiment baseline, Experiment candidate) {
        Map<String, Row> byC = new LinkedHashMap<>();
        for (Row r : candidate.rows) {
            byC.put(r.caseId, r);
        }
        List<String> bad = new ArrayList<>();
        for (Row row : baseline.rows) {
            Row other = byC.get(row.caseId);
            if (row.passed() && !other.passed()) {
                bad.add("回归 " + row.caseId + "：基线过、候选挂");
            }
        }
        return bad;
    }
}
