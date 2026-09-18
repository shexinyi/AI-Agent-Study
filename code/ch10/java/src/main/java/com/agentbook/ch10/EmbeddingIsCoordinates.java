package com.agentbook.ch10;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EmbeddingIsCoordinates {
    static final String[] SENTENCES = {
            "我喜欢窗口靠过道的工位",
            "工位请安排在走廊一侧",
            "ORDER-88 还没发货，申请退款",
            "我的密码是 123456"
    };

    public static void main(String[] args) {
        System.out.println("教学嵌入维度: " + LocalHashEmbedding.DIM + "（哈希坐标，不是 OpenAI / MiniLM）\n");
        List<double[]> vectors = new ArrayList<>();
        for (String text : SENTENCES) {
            double[] vec = LocalHashEmbedding.embed(text);
            vectors.add(vec);
            System.out.println("文本: " + text);
            System.out.println("  词元: " + LocalHashEmbedding.tokens(text));
            System.out.println("  向量: " + preview(vec) + "\n");
        }
        String query = "他坐哪边比较靠过道？";
        double[] q = LocalHashEmbedding.embed(query);
        System.out.println("查询: " + query);
        List<String> ranked = new ArrayList<>(List.of(SENTENCES));
        ranked.sort(Comparator.comparingDouble(
                (String t) -> LocalHashEmbedding.cosine(q, LocalHashEmbedding.embed(t))).reversed());
        System.out.println("按余弦相似度从高到低：");
        for (String text : ranked) {
            System.out.printf("  %+.3f  %s%n", LocalHashEmbedding.cosine(q, LocalHashEmbedding.embed(text)), text);
        }
        System.out.println("\n读法：措辞不同但都在说座位的两句更近；订单和密码更远。");
    }

    private static String preview(double[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("%+.2f", vec[i]));
        }
        return sb.append(", ... ] dim=").append(vec.length).toString();
    }
}
