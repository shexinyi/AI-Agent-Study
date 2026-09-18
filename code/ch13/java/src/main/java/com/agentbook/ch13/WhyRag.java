package com.agentbook.ch13;

import java.nio.file.Path;
import java.util.List;

public class WhyRag {
    static final String QUESTION = "事假最多几天？当天能申请吗？";

    public static void main(String[] args) {
        System.out.println("问: " + QUESTION);
        System.out.println("\n=== 闭卷（没有检索）===");
        System.out.println(RagKit.generateClosedBook(QUESTION));
        System.out.println("  ↑ 5 天、当天申请：都是编的，和制度相反。");
        List<RagKit.Chunk> chunks = RagKit.buildIndex(Path.of("kb"));
        List<RagKit.Hit> hits = RagKit.retrieve(chunks, QUESTION, 2);
        System.out.println("\n=== Top-2 检索 ===");
        hits.forEach(h -> System.out.printf("  %.3f  %s / %s%n", h.score, h.chunk.source, h.chunk.heading));
        System.out.println("\n=== RAG（只准引用命中块）===");
        System.out.println(RagKit.generateExtractive(QUESTION, hits, 3));
        System.out.println("\n读法：RAG 不是更聪明的模型，是先把书翻开再准许说话。");
    }
}
