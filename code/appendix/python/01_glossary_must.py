"""示例 1：附录 A 口令还在。"""

from root import repo_root

MUST = ["智能体", "Agent", "RAG", "HITL", "Trace", "MODEL_LIE", "可退 ≠ 已退", "意图", "Fail-closed"]


if __name__ == "__main__":
    text = (repo_root() / "docs" / "附录A-术语表.md").read_text(encoding="utf-8")
    print("附录 A 口令自检（教学）\n")
    missing = [m for m in MUST if m not in text]
    for m in MUST:
        print(f"  {'OK' if m in text else 'MISS'} {m}")
    print()
    checks = [
        ("关键口令齐全", missing == []),
        ("有排障代号表", "RETRIEVE" in text and "LOOP" in text and "BILL" in text),
        ("不写 99% 作为推荐", "不写 99%" in text),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：默写代号，不要默写框架商标。")
