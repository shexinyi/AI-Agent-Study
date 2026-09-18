package com.agentbook.ch09;

import java.util.List;

public class ShortTerm {
    public static void main(String[] args) {
        MemoryKit.WorkingMemory wm = new MemoryKit.WorkingMemory(3);
        List<String[]> turns = List.of(
                new String[]{"user", "我是凌秋"},
                new String[]{"assistant", "你好，凌秋。"},
                new String[]{"user", "下周一到周三请假去上海"},
                new String[]{"assistant", "3 天已记录。"},
                new String[]{"user", "改成通知经理"},
                new String[]{"assistant", "好。"}
        );
        int i = 1;
        for (String[] t : turns) {
            wm.add(t[0], t[1]);
            System.out.println("\n--- 第 " + i++ + " 轮后 ---");
            System.out.println("窗口条数=" + wm.turns.size() + " 摘要=" + (wm.summary.isBlank() ? "（空）" : wm.summary));
            System.out.println("槽位=" + wm.slots);
            System.out.println(wm.packForPrompt());
        }
        System.out.println("\n观察：第 4 轮起窗口顶掉最早的话，但 employee / 日期仍在槽位里。");
    }
}
