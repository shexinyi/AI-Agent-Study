package com.agentbook.ch02;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ToolRegistry {
    private final Map<String, Function<Map<String, Object>, Observation>> tools = new LinkedHashMap<>();

    public void register(String name, Function<Map<String, Object>, Observation> fn) {
        tools.put(name, fn);
    }

    public Observation invoke(String name, Map<String, Object> args) {
        Function<Map<String, Object>, Observation> fn = tools.get(name);
        if (fn == null) {
            return new Observation(false, Map.of(), "未知工具: " + name);
        }
        return fn.apply(args);
    }
}
