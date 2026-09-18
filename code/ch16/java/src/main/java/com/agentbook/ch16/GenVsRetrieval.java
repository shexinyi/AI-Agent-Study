package com.agentbook.ch16;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenVsRetrieval {
    public static void main(String[] args) {
        EvalKit.GoldFile gold = EvalKit.load(Path.of("goldenset.json"));
        List<RagKit.Chunk> chunks = RagKit.buildCorpus(Path.of("kb"), "heading");
        Map<String, EvalKit.GoldCase> cases = gold.cases.stream().collect(Collectors.toMap(c -> c.id, c -> c));

        show("Hit@3 过了，Top-1 错了（只向量问 ORDER-88）",
                EvalKit.eval(cases.get("order88"), chunks, "vector", gold.k, "", true, true));
        show("检索对了，却塞进无出处的一句",
                EvalKit.eval(cases.get("leave_days"), chunks, "prod", gold.k, "通常三天到账。", true, true));
        show("检索对、抽取也对",
                EvalKit.eval(cases.get("leave_days"), chunks, "prod", gold.k, "", true, true));

        System.out.println("\n读法：两列分数要分开挂。Hit@3 过了但 Hit@1 挂 = 抄了错的第一块。");
        System.out.println("正确率过、忠实度挂 = 数字碰巧对，但多编了资料里没有的句子。");
    }

    private static void show(String title, EvalKit.Row row) {
        System.out.println("\n######## " + title + " ########");
        System.out.printf("检索 Hit@1=%.0f  Hit@3=%.0f  MRR=%.2f  泄漏=%s%n",
                row.hit1, row.hitK, row.mrr, row.leak ? "是" : "否");
        System.out.printf("生成 正确=%s  忠实=%s%n", row.correct, row.faithful);
        System.out.println("Top: " + row.headings);
        System.out.println("答: " + row.answer);
    }
}
