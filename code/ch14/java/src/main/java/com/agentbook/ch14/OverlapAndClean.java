package com.agentbook.ch14;

import java.util.ArrayList;
import java.util.List;

public class OverlapAndClean {
    static final String QUERY = "累计不超过几个工作日";

    public static void main(String[] args) {
        String raw = ChunkKit.readKb();
        System.out.println("问: " + QUERY);
        report("未清洗", raw, 36, 0);
        String cleaned = ChunkKit.clean(raw);
        report("已清洗、无重叠", cleaned, 36, 0);
        report("已清洗、重叠 18", cleaned, 36, 18);
        System.out.println("\n读法：页眉和 MAX_LEAVE=99 会变成很近的垃圾。重叠让跨窗句子完整。");
    }

    static void report(String title, String text, int size, int overlap) {
        List<ChunkKit.Chunk> chunks = ChunkKit.window(text, size, overlap);
        System.out.println("\n=== " + title + "  size=" + size + " overlap=" + overlap + " 块数=" + chunks.size() + " ===");
        for (ChunkKit.Hit hit : ChunkKit.retrieve(chunks, QUERY, 2, false)) {
            List<String> flag = new ArrayList<>();
            if (hit.chunk.text.contains("10 个工作日")) {
                flag.add("GOLD");
            }
            if (hit.chunk.text.contains("99")) {
                flag.add("CODE99");
            }
            if (hit.chunk.text.contains("机密") || hit.chunk.text.contains("作废")) {
                flag.add("HEADER");
            }
            if (hit.chunk.text.contains("5 天")) {
                flag.add("OLD5");
            }
            System.out.printf("  %.3f  %s  %s  %s%n", hit.score, hit.chunk.id,
                    flag.isEmpty() ? "-" : String.join(",", flag), hit.chunk.preview(40));
        }
    }
}
