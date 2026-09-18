"""示例 2：简历对照教学 JD。"""

from kit import CLEAN_RESUME, DIRTY_RESUME, JD_MUST, lint_resume

if __name__ == "__main__":
    print("JD 是尺子。简历上没有的词，面试官会以为你没做。\n")
    print("教学 JD 必须出现:", ", ".join(JD_MUST))
    print()
    dirty = lint_resume(DIRTY_RESUME)
    clean = lint_resume(CLEAN_RESUME)
    print("=== 病简历 ===")
    print(DIRTY_RESUME)
    print("问题:", dirty)
    print()
    print("=== 可投简历 ===")
    print(CLEAN_RESUME)
    print("问题:", clean or "无")
    print()

    checks = [
        ("病简历被打回", len(dirty) >= 3),
        ("病简历禁写 99%", any("99%" in x for x in dirty)),
        ("病简历禁写双框架", any("FastAPI" in x for x in dirty)),
        ("可投无问题", clean == []),
        ("可投覆盖 JD", all(k in CLEAN_RESUME for k in JD_MUST)),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：先删 99% 和双框架，再补 HITL / 金标 / 配额。")
