package com.agentbook.ch10;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Java 调 Chroma 的常见方式是 HTTP，而不是嵌入式 PersistentClient。
 * 本类只打印请求形状，不发起网络调用。
 */
public class ChromaRestShapes {
    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        double[] embedding = LocalHashEmbedding.embed("我喜欢窗口靠过道的工位");
        ObjectNode add = mapper.createObjectNode();
        add.putPOJO("ids", List.of("linqiu:preference"));
        add.putPOJO("documents", List.of("preference=我喜欢窗口靠过道的工位"));
        add.putPOJO("embeddings", List.of(embedding));
        add.putPOJO("metadatas", List.of(mapper.createObjectNode()
                .put("user_id", "linqiu")
                .put("kind", "preference")));

        ObjectNode query = mapper.createObjectNode();
        query.putPOJO("query_embeddings", List.of(LocalHashEmbedding.embed("他坐哪边")));
        query.put("n_results", 3);
        query.set("where", mapper.createObjectNode().put("user_id", "linqiu"));

        System.out.println("POST /api/v2/tenants/default_tenant/databases/default_database/collections/{id}/add");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(add));
        System.out.println("\nPOST .../collections/{id}/query");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(query));
        System.out.println("\n读法：Java 进程算坐标，Chroma 服务管索引。路径以你部署的 Chroma 版本文档为准。");
    }
}
