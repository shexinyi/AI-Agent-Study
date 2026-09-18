package com.agentbook.ch34;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class OssGap {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("开源像把钥匙挂门口：教学名可以留，手机号和 Key 不行。\n");
        List<String> dirtyHits = new ArrayList<>();
        Kit.DIRTY_REPO.forEach((name, text) -> dirtyHits.addAll(Kit.scanText(name, text)));
        List<String> cleanHits = new ArrayList<>();
        Kit.CLEAN_REPO.forEach((name, text) -> cleanHits.addAll(Kit.scanText(name, text)));
        System.out.println("脏仓库: " + dirtyHits);
        System.out.println("干净仓库: " + (cleanHits.isEmpty() ? "无命中" : cleanHits));
        System.out.println();
        System.out.println("=== 初级 vs 中级 ===");
        System.out.println("初级: " + String.join("；", Kit.JUNIOR.stream().sorted().toList()));
        System.out.println("中级: " + String.join("；", Kit.MID.stream().sorted().toList()));
        System.out.println();
        System.out.println("=== 8 周地图 ===");
        for (Object[] g : Kit.GAPS) {
            System.out.println(g[0] + " " + g[1] + ": 口令 " + String.join(", ", (List<String>) g[2]));
        }
        System.out.println();
        List<String> weakGap = Kit.gapOf(Kit.WEAK);
        List<String> strongGap = Kit.gapOf(Kit.STRONG);
        System.out.println("弱稿缺口: " + weakGap);
        System.out.println("强稿缺口: " + (strongGap.isEmpty() ? "齐" : strongGap));
        System.out.println();

        Object[][] checks = {
                {"脏仓库至少三处泄漏", dirtyHits.size() >= 3},
                {"干净仓库无命中", cleanHits.isEmpty()},
                {".env.example 放行", cleanHits.stream().noneMatch(h -> h.contains(".env.example"))},
                {"初级中级不是同一句", disjoint()},
                {"弱稿有缺口", weakGap.size() >= 2},
                {"强稿缺口为空", strongGap.isEmpty()},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：缺哪一周的口令，就回那一周补演示，不要再堆框架。");
    }

    @SuppressWarnings("unchecked")
    private static boolean disjoint() {
        return new HashSet<>(Kit.JUNIOR).stream().noneMatch(Kit.MID::contains);
    }
}
