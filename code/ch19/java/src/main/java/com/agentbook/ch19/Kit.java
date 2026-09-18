package com.agentbook.ch19;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Kit {
    static final String SAFE_DAYS = "from datetime import date\nprint((date(2026,9,16)-date(2026,9,14)).days+1)\n";
    private static final Map<String, String> ORDERS = Map.of(
            "ORDER-88", "ORDER-88 未发货，可申请整单退款。写退款必须人工确认。",
            "ORDER-99", "ORDER-99 已发货，物流 TZ123，对话里不能直接退，须走售后。"
    );
    private static final String[] DANGER = {"os.system", "subprocess", "eval(", "exec(", "__import__", "open(", "socket", "requests"};

    private Kit() {
    }

    public static String norm(String src) {
        return src.strip().replaceAll("\\s+", "");
    }

    public static String getOrder(String orderId) {
        return ORDERS.getOrDefault(orderId, orderId + " 查无此单。");
    }

    public static String orderIdOf(String text) {
        String q = text.toUpperCase();
        if (q.contains("TZ123") || q.contains("ORDER-99")) {
            return "ORDER-99";
        }
        if (q.contains("ORDER-88")) {
            return "ORDER-88";
        }
        return "";
    }

    public static String runWhitelisted(String src) {
        for (String tok : DANGER) {
            if (src.contains(tok)) {
                return "REJECT: 发现危险调用，拒绝执行。";
            }
        }
        if (norm(SAFE_DAYS).equals(norm(src))) {
            return "3";
        }
        return "REJECT: 不在白名单，拒绝执行。";
    }

    public static MiniChat.Agent policy() {
        return new MiniChat.Agent("政策岗", messages -> {
            String q = MiniChat.lastUser(messages);
            String blob = join(messages);
            if (blob.contains("REJECT")) {
                return "【不足】代码未执行，不能拿结果编天数。 TERMINATE";
            }
            if (blob.contains("执行结果: 3")) {
                return "工具算出 3 天，未超事假每年 10 个工作日上限。 TERMINATE";
            }
            if (q.contains("年假") || q.contains("食堂")) {
                return "【不足】资料只覆盖事假。 TERMINATE";
            }
            if (q.contains("事假") || q.contains("请假") || q.contains("工作日")) {
                return "根据制度，事假每年累计不超过 10 个工作日。 TERMINATE";
            }
            return "政策岗不处理订单。交给订单岗。";
        });
    }

    public static MiniChat.Agent clerk() {
        return new MiniChat.Agent("订单岗", messages -> {
            String blob = join(messages);
            boolean toolDone = messages.stream().anyMatch(m ->
                    "用户代理".equals(m.name) && (m.content.contains("未发货") || m.content.contains("已发货") || m.content.contains("查无")));
            if (!toolDone) {
                String oid = orderIdOf(blob);
                if (oid.isEmpty()) {
                    return "订单岗需要单号。 TERMINATE";
                }
                return "请用户代理查单。 TOOL:get_order:" + oid;
            }
            if (blob.contains("未发货")) {
                return "根据工具，ORDER-88 未发货，用户本人可申请整单退款。写工具前须人工确认。 TERMINATE";
            }
            if (blob.contains("已发货") || blob.contains("TZ123")) {
                return "根据工具，TZ123 对应 ORDER-99，已发货须走售后。 TERMINATE";
            }
            return "根据工具，查无此单。 TERMINATE";
        });
    }

    public static MiniChat.Agent proxy() {
        return new MiniChat.Agent("用户代理", messages -> {
            String last = messages.get(messages.size() - 1).content;
            Matcher m = Pattern.compile("TOOL:get_order:(ORDER-\\d+)").matcher(last);
            if (m.find()) {
                return getOrder(m.group(1));
            }
            if (last.contains("```")) {
                String src = last.split("```", 3)[1].replaceFirst("(?i)^python", "").strip();
                return "执行结果: " + runWhitelisted(src);
            }
            return "用户代理无动作。";
        });
    }

    public static MiniChat.Agent coder() {
        return new MiniChat.Agent("代码岗", messages -> {
            String q = MiniChat.lastUser(messages);
            if (q.contains("危险") || q.toLowerCase().contains("pwn")) {
                return "```python\nimport os\nos.system('echo pwn')\n```";
            }
            return "```python\n" + SAFE_DAYS + "```";
        });
    }

    static MiniChat.Agent byName(List<MiniChat.Agent> agents, String name) {
        return agents.stream().filter(a -> a.name.equals(name)).findFirst().orElseThrow();
    }

    private static String join(List<MiniChat.Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (MiniChat.Message m : messages) {
            sb.append(m.content).append('\n');
        }
        return sb.toString();
    }
}
