package com.agentbook.ch28;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BillSources {
    static final List<String> QS = List.of(
            "林秋问：事假每年上限几天？",
            "林秋问：ORDER-88 能不能退？",
            "林秋问：食堂有没有红烧肉？"
    );

    public static void main(String[] args) {
        System.out.println("账单四列：输入、输出、重试、检索。单价是课堂教学币，不是官网价。");
        System.out.println("Token 按字符数计。fat 会长 Prompt、检索 3 次、再重试 2 次。");
        System.out.println();
        Kit.resetCache();
        List<MiniBill.Run> fatRuns = new ArrayList<>();
        List<MiniBill.Run> leanRuns = new ArrayList<>();
        for (String q : QS) {
            fatRuns.add(Kit.fat(q));
            leanRuns.add(Kit.lean(q, false));
        }
        double tFat = dump("fat 浪费", fatRuns);
        double tLean = dump("lean 节俭", leanRuns);
        Map<String, Double> fatBag = MiniBill.summarize(fatRuns);
        Map<String, Double> leanBag = MiniBill.summarize(leanRuns);
        Kit.check("浪费更贵", tFat > tLean);
        Kit.check("浪费的重试不是 0", fatBag.get("retry") > 0);
        Kit.check("节俭的重试是 0", leanBag.get("retry") == 0);
        Kit.check("浪费检索更贵", fatBag.get("retrieve") > leanBag.get("retrieve"));
        Kit.check("四列都能看见", fatBag.keySet().containsAll(List.of("input", "output", "retry", "retrieve")));
        System.out.println("读法：先拆四列，再谈优化。重试和长 Prompt 往往比「换更贵的模型」更烧钱。");
    }

    private static double dump(String title, List<MiniBill.Run> runs) {
        Map<String, Double> bag = MiniBill.summarize(runs);
        double total = Math.round(bag.values().stream().mapToDouble(x -> x).sum() * 10000.0) / 10000.0;
        System.out.println("=== " + title + " 合计 " + total + " 教学币 ===");
        System.out.println("  输入=" + bag.get("input") + "  输出=" + bag.get("output")
                + "  重试=" + bag.get("retry") + "  检索=" + bag.get("retrieve"));
        for (int i = 0; i < QS.size(); i++) {
            MiniBill.Run run = runs.get(i);
            String clip = run.text.length() > 22 ? run.text.substring(0, 22) : run.text;
            System.out.println("  - " + QS.get(i) + " → " + run.amount() + " / " + run.model + " / " + clip);
        }
        System.out.println();
        return total;
    }
}
