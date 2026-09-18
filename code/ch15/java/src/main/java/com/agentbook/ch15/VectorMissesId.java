package com.agentbook.ch15;

import java.nio.file.Path;
import java.util.List;

public class VectorMissesId {
    public static void main(String[] args) {
        List<RetrieveKit.Chunk> chunks = RetrieveKit.buildCorpus(Path.of("kb"));
        System.out.println("向量通道丢掉 order/tz/纯数字，模拟稠密模型把稀有编号当噪声。");
        for (String q : List.of("ORDER-88 能不能退", "TZ123 是哪张单")) {
            System.out.println("\n问: " + q);
            RetrieveKit.dump("向量 Top-3", RetrieveKit.vectorSearch(chunks, q, 3));
            RetrieveKit.dump("BM25 Top-3", RetrieveKit.bm25Search(chunks, q, 3));
        }
        System.out.println("\n读法：专有名词和单号是关键词的主场。");
    }
}
