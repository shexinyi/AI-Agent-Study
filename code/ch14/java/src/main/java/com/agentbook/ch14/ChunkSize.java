package com.agentbook.ch14;

import java.util.List;

public class ChunkSize {
    static final String QUERY = "事假每年最多几天";

    public static void main(String[] args) {
        String doc = ChunkKit.clean(ChunkKit.readKb());
        System.out.println("问: " + QUERY);
        report("整篇一块（太粗）", ChunkKit.whole(doc));
        report("固定窗 28 字 / 重叠 0（太细）", ChunkKit.window(doc, 28, 0));
        report("按 ## 标题", ChunkKit.byHeading(doc));
        System.out.println("\n读法：太粗捆住废止条款；太细切飞 10 个工作日。");
    }

    static void report(String name, List<ChunkKit.Chunk> chunks) {
        ChunkKit.Hit hit = ChunkKit.retrieve(chunks, QUERY, 1, false).get(0);
        boolean gold = hit.chunk.text.contains("10 个工作日");
        boolean poison = hit.chunk.text.contains("5 天") || hit.chunk.text.contains("99");
        System.out.println("\n=== " + name + "  块数=" + chunks.size() + " ===");
        System.out.printf("  Top-1 sim=%.3f  %s%n", hit.score, hit.chunk.id);
        System.out.println("  预览: " + hit.chunk.preview(48));
        System.out.println("  含「10 个工作日」? " + gold + "   掺进 5天/99? " + poison);
    }
}
