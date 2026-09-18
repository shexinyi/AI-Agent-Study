package com.agentbook.ch31;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Flow {
    public static final String LEAVE = "事假每年累计不超过 10 个工作日。林秋 2026-09-14..16 共 3 天，未超上限。";
    public static final Map<String, String> ORDERS = Map.of(
            "ORDER-88", "ORDER-88 未发货，可整单退款。写退款须人工确认。",
            "ORDER-99", "ORDER-99 已发货，物流 TZ123，须走售后。");

    public static final class Slot {
        public final String kind;
        public final String status;
        public final String text;

        public Slot(String kind, String status, String text) {
            this.kind = kind;
            this.status = status;
            this.text = text;
        }
    }

    public static final class Ticket {
        public final String id;
        public final String status;
        public final String orderId;
        public final boolean executed;

        public Ticket(String id, String status, String orderId, boolean executed) {
            this.id = id;
            this.status = status;
            this.orderId = orderId;
            this.executed = executed;
        }
    }

    public static final class Report {
        public final String text;
        public final List<Slot> slots;
        public final Ticket ticket;
        public final List<String> executed;

        public Report(String text, List<Slot> slots, Ticket ticket, List<String> executed) {
            this.text = text;
            this.slots = slots;
            this.ticket = ticket;
            this.executed = executed;
        }
    }

    public static String oid(String q) {
        String u = q.toUpperCase(Locale.ROOT);
        if (u.contains("TZ123") || u.contains("ORDER-99")) {
            return "ORDER-99";
        }
        if (u.contains("ORDER-88")) {
            return "ORDER-88";
        }
        return "";
    }

    public static String chitchat(String question) {
        if (question.contains("食堂")) {
            return "客服C：食堂今天有红烧肉。";
        }
        if (question.toUpperCase(Locale.ROOT).contains("ORDER") || question.contains("退")) {
            return "客服B：那您看要不要退款呀～";
        }
        if (question.contains("事假")) {
            return "客服A：假期的事情我记下啦。";
        }
        return "客服们：您好欢迎光临。";
    }

    public static final class Workflow {
        public int n = 0;
        public final Map<String, Ticket> tickets = new LinkedHashMap<>();

        public Report run(String question, String role, String confirm, boolean cafeBoom) {
            List<Slot> slots = new ArrayList<>();
            if (question.contains("事假") || question.contains("请假") || question.contains("工作日")) {
                slots.add(new Slot("leave", "ok", LEAVE));
            }
            if (!oid(question).isEmpty() || question.contains("退")) {
                String code = oid(question);
                if (code.isEmpty()) {
                    slots.add(new Slot("order", "clarify", "【澄清】请提供订单号。"));
                } else if ("ORDER-99".equals(code)) {
                    slots.add(new Slot("order", "ok", ORDERS.get("ORDER-99")));
                } else {
                    slots.add(new Slot("order", "ok", ORDERS.get("ORDER-88")));
                }
            }
            if (question.contains("食堂")) {
                if (cafeBoom) {
                    slots.add(new Slot("cafe", "failed", "【失败隔离】食堂菜单超时 3000ms"));
                } else {
                    slots.add(new Slot("cafe", "ok", "【无资料】食堂不在办理范围。"));
                }
            }

            Ticket ticket = null;
            List<String> executed = new ArrayList<>();
            boolean wantRefund = question.toUpperCase(Locale.ROOT).contains("ORDER-88") && question.contains("退");
            boolean unshipped = slots.stream().filter(s -> "order".equals(s.kind)).allMatch(s -> s.text.contains("未发货"));
            if (wantRefund && unshipped) {
                if (!"agent_ops".equals(role)) {
                    slots.add(new Slot("hitl", "denied", "【拒绝】intern 不在 refund_order 白名单。"));
                } else if ("approve".equals(confirm)) {
                    n++;
                    ticket = new Ticket("t" + n, "executed", "ORDER-88", true);
                    tickets.put(ticket.id, ticket);
                    executed.add("refund_order:ORDER-88");
                    slots.add(new Slot("hitl", "executed", "已执行 refund_order(ORDER-88)。"));
                } else if ("reject".equals(confirm)) {
                    slots.add(new Slot("hitl", "rejected", "【已拒绝】不执行写工具。"));
                } else {
                    n++;
                    ticket = new Ticket("t" + n, "pending", "ORDER-88", false);
                    tickets.put(ticket.id, ticket);
                    slots.add(new Slot("hitl", "pending", "审批单 " + ticket.id + " 等待确认。"));
                }
            }
            if (slots.isEmpty()) {
                return new Report("【不足】不是办理范围内的跨系统工单。", slots, null, executed);
            }
            StringBuilder sb = new StringBuilder("办理汇报：");
            for (Slot s : slots) {
                sb.append("\n- [").append(s.kind).append("] ").append(s.text);
            }
            return new Report(sb.toString(), slots, ticket, executed);
        }
    }
}
