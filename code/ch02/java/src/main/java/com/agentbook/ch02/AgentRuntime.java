package com.agentbook.ch02;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class AgentRuntime {
    private final Planner planner;
    private final ToolRegistry tools;
    private final StopPolicy policy;

    public AgentRuntime(Planner planner, ToolRegistry tools, StopPolicy policy) {
        this.planner = planner;
        this.tools = tools;
        this.policy = policy;
    }

    public RunResult run(Goal goal) {
        AgentState state = new AgentState(goal);
        state.remember("接收目标: " + goal.description());

        while (true) {
            RunResult guard = checkGuards(state);
            if (guard != null) {
                return guard;
            }

            state.step++;
            Decision decision = planner.decide(state);
            state.remember("第 " + state.step + " 步思考: " + decision.kind() + " / " + decision.reason());
            System.out.println("第 " + state.step + " 步 -> " + decision.kind() + ": " + decision.reason());

            switch (decision.kind()) {
                case FINISH -> {
                    if (goal.isSatisfied(state.slots)) {
                        return result(RunResult.Status.SUCCESS, decision.reason(), state);
                    }
                    return result(RunResult.Status.FAILED, "思考器宣布完成，但成功条件未满足", state);
                }
                case FAIL -> {
                    return result(RunResult.Status.FAILED, decision.reason(), state);
                }
                case ASK_HUMAN -> {
                    return result(RunResult.Status.NEED_HUMAN, decision.reason(), state);
                }
                case CALL_TOOL -> {
                    state.toolTrace.add(decision.toolName());
                    System.out.println("  调用 " + decision.toolName() + decision.toolArgs());
                    Observation observation = tools.invoke(decision.toolName(), decision.toolArgs());
                    state.remember("观察[" + decision.toolName() + "]: " + observation.message());
                    System.out.println("  观察: " + observation.message());
                    mergeSlots(state, observation);
                }
            }
        }
    }

    private RunResult checkGuards(AgentState state) {
        double elapsed = (System.nanoTime() - state.startedAtNanos) / 1_000_000_000.0;
        if (elapsed > policy.timeoutSeconds()) {
            return result(RunResult.Status.TIMEOUT, "超过 " + policy.timeoutSeconds() + "s", state);
        }
        if (state.step >= policy.maxSteps()) {
            return result(RunResult.Status.MAX_STEPS, "达到最大步数 " + policy.maxSteps(), state);
        }
        if (sameToolStreak(state) >= policy.maxSameToolInARow()) {
            String last = state.toolTrace.get(state.toolTrace.size() - 1);
            return result(RunResult.Status.REPEATED_TOOL, "连续重复调用 " + last, state);
        }
        return null;
    }

    private static int sameToolStreak(AgentState state) {
        if (state.toolTrace.isEmpty()) {
            return 0;
        }
        String last = state.toolTrace.get(state.toolTrace.size() - 1);
        int streak = 0;
        for (int i = state.toolTrace.size() - 1; i >= 0; i--) {
            if (!state.toolTrace.get(i).equals(last)) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private static void mergeSlots(AgentState state, Observation observation) {
        if (!observation.ok()) {
            state.slots.put("last_error", observation.message());
            return;
        }
        state.slots.remove("last_error");
        state.slots.putAll(observation.data());
    }

    private static RunResult result(RunResult.Status status, String reason, AgentState state) {
        return new RunResult(status, reason, new LinkedHashMap<>(state.slots), new ArrayList<>(state.events), state.step);
    }
}
