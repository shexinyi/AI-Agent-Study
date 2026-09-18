package com.agentbook.ch11;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MiniMilvus {
    public static final class Row {
        public final String id;
        public final double[] vector;
        public final Map<String, Object> fields;
        public boolean flushed;
        public Integer bucket;

        Row(String id, double[] vector, Map<String, Object> fields) {
            this.id = id;
            this.vector = vector;
            this.fields = fields;
        }
    }

    public static final class Hit {
        public final double distance;
        public final Row row;

        Hit(double distance, Row row) {
            this.distance = distance;
            this.row = row;
        }
    }

    public static final class Stats {
        public int compared;
        public int skippedUnflushed;
        public int skippedIvf;
        public int partitions;
        public int nprobe;
    }

    public static final class Collection {
        public final String name;
        public final int dim;
        public final String metric;
        public final String indexType;
        public final int nlist;
        public int nprobe;
        public boolean loaded;
        public boolean indexed;
        public final Map<String, List<Row>> partitions = new LinkedHashMap<>();

        Collection(String name, int dim, String metric, String indexType, int nlist, int nprobe) {
            this.name = name;
            this.dim = dim;
            this.metric = metric;
            this.indexType = indexType;
            this.nlist = nlist;
            this.nprobe = nprobe;
            partitions.put("_default", new ArrayList<>());
        }
    }

    public final Map<String, Collection> collections = new LinkedHashMap<>();

    public Collection createCollection(String name, String metric, String indexType, int nlist, int nprobe) {
        Collection col = new Collection(name, LocalHashEmbedding.DIM, metric, indexType, nlist, nprobe);
        collections.put(name, col);
        return col;
    }

    public void createPartition(String name, String partition) {
        collections.get(name).partitions.putIfAbsent(partition, new ArrayList<>());
    }

    public void insert(String name, String id, double[] vector, Map<String, Object> fields, String partition) {
        Collection col = collections.get(name);
        if (vector.length != col.dim) {
            throw new IllegalArgumentException("维度不一致");
        }
        col.partitions.putIfAbsent(partition, new ArrayList<>());
        col.partitions.get(partition).removeIf(r -> r.id.equals(id));
        col.partitions.get(partition).add(new Row(id, vector, fields));
        col.indexed = false;
        col.loaded = false;
    }

    public int flush(String name) {
        int n = 0;
        for (List<Row> rows : collections.get(name).partitions.values()) {
            for (Row row : rows) {
                if (!row.flushed) {
                    row.flushed = true;
                    n++;
                }
            }
        }
        return n;
    }

    public void createIndex(String name) {
        Collection col = collections.get(name);
        for (List<Row> rows : col.partitions.values()) {
            for (Row row : rows) {
                if (row.flushed) {
                    row.bucket = ivfBucket(row.vector, col.nlist);
                }
            }
        }
        col.indexed = true;
    }

    public void load(String name) {
        Collection col = collections.get(name);
        if (!col.indexed) {
            throw new IllegalStateException("索引未建：先 createIndex，再 load（生产里未 load 的集合不能搜）");
        }
        col.loaded = true;
    }

    public List<Hit> search(String name, double[] vector, int topK, String partition, Integer nprobe) {
        return search(name, vector, topK, partition, nprobe, new Stats());
    }

    public List<Hit> search(String name, double[] vector, int topK, String partition, Integer nprobe, Stats stats) {
        Collection col = collections.get(name);
        if (!col.indexed) {
            throw new IllegalStateException("索引未建：先 createIndex。Lite/集群都不会在空索引上给你「刚好能用」的错觉。");
        }
        if (!col.loaded) {
            throw new IllegalStateException("集合未 load。Milvus 重启后也要重新 load，否则搜不到。");
        }
        List<String> parts;
        if (partition != null) {
            parts = List.of(partition);
        } else {
            parts = new ArrayList<>();
            col.partitions.forEach((p, rows) -> {
                if (!rows.isEmpty()) {
                    parts.add(p);
                }
            });
        }
        int probe = nprobe == null ? col.nprobe : nprobe;
        int qBucket = ivfBucket(vector, col.nlist);
        List<Row> candidates = new ArrayList<>();
        for (String part : parts) {
            for (Row row : col.partitions.getOrDefault(part, List.of())) {
                if (!row.flushed) {
                    stats.skippedUnflushed++;
                    continue;
                }
                if ("IVF_FLAT".equals(col.indexType) && row.bucket != null
                        && !inProbe(row.bucket, qBucket, probe, col.nlist)) {
                    stats.skippedIvf++;
                    continue;
                }
                candidates.add(row);
            }
        }
        stats.compared = candidates.size();
        stats.partitions = parts.size();
        stats.nprobe = probe;
        List<Hit> hits = new ArrayList<>();
        for (Row row : candidates) {
            hits.add(new Hit(distance(col.metric, vector, row.vector), row));
        }
        hits.sort(Comparator.comparingDouble(h -> h.distance));
        return hits.size() > topK ? hits.subList(0, topK) : hits;
    }

    public int count(String name) {
        return collections.get(name).partitions.values().stream().mapToInt(List::size).sum();
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            collections.forEach((name, col) -> root.set(name, toJson(mapper, col)));
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static MiniMilvus open(Path path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(path.toFile());
            MiniMilvus db = new MiniMilvus();
            root.fields().forEachRemaining(e -> db.collections.put(e.getKey(), fromJson(e.getValue())));
            db.collections.values().forEach(c -> c.loaded = false);
            return db;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static double distance(String metric, double[] a, double[] b) {
        if ("COSINE".equals(metric)) {
            return 1.0 - LocalHashEmbedding.dot(a, b);
        }
        if ("L2".equals(metric)) {
            double s = 0;
            for (int i = 0; i < a.length; i++) {
                double d = a[i] - b[i];
                s += d * d;
            }
            return s;
        }
        if ("IP".equals(metric)) {
            return -LocalHashEmbedding.dot(a, b);
        }
        throw new IllegalArgumentException(metric);
    }

    public static int ivfBucket(double[] vec, int nlist) {
        int idx = 0;
        double best = -1;
        for (int i = 0; i < vec.length; i++) {
            double a = Math.abs(vec[i]);
            if (a > best) {
                best = a;
                idx = i;
            }
        }
        return idx % nlist;
    }

    private static boolean inProbe(int bucket, int qBucket, int nprobe, int nlist) {
        for (int i = 0; i < nprobe; i++) {
            if (bucket == (qBucket + i) % nlist) {
                return true;
            }
        }
        return false;
    }

    private static ObjectNode toJson(ObjectMapper mapper, Collection col) {
        ObjectNode n = mapper.createObjectNode();
        n.put("dim", col.dim);
        n.put("metric", col.metric);
        n.put("indexType", col.indexType);
        n.put("nlist", col.nlist);
        n.put("nprobe", col.nprobe);
        n.put("indexed", col.indexed);
        ObjectNode parts = mapper.createObjectNode();
        col.partitions.forEach((p, rows) -> {
            ArrayNode arr = mapper.createArrayNode();
            for (Row r : rows) {
                ObjectNode rn = mapper.createObjectNode();
                rn.put("id", r.id);
                rn.set("vector", mapper.valueToTree(r.vector));
                rn.set("fields", mapper.valueToTree(r.fields));
                rn.put("flushed", r.flushed);
                if (r.bucket != null) {
                    rn.put("bucket", r.bucket);
                }
                arr.add(rn);
            }
            parts.set(p, arr);
        });
        n.set("partitions", parts);
        return n;
    }

    private static Collection fromJson(JsonNode n) {
        Collection col = new Collection(
                "tmp",
                n.get("dim").asInt(),
                n.get("metric").asText(),
                n.get("indexType").asText(),
                n.get("nlist").asInt(),
                n.get("nprobe").asInt()
        );
        col.indexed = n.get("indexed").asBoolean();
        col.partitions.clear();
        n.get("partitions").fields().forEachRemaining(e -> {
            List<Row> rows = new ArrayList<>();
            for (JsonNode rn : e.getValue()) {
                double[] vec = new ObjectMapper().convertValue(rn.get("vector"), double[].class);
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = new ObjectMapper().convertValue(rn.get("fields"), Map.class);
                Row row = new Row(rn.get("id").asText(), vec, fields);
                row.flushed = rn.get("flushed").asBoolean();
                if (rn.has("bucket")) {
                    row.bucket = rn.get("bucket").asInt();
                }
                rows.add(row);
            }
            col.partitions.put(e.getKey(), rows);
        });
        return col;
    }
}
