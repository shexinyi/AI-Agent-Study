package com.agentbook.ch34;

public final class Mock30Min {
    public static void main(String[] args) {
        System.out.println("模拟面试是计时对词，不是聊天。分数是课堂尺子。\n");
        int elapsed = 0;
        System.out.println("=== 30 分钟脚本 ===");
        int sum = 0;
        for (Object[] slot : Kit.SLOTS) {
            int mins = (Integer) slot[0];
            elapsed += mins;
            sum += mins;
            System.out.printf("%2d 分内 · %s: %s%n", elapsed, slot[1], slot[2]);
        }
        System.out.println();

        Kit.Score weak = Kit.scoreTalk(Kit.WEAK);
        Kit.Score strong = Kit.scoreTalk(Kit.STRONG);
        System.out.println("=== 弱稿 ===");
        System.out.println(Kit.WEAK);
        System.out.println("得分 " + weak.points + " " + weak.notes);
        System.out.println();
        System.out.println("=== 强稿 ===");
        System.out.println(Kit.STRONG);
        System.out.println("得分 " + strong.points + " " + strong.notes);
        System.out.println();
        System.out.println("谈薪：用金标、失败路径、401/429 对齐职级。不编造年薪，不问『能加班吗换级别』。");
        System.out.println();

        Object[][] checks = {
                {"六段时间加总 30", sum == 30},
                {"弱稿低于强稿", weak.points < strong.points},
                {"弱稿因 99% 被扣", weak.notes.stream().anyMatch(n -> n.contains("99%"))},
                {"强稿含排障代号", strong.points >= 10 && Kit.STRONG.contains("MODEL_LIE")},
                {"强稿不报年薪", !Kit.STRONG.contains("年薪")},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：最后四分钟谈职级证据，不要报价。");
    }
}
