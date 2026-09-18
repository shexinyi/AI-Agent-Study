package com.agentbook.ch12;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SessionKit {
    public static final List<String> REQUIRED = List.of("employee", "start_date", "end_date", "days");

    public static final class Slot {
        public final String name;
        public Object value;
        public String status = "empty";
        public Object proposed;

        Slot(String name) {
            this.name = name;
        }

        String show() {
            if ("proposed".equals(status)) {
                return name + "=" + value + "->" + proposed + "(待确认)";
            }
            if ("empty".equals(status)) {
                return name + "=∅";
            }
            return name + "=" + value + "(" + status + ")";
        }
    }

    public static final class Turn {
        public final int no;
        public final String role;
        public final String content;

        Turn(int no, String role, String content) {
            this.no = no;
            this.role = role;
            this.content = content;
        }
    }

    public static final class Event {
        public final String kind;
        public final String text;

        Event(String kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    public static final class Session {
        public final String sessionId;
        public final String userId;
        public String phase = "collecting";
        public int version = 1;
        public final Map<String, Slot> slots = new LinkedHashMap<>();
        public final List<Turn> turns = new ArrayList<>();
        public final List<Event> trace = new ArrayList<>();

        public Session(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            for (String n : List.of("employee", "start_date", "end_date", "days", "leave_type")) {
                slots.put(n, new Slot(n));
            }
        }

        void emit(String kind, String text) {
            trace.add(new Event(kind, text));
        }

        public List<String> missing() {
            List<String> out = new ArrayList<>();
            for (String n : REQUIRED) {
                if ("empty".equals(slots.get(n).status)) {
                    out.add(n);
                }
            }
            return out;
        }

        public String packForPrompt() {
            StringBuilder sb = new StringBuilder();
            sb.append("[session ").append(sessionId).append(" user=").append(userId)
                    .append(" phase=").append(phase).append(" v=").append(version).append("]\n");
            sb.append("[槽位] ");
            sb.append(String.join(", ", slots.values().stream().map(Slot::show).toList()));
            if (!missing().isEmpty()) {
                sb.append("\n[缺失] ").append(String.join(", ", missing()));
            }
            int from = Math.max(0, turns.size() - 4);
            for (int i = from; i < turns.size(); i++) {
                Turn t = turns.get(i);
                sb.append("\n[t").append(t.no).append(" ").append(t.role).append("] ").append(t.content);
            }
            return sb.toString();
        }

        Session copy() {
            Session c = new Session(sessionId, userId);
            c.phase = phase;
            c.version = version;
            c.slots.clear();
            slots.forEach((k, s) -> {
                Slot ns = new Slot(k);
                ns.value = s.value;
                ns.status = s.status;
                ns.proposed = s.proposed;
                c.slots.put(k, ns);
            });
            c.turns.addAll(turns);
            c.trace.addAll(trace);
            return c;
        }
    }

    public static final class VersionConflict extends RuntimeException {
        VersionConflict(String msg) {
            super(msg);
        }
    }

    public interface StateStore {
        Session get(String userId, String sessionId);

        void save(Session sess, int expectedVersion);
    }

    public static class MemoryStore implements StateStore {
        final Map<String, Session> data = new LinkedHashMap<>();

        @Override
        public Session get(String userId, String sessionId) {
            Session sess = data.get(sessionId);
            if (sess == null || !sess.userId.equals(userId)) {
                return null;
            }
            return sess.copy();
        }

        @Override
        public void save(Session sess, int expectedVersion) {
            Session old = data.get(sess.sessionId);
            int current = old == null ? 0 : old.version;
            if (current != expectedVersion) {
                throw new VersionConflict("expected v=" + expectedVersion + " actual v=" + current);
            }
            if (old != null && !old.userId.equals(sess.userId)) {
                throw new SecurityException("cannot overwrite another user");
            }
            sess.version = expectedVersion + 1;
            data.put(sess.sessionId, sess.copy());
        }
    }

    public static final class FileStore extends MemoryStore {
        private final Path path;
        private final ObjectMapper mapper = new ObjectMapper();

        public FileStore(Path path) {
            this.path = path;
            try {
                Files.createDirectories(path.getParent());
                if (Files.exists(path)) {
                    JsonNode root = mapper.readTree(path.toFile());
                    root.fields().forEachRemaining(e -> data.put(e.getKey(), fromJson(e.getValue())));
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void save(Session sess, int expectedVersion) {
            super.save(sess, expectedVersion);
            try {
                ObjectNode root = mapper.createObjectNode();
                data.forEach((id, s) -> root.set(id, toJson(s)));
                mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private ObjectNode toJson(Session s) {
            ObjectNode n = mapper.createObjectNode();
            n.put("sessionId", s.sessionId);
            n.put("userId", s.userId);
            n.put("phase", s.phase);
            n.put("version", s.version);
            ObjectNode slots = mapper.createObjectNode();
            s.slots.forEach((k, sl) -> {
                ObjectNode sn = mapper.createObjectNode();
                sn.put("name", sl.name);
                if (sl.value != null) {
                    sn.putPOJO("value", sl.value);
                }
                sn.put("status", sl.status);
                if (sl.proposed != null) {
                    sn.putPOJO("proposed", sl.proposed);
                }
                slots.set(k, sn);
            });
            n.set("slots", slots);
            return n;
        }

        private Session fromJson(JsonNode n) {
            Session s = new Session(n.get("sessionId").asText(), n.get("userId").asText());
            s.phase = n.get("phase").asText();
            s.version = n.get("version").asInt();
            n.get("slots").fields().forEachRemaining(e -> {
                JsonNode sn = e.getValue();
                Slot sl = new Slot(e.getKey());
                sl.status = sn.get("status").asText();
                if (sn.hasNonNull("value")) {
                    sl.value = sn.get("value").isInt() ? sn.get("value").asInt() : sn.get("value").asText();
                }
                if (sn.hasNonNull("proposed")) {
                    sl.proposed = sn.get("proposed").isInt() ? sn.get("proposed").asInt() : sn.get("proposed").asText();
                }
                s.slots.put(e.getKey(), sl);
            });
            return s;
        }
    }

    public static final class SessionService {
        private final StateStore store;

        public SessionService(StateStore store) {
            this.store = store;
        }

        public Session start(String userId, String sessionId) {
            Session sess = new Session(sessionId, userId);
            sess.emit("created", "user=" + userId);
            store.save(sess, 0);
            return store.get(userId, sessionId);
        }

        public Session handle(String userId, String sessionId, String text) {
            Session sess = store.get(userId, sessionId);
            if (sess == null) {
                throw new SecurityException("isolation_or_missing");
            }
            int expected = sess.version;
            sess.turns.add(new Turn(sess.turns.size() + 1, "user", text));
            sess.emit("turn", text);
            if ("不用了".equals(text.strip()) || "取消".equals(text.strip())) {
                sess.phase = "cancelled";
                sess.emit("cancelled", text);
            } else if (isConfirm(text) && ("awaiting_confirm".equals(sess.phase) || "collecting".equals(sess.phase))) {
                applyConfirm(sess);
            } else if (text.contains("还是原来")) {
                rejectProposed(sess);
            } else {
                fillFromUser(sess, text);
                if (sess.missing().isEmpty() && "collecting".equals(sess.phase)) {
                    sess.phase = "awaiting_confirm";
                    sess.emit("ready", "必填已齐，等待确认");
                }
            }
            store.save(sess, expected);
            return store.get(userId, sessionId);
        }
    }

    static boolean isConfirm(String text) {
        return text.strip().matches("对|确认|没问题|就这样|好的，提交");
    }

    static void fillFromUser(Session sess, String text) {
        Map<String, Object> updates = new LinkedHashMap<>();
        Matcher name = Pattern.compile("我是([\\u4e00-\\u9fff]{2,4})").matcher(text);
        if (name.find()) {
            updates.put("employee", name.group(1));
        }
        if (text.contains("周一") && text.contains("周三")) {
            updates.put("start_date", "2026-09-14");
            updates.put("end_date", "2026-09-16");
            updates.put("days", 3);
        }
        if (text.contains("周四") && text.contains("结束")) {
            String start = sess.slots.get("start_date").value == null ? "2026-09-14"
                    : String.valueOf(sess.slots.get("start_date").value);
            updates.put("start_date", start);
            updates.put("end_date", "2026-09-17");
            updates.put("days", LocalDate.parse("2026-09-17").toEpochDay() - LocalDate.parse(start).toEpochDay() + 1);
        }
        if (text.contains("两天") || text.contains("2天")) {
            String start = updates.get("start_date") != null ? String.valueOf(updates.get("start_date"))
                    : (sess.slots.get("start_date").value == null ? "2026-09-14"
                    : String.valueOf(sess.slots.get("start_date").value));
            updates.put("start_date", start);
            updates.put("end_date", LocalDate.parse(start).plusDays(1).toString());
            updates.put("days", 2);
        }
        if (text.contains("事假")) {
            updates.put("leave_type", "事假");
        }
        if (text.contains("年假")) {
            updates.put("leave_type", "年假");
        }
        updates.forEach((k, v) -> putSlot(sess, k, v));
    }

    static void putSlot(Session sess, String key, Object value) {
        Slot slot = sess.slots.get(key);
        if ("confirmed".equals(slot.status) && slot.value != null && !String.valueOf(slot.value).equals(String.valueOf(value))) {
            slot.proposed = value;
            slot.status = "proposed";
            sess.phase = "awaiting_confirm";
            sess.emit("proposed", key + ": " + slot.value + " -> " + value);
            return;
        }
        if (slot.value != null && String.valueOf(slot.value).equals(String.valueOf(value)) && !"empty".equals(slot.status)) {
            return;
        }
        slot.value = value;
        slot.status = "filled";
        slot.proposed = null;
        sess.emit("filled", key + "=" + value);
    }

    static void applyConfirm(Session sess) {
        for (Slot slot : sess.slots.values()) {
            if ("proposed".equals(slot.status)) {
                slot.value = slot.proposed;
                slot.proposed = null;
            }
            if ("filled".equals(slot.status) || "proposed".equals(slot.status)) {
                slot.status = "confirmed";
            }
        }
        if (!sess.missing().isEmpty()) {
            sess.phase = "collecting";
            return;
        }
        sess.phase = "confirmed";
        sess.emit("confirmed", "用户确认，可提交");
    }

    static void rejectProposed(Session sess) {
        for (Slot slot : sess.slots.values()) {
            if ("proposed".equals(slot.status)) {
                slot.proposed = null;
                slot.status = "confirmed";
                sess.emit("rejected", slot.name + " 保持 " + slot.value);
            }
        }
        sess.phase = sess.missing().isEmpty() ? "confirmed" : "collecting";
    }

    private SessionKit() {
    }
}
