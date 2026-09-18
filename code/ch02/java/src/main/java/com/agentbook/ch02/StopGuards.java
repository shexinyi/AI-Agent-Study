package com.agentbook.ch02;

import java.util.List;
import java.util.Map;

/**
 * 第 2 章示例 3：用护栏挡住失控循环。
 *
 * mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.agentbook.ch02.StopGuards"
 */
public class StopGuards {
    public static void main(String[] args) throws Exception {
        runCase("案例 A：重复调用同一工具", new LoopingPlanner(), new StopPolicy(8, 5.0, 2));
        runCase("案例 B：超时（工具故意 sleep）", new SlowThenLoopPlanner(), new StopPolicy(20, 1.0, 99));
        runCase("案例 C：没有重复检测时，靠最大步数兜底", new LoopingPlanner(), new StopPolicy(3, 5.0, 99));
    }

    static void runCase(String title, Planner planner, StopPolicy policy) {
        System.out.println("=".repeat(60));
        System.out.println(title);
        ToolRegistry tools = new ToolRegistry();
        tools.register("check_leave_balance", args ->
                new Observation(true, Map.of("balance_ok", true, "leave_balance", 5),
                        args.get("user_id") + " 余额 5，申请 " + args.get("days") + " 天"));
        tools.register("sleepy_lookup", args -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new Observation(true, Map.of("profile_loaded", true), "已加载 " + args.get("user_id") + " 的档案");
        });
        Goal goal = new Goal("请假三天", "u1001", List.of("ticket_created", "manager_notified"), Map.of());
        RunResult result = new AgentRuntime(planner, tools, policy).run(goal);
        System.out.println("结果: " + result.status() + " / " + result.reason() + " / 步数=" + result.steps() + "\n");
    }

    static class LoopingPlanner implements Planner {
        @Override
        public Decision decide(AgentState state) {
            return Decision.callTool("我再确认一次余额", "check_leave_balance",
                    Map.of("user_id", state.goal.userId(), "days", 3));
        }
    }

    static class SlowThenLoopPlanner implements Planner {
        @Override
        public Decision decide(AgentState state) {
            return Decision.callTool(
                    state.slots.containsKey("profile_loaded") ? "再加载一次" : "先加载档案",
                    "sleepy_lookup",
                    Map.of("user_id", state.goal.userId())
            );
        }
    }
}
