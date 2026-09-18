package com.agentbook.ch01;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 第 1 章示例 1 的 Java 版：不调用大模型的规则 Agent。
 *
 * 运行（在 code/ch01/java 目录）：
 *   mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.agentbook.ch01.RuleAgent
 *
 * 若未配置 exec 插件，也可：
 *   mvn -q -DskipTests compile
 *   java -cp target/classes com.agentbook.ch01.RuleAgent
 */
public class RuleAgent {

    public static void main(String[] args) {
        AgentState state = runAgent("预订明天去上海的机票，并安排接机", 800);
        System.out.println("最终记忆:");
        state.memory.forEach(line -> System.out.println("- " + line));
    }

    static AgentState runAgent(String goal, int budgetCny) {
        AgentState state = new AgentState(goal, budgetCny);
        System.out.println("目标: " + state.goal);
        System.out.println("预算: " + state.budgetCny + " 元\n");

        while (!state.finished && state.step < state.maxSteps) {
            state.step++;
            Map<String, Object> decision = think(state);
            System.out.println("第 " + state.step + " 步思考 -> " + decision);

            if ("finish".equals(decision.get("type"))) {
                state.finished = true;
                String reason = String.valueOf(decision.get("reason"));
                state.memory.add("结束: " + reason);
                System.out.println("结束: " + reason);
                break;
            }

            String toolName = String.valueOf(decision.get("name"));
            @SuppressWarnings("unchecked")
            Map<String, Object> toolArgs = (Map<String, Object>) decision.get("args");
            Observation observation = execute(toolName, toolArgs);
            state.memory.add("观察[" + toolName + "]: " + observation.message());
            System.out.println("行动: " + toolName + toolArgs);
            System.out.println("观察: " + observation.message() + "\n");

            if ("book_flight".equals(toolName) && observation.ok()) {
                state.bookedFlight = observation.data();
            }
            if ("book_transfer".equals(toolName) && observation.ok()) {
                state.bookedTransfer = observation.data();
            }
        }

        if (!state.finished) {
            System.out.println("触发终止条件: 达到最大步数，避免死循环。");
        }
        return state;
    }

    static Map<String, Object> think(AgentState state) {
        if (state.bookedFlight != null && state.bookedTransfer != null) {
            return Map.of("type", "finish", "reason", "机票和接机都已办妥");
        }
        if (state.bookedFlight != null) {
            return Map.of(
                    "type", "tool",
                    "name", "book_transfer",
                    "args", Map.of("flight_no", state.bookedFlight.get("flight_no"))
            );
        }
        boolean searched = state.memory.stream().anyMatch(item -> item.startsWith("观察[search_flights]"));
        if (!searched) {
            return Map.of(
                    "type", "tool",
                    "name", "search_flights",
                    "args", Map.of("date", "2026-09-14", "dest", "SHA")
            );
        }
        return Map.of(
                "type", "tool",
                "name", "book_flight",
                "args", Map.of("flight_no", "MU5101", "budget_cny", state.budgetCny)
        );
    }

    static Observation execute(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "search_flights" -> searchFlights(String.valueOf(args.get("date")), String.valueOf(args.get("dest")));
            case "book_flight" -> bookFlight(String.valueOf(args.get("flight_no")), (Integer) args.get("budget_cny"));
            case "book_transfer" -> bookTransfer(String.valueOf(args.get("flight_no")));
            default -> new Observation(false, Map.of(), "未知工具: " + toolName);
        };
    }

    static Observation searchFlights(String date, String dest) {
        List<Map<String, Object>> catalog = List.of(
                flight("MU5101", date, dest, 680, 4),
                flight("CA1831", date, dest, 920, 1),
                flight("9C8801", date, dest, 450, 0)
        );
        List<Map<String, Object>> available = catalog.stream()
                .filter(item -> ((Integer) item.get("seats")) > 0)
                .toList();
        return new Observation(true, Map.of("flights", available), "找到 " + available.size() + " 个有票航班");
    }

    static Observation bookFlight(String flightNo, int budgetCny) {
        Map<String, Integer> prices = Map.of("MU5101", 680, "CA1831", 920, "9C8801", 450);
        if (!prices.containsKey(flightNo)) {
            return new Observation(false, Map.of(), "航班不存在: " + flightNo);
        }
        if ("9C8801".equals(flightNo)) {
            return new Observation(false, Map.of(), "该航班已售罄");
        }
        int price = prices.get(flightNo);
        if (price > budgetCny) {
            return new Observation(false, Map.of(), "价格 " + price + " 超出预算 " + budgetCny);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("flight_no", flightNo);
        data.put("price", price);
        return new Observation(true, data, "已预订 " + flightNo + "，花费 " + price + " 元");
    }

    static Observation bookTransfer(String flightNo) {
        if (flightNo == null || flightNo.isBlank()) {
            return new Observation(false, Map.of(), "没有航班信息，无法安排接机");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("car", "沪A·D328");
        data.put("pickup", "虹桥T2");
        data.put("flight_no", flightNo);
        return new Observation(true, data, "已为 " + flightNo + " 安排接机：沪A·D328，虹桥T2");
    }

    private static Map<String, Object> flight(String no, String date, String dest, int price, int seats) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("flight_no", no);
        item.put("date", date);
        item.put("dest", dest);
        item.put("price", price);
        item.put("seats", seats);
        return item;
    }
}
