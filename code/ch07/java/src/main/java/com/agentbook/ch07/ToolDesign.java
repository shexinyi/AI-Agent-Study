package com.agentbook.ch07;

import java.util.Map;

public class ToolDesign {
    public static void main(String[] args) {
        dump("坏菜单：一个 do_stuff 包打天下", Catalog.badSpecs());
        dump("好菜单：查单 / 政策 / 退款 分开", Catalog.goodSpecs());
        System.out.println("\n读法：模型点菜只看见 description。when_not 必须写进 description，不能只写在 wiki。");
    }

    static void dump(String title, Map<String, ToolSpec> specs) {
        System.out.println("=".repeat(60));
        System.out.println(title);
        System.out.println("=".repeat(60));
        for (ToolSpec spec : specs.values()) {
            System.out.println("\n# " + spec.name() + "  risk=" + spec.risk()
                    + "  timeout=" + spec.timeoutSeconds() + "s  idempotent=" + spec.idempotent());
            System.out.println("description: " + spec.description());
            System.out.println("when_to_use: " + spec.whenToUse());
            System.out.println("when_not: " + spec.whenNot());
            System.out.println("menu: " + spec.menuText());
        }
    }
}
