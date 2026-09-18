package com.agentbook.ch27;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RedactPrivacy {
    public static void main(String[] args) {
        System.out.println("该留：trace_id、工具名、ORDER-88、延迟、Token、错误类型。");
        System.out.println("不该留：手机、身份证、邮箱、银行卡、未脱敏的整段 Prompt。");
        System.out.println();
        System.out.println("原文：");
        System.out.println("  " + Kit.RAW_LOG);
        String clean = MiniTrace.redact(Kit.RAW_LOG);
        System.out.println("脱敏：");
        System.out.println("  " + clean);
        System.out.println("原文泄漏=" + orNone(MiniTrace.leak(Kit.RAW_LOG)));
        System.out.println("脱敏后泄漏=" + orNone(MiniTrace.leak(clean)));
        System.out.println();
        Kit.check("原文能抓到四类隐私",
                new HashSet<>(MiniTrace.leak(Kit.RAW_LOG)).equals(Set.of("phone", "id", "email", "bank")));
        Kit.check("脱敏后无泄漏", MiniTrace.leak(clean).isEmpty());
        Kit.check("仍能看见订单", clean.contains("ORDER-88"));
        Kit.check("仍能看见人名业务键", clean.contains("林秋"));
        Kit.check("卡号已糊", !clean.contains("6222021234567890123") && clean.contains("[BANK]"));
        System.out.println("读法：单号是排障钥匙，手机号是事故。林秋作为演示名可留，证件不能留。");
    }

    private static String orNone(List<String> items) {
        return items.isEmpty() ? "无" : items.toString();
    }
}
