package com.agentbook.ch15;

import java.nio.file.Path;
import java.util.List;

public class HybridRrf {
    public static void main(String[] args) {
        List<RetrieveKit.Chunk> chunks = RetrieveKit.buildCorpus(Path.of("kb"));
        String q = "ORDER-88 能不能退";
        System.out.println("问: " + q);
        var vec = RetrieveKit.vectorSearch(chunks, q, 4);
        var kw = RetrieveKit.bm25Search(chunks, q, 4);
        RetrieveKit.dump("向量", vec);
        RetrieveKit.dump("BM25", kw);
        RetrieveKit.dump("RRF 融合 k=60", RetrieveKit.rrf(List.of(vec, kw), 60, 3));
        System.out.println("\n读法：融合看名次。88 折若两路都靠前，RRF 仍会把它放第一——所以还要重排。");
    }
}
