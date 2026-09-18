package com.agentbook.ch11;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Java 调生产 Milvus 走 gRPC SDK（io.milvus / milvus-sdk-java）。
 * 本类只打印等价请求形状，不发起网络调用。
 */
public class MilvusRequestShapes {
    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        double[] vec = LocalHashEmbedding.embed("我喜欢窗口靠过道的工位");

        ObjectNode create = mapper.createObjectNode();
        create.put("collectionName", "agent_memory");
        create.put("dimension", LocalHashEmbedding.DIM);
        create.put("metricType", "COSINE");
        create.put("primaryField", "id");
        create.put("vectorField", "vector");
        create.put("indexType", "HNSW");
        create.put("partitionKey", "user_id");

        ObjectNode insert = mapper.createObjectNode();
        insert.putPOJO("ids", List.of("linqiu-pref"));
        insert.putPOJO("vectors", List.of(vec));
        insert.put("partitionName", "linqiu");
        insert.putPOJO("fields", List.of(mapper.createObjectNode()
                .put("user_id", "linqiu")
                .put("text", "我喜欢窗口靠过道的工位")));

        ObjectNode search = mapper.createObjectNode();
        search.putPOJO("vectors", List.of(LocalHashEmbedding.embed("他坐哪边")));
        search.put("topK", 3);
        search.put("metricType", "COSINE");
        search.put("nprobe", 8);
        search.put("partitionName", "linqiu");
        search.put("filter", "user_id == \"linqiu\"");
        search.put("consistencyLevel", "Bounded");

        System.out.println("createCollection 示意：");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(create));
        System.out.println("\ninsert 示意：");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(insert));
        System.out.println("\nsearch 示意：");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(search));
        System.out.println("\n读法：Java 用 milvus-sdk-java 发 gRPC；URI 形如 http://localhost:19530。");
        System.out.println("Lite 是 Python 嵌入式文件库。生产 Java 连的是 Standalone 或集群，不是 PersistentClient。");
    }
}
