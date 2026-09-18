package com.agentbook.ch16;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiseasesAndCi {
    public static void main(String[] args) {
        EvalKit.GoldFile gold = EvalKit.load(Path.of("goldenset.json"));
        List<RagKit.Chunk> heading = RagKit.buildCorpus(Path.of("kb"), "heading");
        List<RagKit.Chunk> coarse = RagKit.buildCorpus(Path.of("kb"), "coarse");
        Map<String, EvalKit.GoldCase> cases = gold.cases.stream().collect(Collectors.toMap(c -> c.id, c -> c));

        System.out.println("病 1 切块不当：整库糊成一块。5 天和 10 天捆在同一上下文。");
        EvalKit.Row coarseRow = EvalKit.eval(cases.get("leave_days"), coarse, "vector", gold.k, "", true, true);
        System.out.printf("  Hit@3=%.0f（金标标题已不存在）  正确=%s  答含5天? %s%n",
                coarseRow.hitK, coarseRow.correct, coarseRow.answer.contains("5 天"));
        System.out.println("  答摘录: " + coarseRow.answer.substring(0, Math.min(80, coarseRow.answer.length())) + "...");

        System.out.println("\n病 2 过期文档：HR 能看见废止稿，若不过滤 kind=deprecated，「事假最多几天」会吃到 5 天。");
        EvalKit.GoldCase staleCase = copy(cases.get("leave_max"));
        staleCase.role = "hr";
        EvalKit.Row stale = EvalKit.eval(staleCase, heading, "prod", gold.k, "", false, true);
        System.out.printf("  Top=%s  泄漏废止稿=%s  正确=%s%n", stale.headings, stale.leak, stale.correct);

        System.out.println("\n病 3 权限泄漏：实习生若关掉 ACL 过滤，废止稿（acl=hr）会进 Top-K。");
        EvalKit.Row leaky = EvalKit.eval(cases.get("leave_max"), heading, "prod", gold.k, "", false, false);
        System.out.printf("  Top=%s  泄漏废止稿=%s  正确=%s%n", leaky.headings, leaky.leak, leaky.correct);

        System.out.println("\n--- CI 门禁：生产管线 = 标题切 + 丢掉废止稿 + intern 只看 acl=all + 混合重排 ---");
        List<EvalKit.Row> rows = EvalKit.suite(gold, heading, "prod", "", true, true);
        EvalKit.dump("CI 金标套件", rows);
        List<String> failed = new ArrayList<>();
        for (EvalKit.Row r : rows) {
            if (!r.pass()) {
                failed.add(r.id);
            }
        }
        System.out.printf("%n门槛: 每条 Hit@1=1 且 正确且忠实且不泄漏。宏观 Hit@1=%.2f 正确率=%.2f%n",
                EvalKit.avg(rows, "hit1"), EvalKit.avg(rows, "correct"));
        if (!failed.isEmpty()) {
            System.out.println("CI RED: " + String.join(", ", failed));
            System.exit(1);
        }
        System.out.println("CI GREEN: 金标套件通过。改切块 / 过滤 / 融合后请再跑，不要只看感觉。");
    }

    private static EvalKit.GoldCase copy(EvalKit.GoldCase src) {
        EvalKit.GoldCase c = new EvalKit.GoldCase();
        c.id = src.id;
        c.query = src.query;
        c.gold_headings = src.gold_headings;
        c.forbidden_headings = src.forbidden_headings;
        c.must_contain = src.must_contain;
        c.must_not_contain = src.must_not_contain;
        c.role = src.role;
        return c;
    }
}
