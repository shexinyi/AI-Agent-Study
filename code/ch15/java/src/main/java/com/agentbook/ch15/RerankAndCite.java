package com.agentbook.ch15;

import java.nio.file.Path;
import java.util.List;

public class RerankAndCite {
    public static void main(String[] args) {
        List<RetrieveKit.Chunk> chunks = RetrieveKit.buildCorpus(Path.of("kb"));
        for (String q : List.of("ORDER-88 能不能退", "TZ123 是哪张单", "事假上限几天")) {
            System.out.println("\n######## " + q + " ########");
            var pool = RetrieveKit.rrf(
                    List.of(RetrieveKit.vectorSearch(chunks, q, 4), RetrieveKit.bm25Search(chunks, q, 4)),
                    60, 5);
            RetrieveKit.dump("融合池（重排前）", pool);
            var ranked = RetrieveKit.rerank(q, pool);
            RetrieveKit.dump("教学重排后", ranked);
            System.out.println();
            System.out.println(RetrieveKit.answer(q, ranked.subList(0, Math.min(2, ranked.size()))));
        }
        System.out.println("\n读法：重排是第二道闸。每句事实必须能指回 [n]。");
    }
}
