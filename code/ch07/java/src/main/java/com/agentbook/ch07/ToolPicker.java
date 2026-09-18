package com.agentbook.ch07;

import java.util.List;
import java.util.Map;

public class ToolPicker {
    static final List<String> CASES = List.of(
            "查一下 ORDER-88 发货了吗",
            "未发货缺货，能不能退款？",
            "ORDER-88 确认未发货，给我退。"
    );

    public static void main(String[] args) {
        Map<String, ToolSpec> good = Catalog.goodSpecs();
        Map<String, ToolSpec> bad = Catalog.badSpecs();
        System.out.println("=== 坏菜单对所有话只会点到 do_stuff（或什么都点不到） ===");
        for (String text : CASES) {
            System.out.println(text + " -> " + Picker.pick(text, bad, 2));
        }
        System.out.println("\n=== 好菜单：咨询不碰资金，执行才点退款 ===");
        for (String text : CASES) {
            System.out.println("\n用户: " + text);
            System.out.println("打分: " + Picker.pick(text, good, 2));
            System.out.println("点菜: " + Picker.toCalls(text, good).stream()
                    .map(c -> c.name() + c.arguments()).toList());
        }
    }
}
