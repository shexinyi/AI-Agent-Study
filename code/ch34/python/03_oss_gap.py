"""示例 3：开源扫描 + 初级/中级 + 8 周缺口。"""

from kit import CLEAN_REPO, DIRTY_REPO, GAPS, JUNIOR, MID, STRONG, WEAK, gap_of, scan_text

if __name__ == "__main__":
    print("开源像把钥匙挂门口：教学名可以留，手机号和 Key 不行。\n")
    dirty_hits: list[str] = []
    for name, text in DIRTY_REPO.items():
        dirty_hits.extend(scan_text(name, text))
    clean_hits: list[str] = []
    for name, text in CLEAN_REPO.items():
        clean_hits.extend(scan_text(name, text))
    print("脏仓库:", dirty_hits)
    print("干净仓库:", clean_hits or "无命中")
    print()
    print("=== 初级 vs 中级 ===")
    print("初级:", "；".join(sorted(JUNIOR)))
    print("中级:", "；".join(sorted(MID)))
    print()
    print("=== 8 周地图 ===")
    for week, title, needles in GAPS:
        print(f"{week} {title}: 口令 {', '.join(needles)}")
    print()
    weak_gap = gap_of(WEAK)
    strong_gap = gap_of(STRONG)
    print("弱稿缺口:", weak_gap)
    print("强稿缺口:", strong_gap or "齐")
    print()

    checks = [
        ("脏仓库至少三处泄漏", len(dirty_hits) >= 3),
        ("干净仓库无命中", clean_hits == []),
        (".env.example 放行", not any(".env.example" in h for h in clean_hits)),
        ("初级中级不是同一句", JUNIOR.isdisjoint(MID)),
        ("弱稿有缺口", len(weak_gap) >= 2),
        ("强稿缺口为空", strong_gap == []),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：缺哪一周的口令，就回那一周补演示，不要再堆框架。")
