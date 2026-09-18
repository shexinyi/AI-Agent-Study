package com.agentbook.ch13;

import java.nio.file.Path;
import java.util.List;

public class OfflineOnline {
    public static void main(String[] args) {
        System.out.println("=== 离线索引流 ===");
        List<RagKit.Chunk> chunks = RagKit.buildIndex(Path.of("kb"));
        Path index = Path.of("data", "index.json");
        RagKit.saveIndex(chunks, index);
        System.out.println("扫描 kb → " + chunks.size() + " 块 → " + index.getFileName());
        chunks.forEach(c -> System.out.printf("  %-20s %s / %s%n", c.chunkId, c.source, c.heading));

        System.out.println("\n=== 在线问答流（重新 loadIndex）===");
        List<RagKit.Chunk> loaded = RagKit.loadIndex(index);
        for (String q : List.of("未发货能不能退", "靠过道工位怎么申请", "明年国庆放几天")) {
            List<RagKit.Hit> hits = RagKit.retrieve(loaded, q, 2);
            System.out.println("\n问: " + q);
            System.out.print("  命中: ");
            System.out.println(String.join(", ", hits.stream()
                    .map(h -> String.format("%s(%.2f)", h.chunk.heading, h.score)).toList()));
            System.out.println(RagKit.generateExtractive(q, hits, 3));
        }
        System.out.println("\n读法：国庆不在三份制度里，生成器必须说不足。");
    }
}
