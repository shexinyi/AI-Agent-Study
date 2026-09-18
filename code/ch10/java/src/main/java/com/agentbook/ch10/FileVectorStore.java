package com.agentbook.ch10;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chroma 集合契约的 Java 落盘实现。生产里把这里换成 HttpClient 调 Chroma / Milvus。
 */
public final class FileVectorStore {
    public record Hit(String id, String document, Map<String, Object> metadata, double distance) {
    }

    private final Path path;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ObjectNode> rows = new LinkedHashMap<>();

    public FileVectorStore(Path path) {
        this.path = path;
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                JsonNode root = mapper.readTree(path.toFile());
                JsonNode stored = root.get("rows");
                if (stored != null && stored.isObject()) {
                    Iterator<String> names = stored.fieldNames();
                    while (names.hasNext()) {
                        String id = names.next();
                        rows.put(id, (ObjectNode) stored.get(id));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void upsert(String id, String document, Map<String, Object> metadata) {
        ObjectNode row = mapper.createObjectNode();
        row.put("id", id);
        row.put("document", document);
        row.set("metadata", mapper.valueToTree(metadata));
        row.set("embedding", mapper.valueToTree(LocalHashEmbedding.embed(document)));
        rows.put(id, row);
        flush();
    }

    public List<Hit> query(String text, int nResults, Map<String, Object> where) {
        double[] q = LocalHashEmbedding.embed(text);
        List<Hit> hits = new ArrayList<>();
        for (ObjectNode row : rows.values()) {
            Map<String, Object> meta = mapper.convertValue(row.get("metadata"), Map.class);
            if (!match(meta, where)) {
                continue;
            }
            double[] emb = mapper.convertValue(row.get("embedding"), double[].class);
            hits.add(new Hit(row.get("id").asText(), row.get("document").asText(), meta,
                    LocalHashEmbedding.cosineDistance(q, emb)));
        }
        hits.sort(Comparator.comparingDouble(Hit::distance));
        return hits.size() > nResults ? hits.subList(0, nResults) : hits;
    }

    public void delete(String id) {
        rows.remove(id);
        flush();
    }

    public int count() {
        return rows.size();
    }

    private static boolean match(Map<String, Object> meta, Map<String, Object> where) {
        if (where == null || where.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> e : where.entrySet()) {
            Object got = meta.get(e.getKey());
            if (got == null || !String.valueOf(got).equals(String.valueOf(e.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private void flush() {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode stored = mapper.createObjectNode();
            rows.forEach(stored::set);
            root.set("rows", stored);
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
