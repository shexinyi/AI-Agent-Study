package com.agentbook.ch02;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第 2 章示例 2：用 Runtime 办理请假单。
 *
 * mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.agentbook.ch02.LeaveAgent"
 */
public class LeaveAgent {
    static final Map<String, Map<String, Object>> HR_DB = new HashMap<>();

    static {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "林秋");
        user.put("leave_balance", 5);
        user.put("tickets", new ArrayList<String>());
        user.put("manager", "m2001");
        HR_DB.put("u1001", user);
    }

    public static void main(String[] args) {
        AgentRuntime runtime = buildRuntime();
        Goal goal = new Goal(
                "我是林秋，下周一到周三请假去上海办事，帮我走流程。",
                "u1001",
                List.of("ticket_created", "manager_notified"),
                Map.of("max_days_without_approval", 3)
        );
        RunResult result = runtime.run(goal);
        System.out.println("\n状态: " + result.status());
        System.out.println("原因: " + result.reason());
        System.out.println("槽位: " + result.slots());
        System.out.println("HR 工单: " + HR_DB.get("u1001").get("tickets")
                + "，余额 " + HR_DB.get("u1001").get("leave_balance"));
    }

    static AgentRuntime buildRuntime() {
        ToolRegistry tools = new ToolRegistry();
        tools.register("parse_leave_request", LeaveAgent::parseLeaveRequest);
        tools.register("check_leave_balance", LeaveAgent::checkLeaveBalance);
        tools.register("create_leave_ticket", LeaveAgent::createLeaveTicket);
        tools.register("notify_manager", LeaveAgent::notifyManager);
        return new AgentRuntime(new LeavePlanner(), tools, new StopPolicy(8, 3.0, 2));
    }

    static Observation parseLeaveRequest(Map<String, Object> args) {
        String userText = String.valueOf(args.get("user_text"));
        if (userText.contains("周一") && userText.contains("周三")) {
            return new Observation(true, Map.of(
                    "parsed", true,
                    "start_date", "2026-09-14",
                    "end_date", "2026-09-16",
                    "days", 3,
                    "leave_type", "事假"
            ), "解析成功：2026-09-14 至 2026-09-16，共 3 天事假");
        }
        return new Observation(false, Map.of("parsed", false), "无法解析请假日期，需要用户补充");
    }

    static Observation checkLeaveBalance(Map<String, Object> args) {
        String userId = String.valueOf(args.get("user_id"));
        int days = (Integer) args.get("days");
        Map<String, Object> user = HR_DB.get(userId);
        if (user == null) {
            return new Observation(false, Map.of(), "用户不存在: " + userId);
        }
        int balance = (Integer) user.get("leave_balance");
        boolean enough = balance >= days;
        return new Observation(
                true,
                Map.of("balance_ok", enough, "leave_balance", balance),
                "剩余 " + balance + " 天，申请 " + days + " 天，" + (enough ? "通过" : "不足")
        );
    }

    static Observation createLeaveTicket(Map<String, Object> args) {
        String userId = String.valueOf(args.get("user_id"));
        String startDate = String.valueOf(args.get("start_date"));
        int days = (Integer) args.get("days");
        Map<String, Object> user = HR_DB.get(userId);
        String ticketId = "L-" + startDate.replace("-", "");
        @SuppressWarnings("unchecked")
        List<String> tickets = (List<String>) user.get("tickets");
        tickets.add(ticketId);
        user.put("leave_balance", (Integer) user.get("leave_balance") - days);
        return new Observation(
                true,
                Map.of("ticket_created", true, "ticket_id", ticketId),
                "已创建工单 " + ticketId + "，余额变为 " + user.get("leave_balance")
        );
    }

    static Observation notifyManager(Map<String, Object> args) {
        String userId = String.valueOf(args.get("user_id"));
        String ticketId = String.valueOf(args.get("ticket_id"));
        String manager = String.valueOf(HR_DB.get(userId).get("manager"));
        return new Observation(
                true,
                Map.of("manager_notified", true, "manager_id", manager),
                "已通知经理 " + manager + "，待审批工单 " + ticketId
        );
    }

    static class LeavePlanner implements Planner {
        @Override
        public Decision decide(AgentState state) {
            Map<String, Object> slots = state.slots;
            if (state.goal.isSatisfied(slots)) {
                return Decision.finish("请假单已创建且经理已收到通知");
            }
            if (!Boolean.TRUE.equals(slots.get("parsed"))) {
                return Decision.callTool("先把自然语言变成结构化槽位", "parse_leave_request",
                        Map.of("user_text", state.goal.description()));
            }
            if (!slots.containsKey("balance_ok")) {
                return Decision.callTool("核对剩余假期是否足够", "check_leave_balance",
                        Map.of("user_id", state.goal.userId(), "days", slots.get("days")));
            }
            if (Boolean.FALSE.equals(slots.get("balance_ok"))) {
                return Decision.askHuman("余额不足，需要主管特批或缩短天数");
            }
            if (!Boolean.TRUE.equals(slots.get("ticket_created"))) {
                return Decision.callTool("余额足够，创建请假工单", "create_leave_ticket", Map.of(
                        "user_id", state.goal.userId(),
                        "start_date", slots.get("start_date"),
                        "end_date", slots.get("end_date"),
                        "days", slots.get("days")
                ));
            }
            return Decision.callTool("工单已在，通知经理", "notify_manager",
                    Map.of("user_id", state.goal.userId(), "ticket_id", slots.get("ticket_id")));
        }
    }
}
