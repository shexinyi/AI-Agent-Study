package com.agentbook.ch11;

public class FourNouns {
    public static void main(String[] args) {
        String a = "我喜欢窗口靠过道的工位";
        String b = "工位请安排在走廊一侧";
        double[] va = LocalHashEmbedding.embed(a);
        double[] vb = LocalHashEmbedding.embed(b);
        System.out.println("=== 一张生产集合的最低配置 ===");
        System.out.println("Collection : agent_memory");
        System.out.println("Partition  : tenant=acme / user=linqiu");
        System.out.println("Index      : FLAT | IVF_FLAT | HNSW");
        System.out.println("Metric     : COSINE | L2 | IP");
        System.out.println();
        System.out.println("两句座位话在三种度量下的距离（向量已 L2 归一化）：");
        for (String metric : new String[]{"COSINE", "L2", "IP"}) {
            System.out.printf("  %-7s  距离(越小越近)=%+.3f%n", metric, MiniMilvus.distance(metric, va, vb));
        }
        System.out.println();
        System.out.println("读法：同一对点，三种度量数字不同。集合用 COSINE 建成，查询却按 L2 理解，就是比例尺错了。");
        System.out.println("FLAT=全比；IVF_FLAT=先入桶再比 nprobe 个桶；HNSW=图索引。");
    }
}
