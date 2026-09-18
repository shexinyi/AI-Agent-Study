package com.agentbook.ch01;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentState {
    public final String goal;
    public final int budgetCny;
    public int step = 0;
    public final int maxSteps = 8;
    public final List<String> memory = new ArrayList<>();
    public Map<String, Object> bookedFlight;
    public Map<String, Object> bookedTransfer;
    public boolean finished = false;

    public AgentState(String goal, int budgetCny) {
        this.goal = goal;
        this.budgetCny = budgetCny;
    }
}
