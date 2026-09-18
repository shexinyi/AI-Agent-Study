package com.agentbook.ch16;

import java.nio.file.Path;
import java.util.List;

public class RetrievalMetrics {
    public static void main(String[] args) {
        EvalKit.GoldFile gold = EvalKit.load(Path.of("goldenset.json"));
        List<RagKit.Chunk> chunks = RagKit.buildCorpus(Path.of("kb"), "heading");
        System.out.println("金标写在 goldenset.json，不写在检索函数里。");
        System.out.println("Hit@K = 金标块是否进 Top-K；MRR = 第一块金标的名次倒数。");
        EvalKit.dump("只向量（丢掉编号词项）", EvalKit.suite(gold, chunks, "vector", "", true, true));
        EvalKit.dump("混合召回 + 教学重排", EvalKit.suite(gold, chunks, "prod", "", true, true));
        System.out.println("\n读法：Hit@3 会骗人——ORDER-88 在向量里是第 3，Hit@3=1，但 Top-1 是 88 折。看 Hit@1 和 MRR。");
    }
}
