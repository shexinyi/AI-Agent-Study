package com.agentbook.ch25;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 人类介入教具。人的决定由脚本传入；资金禁止 fail_open；写工具按单号幂等。 */
public final class MiniHitl {
    public static final String READ = "read";
    public static final String MONEY = "money";
    public static final String WRITE = "write";

    public static final String AUTO = "auto";
    public static final String APPROVE = "approve";
    public static final String CLARIFY = "clarify";
    public static final String TAKEOVER = "takeover";
    public static final String REFUSE = "refuse";

    public static final String TICKET = "ticket";
    public static final String IM = "im";
    public static final String CONSOLE = "console";

    public static final String FAIL_CLOSED = "fail_closed";
    public static final String FAIL_OPEN = "fail_open";
    public static final String ESCALATE = "escalate";

    public static final class PolicyError extends RuntimeException {
        public PolicyError(String message) {
            super(message);
        }
    }

    public static final class Action {
        public final String name;
        public final Map<String, String> args;
        public final String risk;

        public Action(String name, Map<String, String> args, String risk) {
            this.name = name;
            this.args = new LinkedHashMap<>(args);
            this.risk = risk;
        }
    }

    public static final class Ticket {
        public final String id;
        public String mode;
        public String channel;
        public final Action action;
        public String status;
        public final int createdMs;
        public final int deadlineMs;
        public final String timeoutPolicy;
        public final Map<String, String> payload = new LinkedHashMap<>();
        public String note = "";

        public Ticket(String id, String mode, String channel, Action action, String status,
                      int createdMs, int deadlineMs, String timeoutPolicy) {
            this.id = id;
            this.mode = mode;
            this.channel = channel;
            this.action = action;
            this.status = status;
            this.createdMs = createdMs;
            this.deadlineMs = deadlineMs;
            this.timeoutPolicy = timeoutPolicy;
        }
    }

    public static final class Clock {
        public int t;

        public Clock() {
            this(0);
        }

        public Clock(int t) {
            this.t = t;
        }

        public int now() {
            return t;
        }

        public void advance(int ms) {
            t += ms;
        }
    }

    public static final class HitlRuntime {
        public final Clock clock;
        public final Map<String, Ticket> tickets = new LinkedHashMap<>();
        public int seq = 0;
        public final Map<String, String> executed = new LinkedHashMap<>();
        public final List<String> log = new ArrayList<>();

        public HitlRuntime() {
            this(new Clock());
        }

        public HitlRuntime(Clock clock) {
            this.clock = clock;
        }

        public String classify(String user, Action action) {
            if (user.contains("律师") || user.contains("投诉") || user.contains("报警")) {
                return TAKEOVER;
            }
            if (user.contains("食堂")) {
                return REFUSE;
            }
            String oid = action == null ? "" : action.args.getOrDefault("order_id", "");
            boolean missingRefund = (action != null && "refund_order".equals(action.name)
                    && oid.isEmpty() && !hasOrder(user))
                    || (action == null && user.contains("退") && !hasOrder(user));
            if (missingRefund) {
                return CLARIFY;
            }
            if ((user.contains("请假") || user.contains("事假")) && !user.contains("2026-09-14")
                    && (action == null || "create_leave".equals(action.name))) {
                return CLARIFY;
            }
            if (action == null) {
                if (user.contains("退") && user.toUpperCase(Locale.ROOT).contains("ORDER-99")) {
                    return REFUSE;
                }
                if (user.contains("退") && user.toUpperCase(Locale.ROOT).contains("ORDER-88")) {
                    return APPROVE;
                }
                if (user.contains("事假") || user.contains("请假") || user.toUpperCase(Locale.ROOT).contains("ORDER")
                        || user.toUpperCase(Locale.ROOT).contains("TZ123")) {
                    return AUTO;
                }
                return REFUSE;
            }
            if (READ.equals(action.risk)) {
                return AUTO;
            }
            if ("refund_order".equals(action.name)
                    && ("ORDER-99".equals(oid) || user.toUpperCase(Locale.ROOT).contains("ORDER-99"))) {
                return REFUSE;
            }
            if (MONEY.equals(action.risk) || WRITE.equals(action.risk)) {
                return APPROVE;
            }
            return AUTO;
        }

        public Ticket submit(String mode, String channel, Action action, int waitMs, String timeoutPolicy,
                             Map<String, String> payload) {
            if (MONEY.equals(action.risk) && FAIL_OPEN.equals(timeoutPolicy)) {
                throw new PolicyError("资金动作禁止 fail_open");
            }
            seq++;
            int now = clock.now();
            Ticket ticket = new Ticket("hitl-" + seq, mode, channel, action, "pending", now, now + waitMs, timeoutPolicy);
            if (payload != null) {
                ticket.payload.putAll(payload);
            }
            tickets.put(ticket.id, ticket);
            log.add("submit " + ticket.id + " " + mode + "/" + channel + " " + action.name);
            return ticket;
        }

        public String resume(String ticketId, String decision, String note) {
            Ticket ticket = tickets.get(ticketId);
            if (!"pending".equals(ticket.status)) {
                return "【不能恢复】" + ticket.id + " 已是 " + ticket.status;
            }
            ticket.note = note;
            if ("approve".equals(decision)) {
                ticket.status = "approved";
                return execute(ticket);
            }
            if ("reject".equals(decision)) {
                ticket.status = "rejected";
                log.add("reject " + ticket.id);
                return "【已拒绝】不执行写工具。";
            }
            if ("clarify".equals(decision)) {
                ticket.status = "clarified";
                ticket.payload.put("answer", note);
                if (note.toUpperCase(Locale.ROOT).startsWith("ORDER-88")) {
                    ticket.action.args.put("order_id", "ORDER-88");
                    ticket.mode = APPROVE;
                    ticket.channel = TICKET;
                    ticket.status = "pending";
                    log.add("clarify→approve " + ticket.id);
                    return ticket.id + " 槽位已补，仍须审批。";
                }
                return ticket.id + " 已澄清：" + note;
            }
            if ("takeover".equals(decision)) {
                ticket.status = "taken_over";
                log.add("takeover " + ticket.id);
                return "【已接管】Agent 停手，由控制台人工继续。";
            }
            return "【不足】不认识的决定 " + decision;
        }

        public String replay(String ticketId) {
            Ticket ticket = tickets.get(ticketId);
            String snap = "回放 " + ticket.id + " mode=" + ticket.mode + " status=" + ticket.status
                    + " action=" + ticket.action.name + " " + ticket.action.args;
            if ((MONEY.equals(ticket.action.risk) || WRITE.equals(ticket.action.risk))
                    && ("approved".equals(ticket.status) || "executed".equals(ticket.status))) {
                return snap + " | " + execute(ticket);
            }
            return snap;
        }

        public List<String> tick() {
            List<String> reports = new ArrayList<>();
            int now = clock.now();
            for (Ticket ticket : new ArrayList<>(tickets.values())) {
                if (!"pending".equals(ticket.status) || now < ticket.deadlineMs) {
                    continue;
                }
                if (FAIL_OPEN.equals(ticket.timeoutPolicy)) {
                    throw new PolicyError("资金动作禁止 fail_open");
                }
                String msg;
                if (ESCALATE.equals(ticket.timeoutPolicy)) {
                    ticket.status = "escalated";
                    seq++;
                    Ticket boss = new Ticket("hitl-" + seq, APPROVE, CONSOLE, ticket.action, "pending",
                            now, now + 60_000, FAIL_CLOSED);
                    boss.payload.put("escalated_from", ticket.id);
                    tickets.put(boss.id, boss);
                    msg = "【已升级】" + ticket.id + " → " + boss.id + "，仍未执行写工具。";
                } else {
                    ticket.status = "timed_out";
                    msg = "【超时关闭】" + ticket.id + " 未执行 " + ticket.action.name + "。";
                }
                log.add(msg);
                reports.add(msg);
            }
            return reports;
        }

        private String execute(Ticket ticket) {
            Action action = ticket.action;
            String idPart = action.args.getOrDefault("order_id",
                    action.args.getOrDefault("leave_id", action.name));
            String key = action.name + ":" + idPart;
            if (executed.containsKey(key)) {
                log.add("idempotent " + key);
                return "【幂等】" + key + " 已执行过：" + executed.get(key);
            }
            String text;
            if ("refund_order".equals(action.name)) {
                if (!"ORDER-88".equals(action.args.getOrDefault("order_id", ""))) {
                    return "【不足】只能退未发货的 ORDER-88。";
                }
                text = "已执行 refund_order(ORDER-88)。";
            } else if ("create_leave".equals(action.name)) {
                text = "已执行 create_leave(林秋, 2026-09-14..16, 3天)。";
            } else {
                text = "已执行 " + action.name + "。";
            }
            executed.put(key, text);
            ticket.status = "executed";
            log.add("exec " + key);
            return text;
        }

        private static boolean hasOrder(String text) {
            String q = text.toUpperCase(Locale.ROOT);
            return q.contains("ORDER") || q.contains("TZ123");
        }
    }
}
