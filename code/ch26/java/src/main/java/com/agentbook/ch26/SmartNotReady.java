package com.agentbook.ch26;

public final class SmartNotReady {
    public static void main(String[] args) {
        System.out.println("clever 会说话：爱把可退说成已退，复合问法只答后一句。");
        System.out.println("gated 带闸门：须确认、不执行写工具、食堂【不足】。");
        System.out.println("自拍分 = 像一句完整人话。它不是上线尺子。");
        System.out.println();

        MiniSmith.Experiment a = MiniSmith.run("clever", Kit.CASES, Kit::clever);
        MiniSmith.Experiment b = MiniSmith.run("gated", Kit.CASES, Kit::gated);
        Kit.show(a);
        Kit.show(b);

        Kit.check("clever 自拍更高", a.rate("vanity") > b.rate("vanity"));
        Kit.check("clever 通过率低", a.passRate() < 0.5);
        Kit.check("gated 全过", b.passRate() == 1.0);
        Kit.check("gated 自拍可以低", b.rate("vanity") < 1.0);
        System.out.println("读法：在线看着挺聪明，只是自拍。闸门三列才是验血。");
    }
}
