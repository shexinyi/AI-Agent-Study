package com.agentbook.ch03;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentState {
    public final Goal goal;
    public final Map<String, Object> slots = new LinkedHashMap<>();
    public final List<String> events = new ArrayList<>();
    public final List<String> toolTrace = new ArrayList<>();
    public int step = 0;
    public final long startedAtNanos = System.nanoTime();

    public AgentState(Goal goal) {
        this.goal = goal;
    }

    public void remember(String line) {
        events.add(line);
    }
}
