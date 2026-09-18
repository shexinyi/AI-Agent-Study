package com.agentbook.ch10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RememberPolicy {
    public record Fact(String key, String value, String source, double confidence) {
    }

    public record Decision(boolean write, String reason, Fact fact) {
    }

    private RememberPolicy() {
    }

    public static Decision shouldRemember(String text) {
        if (text.matches("(?s).*(密码|口令|验证码|银行卡).*")) {
            return new Decision(false, "敏感信息，禁止写入向量库", null);
        }
        if (text.length() < 8 && text.matches("(?s).*(哈哈|嗯嗯|好的|在吗).*")) {
            return new Decision(false, "寒暄，不值得长期占用", null);
        }
        if (text.contains("喜欢") || text.contains("偏好")) {
            return new Decision(true, "稳定偏好", new Fact("preference", text, "user", 0.8));
        }
        Matcher name = Pattern.compile("我是([\\u4e00-\\u9fff]{2,4})").matcher(text);
        if (name.find()) {
            boolean correction = text.contains("其实");
            return new Decision(
                    true,
                    correction ? "身份更正" : "身份槽位",
                    new Fact("employee", name.group(1), correction ? "user_correction" : "user",
                            correction ? 0.95 : 0.9)
            );
        }
        return new Decision(false, "一次性任务细节，不要向量化", null);
    }
}
