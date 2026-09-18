package com.agentbook.ch11;

import java.util.List;
import java.util.Map;

public class PartitionAndIvf {
    static final String[][] DOCS = {
            {"linqiu", "linqiu-pref", "我喜欢窗口靠过道的工位"},
            {"linqiu", "linqiu-order", "ORDER-88 未发货，用户询问能否退款"},
            {"zhangsan", "zhang-pref", "张三喜欢靠墙的工位，不要过道"},
            {"lisi", "lisi-pref", "李四坐电梯口，要安静"},
            {"lisi", "lisi-note", "李四下周去上海出差"}
    };

    public static void main(String[] args) {
        double[] q = LocalHashEmbedding.embed("他工位喜欢靠哪边");
        MiniMilvus flat = fill("FLAT", 4, 2);
        searchAndDump(flat, q, null, null, "FLAT + 全分区");
        searchAndDump(flat, q, "linqiu", null, "FLAT + 只搜 partition=linqiu");

        MiniMilvus ivf = fill("IVF_FLAT", 4, 1);
        searchAndDump(ivf, q, null, 1, "IVF nprobe=1（可能漏掉真近邻）");
        searchAndDump(ivf, q, null, 4, "IVF nprobe=4（等于全桶，接近 FLAT）");

        System.out.println("\n=== 选型对照（教学口径，不是报价单）===");
        System.out.println("尺度     Chroma 嵌入式        Lite                  集群 Milvus / 云托管");
        System.out.println("分区     metadata 过滤        官方不支持分区         一等公民 Partition");
        System.out.println("索引     主要 HNSW            多 FLAT               IVF / HNSW / 磁盘索引");
        System.out.println("Java     HTTP 为主            仍要连服务            gRPC SDK 一等公民");
        System.out.println("\n读法：Chroma 把记忆跑起来；Milvus 把规模、分区和索引手刹交给你。");
    }

    private static MiniMilvus fill(String indexType, int nlist, int nprobe) {
        MiniMilvus db = new MiniMilvus();
        db.createCollection("agent_memory", "COSINE", indexType, nlist, nprobe);
        for (String[] d : DOCS) {
            db.createPartition("agent_memory", d[0]);
            db.insert("agent_memory", d[1], LocalHashEmbedding.embed(d[2]),
                    Map.of("user_id", d[0], "text", d[2]), d[0]);
        }
        db.flush("agent_memory");
        db.createIndex("agent_memory");
        db.load("agent_memory");
        return db;
    }

    private static void searchAndDump(MiniMilvus db, double[] q, String partition, Integer nprobe, String title) {
        MiniMilvus.Stats s = new MiniMilvus.Stats();
        dump(title, db.search("agent_memory", q, 3, partition, nprobe, s), s);
    }

    private static void dump(String title, List<MiniMilvus.Hit> hits, MiniMilvus.Stats stats) {
        if (hits == null) {
            return;
        }
        System.out.println("\n=== " + title + " ===");
        System.out.println("  compared=" + stats.compared + "  skipped_ivf=" + stats.skippedIvf
                + "  partitions=" + stats.partitions + "  nprobe=" + stats.nprobe);
        for (MiniMilvus.Hit hit : hits) {
            System.out.printf("  d=%.3f  %-16s  %s%n", hit.distance, hit.row.id, hit.row.fields.get("text"));
        }
    }
}
