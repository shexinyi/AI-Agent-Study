package com.agentbook.ch11;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class WriteReadPath {
    static final String DOC = "我喜欢窗口靠过道的工位";

    public static void main(String[] args) {
        MiniMilvus db = new MiniMilvus();
        db.createCollection("agent_memory", "COSINE", "FLAT", 4, 2);
        db.insert("agent_memory", "linqiu-pref", LocalHashEmbedding.embed(DOC),
                Map.of("user_id", "linqiu", "text", DOC), "linqiu");
        System.out.println("insert 后条数=" + db.count("agent_memory") + "  但还在 growing segment");
        trySearch(db, "未 flush / 未 index / 未 load");

        int n = db.flush("agent_memory");
        System.out.println("\nflush " + n + " 条 → sealed。仍不能搜。");
        trySearch(db, "已 flush，未 index");

        db.createIndex("agent_memory");
        trySearch(db, "已建索引，未 load");

        db.load("agent_memory");
        trySearch(db, "load 之后");

        Path data = Path.of("data", "write_path.json");
        db.save(data);
        MiniMilvus db2 = MiniMilvus.open(data);
        System.out.println("\n模拟进程重启，从 " + data.getFileName() + " 打开。loaded 被清掉。");
        trySearch(db2, "重启后直接搜");
        db2.createIndex("agent_memory");
        db2.load("agent_memory");
        trySearch(db2, "重启后重新 index + load");
        System.out.println("\n读法：生产事故第一名不是算法，是忘了 load。");
    }

    private static void trySearch(MiniMilvus db, String title) {
        System.out.println("\n--- " + title + " ---");
        try {
            MiniMilvus.Stats stats = new MiniMilvus.Stats();
            List<MiniMilvus.Hit> hits = db.search("agent_memory",
                    LocalHashEmbedding.embed("他坐哪边靠过道"), 2, null, null, stats);
            if (hits.isEmpty()) {
                System.out.println("  命中 0  compared=" + stats.compared
                        + "  skipped_unflushed=" + stats.skippedUnflushed);
                return;
            }
            for (MiniMilvus.Hit hit : hits) {
                System.out.printf("  d=%.3f  %s  %s%n", hit.distance, hit.row.id, hit.row.fields.get("text"));
            }
            System.out.println("  compared=" + stats.compared + "  skipped_unflushed=" + stats.skippedUnflushed);
        } catch (RuntimeException exc) {
            System.out.println("  失败: " + exc.getMessage());
        }
    }
}
