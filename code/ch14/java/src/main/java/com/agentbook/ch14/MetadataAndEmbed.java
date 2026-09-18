package com.agentbook.ch14;

import java.util.ArrayList;
import java.util.List;

public class MetadataAndEmbed {
    public static void main(String[] args) {
        List<ChunkKit.Chunk> chunks = withMeta();
        System.out.println("=== 块元数据 ===");
        for (ChunkKit.Chunk ch : chunks) {
            System.out.printf("  %-8s acl=%-3s ver=%-7s kind=%-10s %s%n",
                    ch.id, ch.meta.get("acl"), ch.meta.get("version"), ch.meta.get("kind"), ch.meta.get("heading"));
        }
        String q = "事假上限几天";
        System.out.println("\n问: " + q);
        System.out.println("实习生（acl=all）Top-1: " + ChunkKit.retrieveFiltered(chunks, q, "acl", "all").get(0).chunk.preview(42));
        System.out.println("HR（acl=hr）Top-1:     " + ChunkKit.retrieveFiltered(chunks, q, "acl", "hr").get(0).chunk.preview(42));
        System.out.println("只要 2026-03:           " + ChunkKit.retrieveFiltered(chunks, q, "version", "2026-03").get(0).chunk.preview(42));

        String mixed = "personal leave 上限几天";
        System.out.println("\n中英混合问: " + mixed);
        ChunkKit.Hit full = ChunkKit.retrieve(chunks, mixed, 1, false).get(0);
        List<ChunkKit.Chunk> ascii = reembed(chunks, true);
        ChunkKit.Hit bad = ChunkKit.retrieve(ascii, mixed, 1, true).get(0);
        System.out.printf("  全语种编目  sim=%.3f  %s%n", full.score, full.chunk.preview(36));
        System.out.printf("  仅 ASCII    sim=%.3f  %s%n", bad.score, bad.chunk.preview(36));
        System.out.println("\n读法：权限和版本是过滤条件。中文制度用只吃英文的模型等于瞎编目。");
    }

    static List<ChunkKit.Chunk> withMeta() {
        List<ChunkKit.Chunk> chunks = ChunkKit.byHeading(ChunkKit.clean(ChunkKit.readKb()));
        for (ChunkKit.Chunk ch : chunks) {
            String heading = ch.meta.getOrDefault("heading", "");
            ch.meta.put("kind", heading.contains("历史") ? "deprecated" : heading.contains("对照") ? "table" : "policy");
            ch.meta.put("acl", heading.contains("现行") || heading.contains("对照") ? "hr" : "all");
            ch.meta.put("version", heading.contains("历史") ? "2024" : "2026-03");
        }
        return chunks;
    }

    static List<ChunkKit.Chunk> reembed(List<ChunkKit.Chunk> chunks, boolean asciiOnly) {
        List<ChunkKit.Chunk> out = new ArrayList<>();
        for (ChunkKit.Chunk ch : chunks) {
            ChunkKit.Chunk n = new ChunkKit.Chunk(ch.id, ch.text, ChunkKit.embed(ch.text, asciiOnly));
            n.meta.putAll(ch.meta);
            out.add(n);
        }
        return out;
    }
}
