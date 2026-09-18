package com.agentbook.ch18;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 同构教具：State / Node / Edge / 条件跳转 / 按 thread_id 的检查点。
 * 不是 LangGraph4j。用来对照第 12 章手写状态机和第 18 章 Python 图。
 */
public final class MiniGraph {
    public static final class Interrupt extends RuntimeException {
        public final Object value;

        public Interrupt(Object value) {
            super(String.valueOf(value));
            this.value = value;
        }
    }

    public static final class Result {
        public final Map<String, Object> state;
        public final Object interrupt;

        Result(Map<String, Object> state, Object interrupt) {
            this.state = state;
            this.interrupt = interrupt;
        }

        public boolean paused() {
            return interrupt != None.NONE && interrupt != null;
        }
    }

    private enum None {NONE}

    private final Map<String, BiFunction<Map<String, Object>, Object, Map<String, Object>>> nodes = new LinkedHashMap<>();
    private final Map<String, String> edges = new HashMap<>();
    private final Map<String, Function<Map<String, Object>, String>> conds = new HashMap<>();
    private final Map<String, Map<String, String>> condMaps = new HashMap<>();
    private final Map<String, Thread> threads = new HashMap<>();
    private String entry = "start";

    public MiniGraph addNode(String name, BiFunction<Map<String, Object>, Object, Map<String, Object>> fn) {
        nodes.put(name, fn);
        return this;
    }

    public MiniGraph addEdge(String src, String dst) {
        edges.put(src, dst);
        if ("start".equals(src)) {
            entry = dst;
        }
        return this;
    }

    public MiniGraph addCond(String src, Function<Map<String, Object>, String> pred, Map<String, String> mapping) {
        conds.put(src, pred);
        condMaps.put(src, mapping);
        return this;
    }

    public Result invoke(Map<String, Object> seed, String threadId) {
        return invoke(seed, threadId, None.NONE);
    }

    public Result invoke(Map<String, Object> seed, String threadId, Object resume) {
        Thread th = threads.get(threadId);
        if (th == null) {
            th = new Thread(copy(seed), entry);
            threads.put(threadId, th);
        }
        Object token = resume;
        while (th.nxt != null && !"end".equals(th.nxt)) {
            try {
                Map<String, Object> delta = nodes.get(th.nxt).apply(th.state, token == None.NONE ? null : token);
                token = None.NONE;
                merge(th.state, delta);
            } catch (Interrupt hit) {
                return new Result(copy(th.state), hit.value);
            }
            if (conds.containsKey(th.nxt)) {
                th.nxt = condMaps.get(th.nxt).get(conds.get(th.nxt).apply(th.state));
            } else {
                th.nxt = edges.getOrDefault(th.nxt, "end");
            }
        }
        return new Result(copy(th.state), null);
    }

    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> state, Map<String, Object> delta) {
        for (var e : delta.entrySet()) {
            if ("trace".equals(e.getKey()) && e.getValue() instanceof List<?> add) {
                List<String> cur = (List<String>) state.computeIfAbsent("trace", k -> new ArrayList<String>());
                for (Object item : add) {
                    cur.add(String.valueOf(item));
                }
            } else {
                state.put(e.getKey(), e.getValue());
            }
        }
    }

    private static Map<String, Object> copy(Map<String, Object> src) {
        Map<String, Object> out = new LinkedHashMap<>(src);
        Object t = src.get("trace");
        if (t instanceof List<?> list) {
            List<String> copy = new ArrayList<>();
            for (Object item : list) {
                copy.add(String.valueOf(item));
            }
            out.put("trace", copy);
        }
        return out;
    }

    private static final class Thread {
        final Map<String, Object> state;
        String nxt;

        Thread(Map<String, Object> state, String nxt) {
            this.state = state;
            this.nxt = nxt;
        }
    }
}
