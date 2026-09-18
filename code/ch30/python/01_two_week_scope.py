"""示例 1：两周范围。只答事假和餐补；ACL 关掉就会漏出 5 天。"""

from kb import answer

ROWS = [
    ("intern 事假", "事假每年上限几天？", "intern", True),
    ("intern 年假", "年假上限几天？", "intern", True),
    ("intern 退款", "ORDER-88 能不能退？", "intern", True),
    ("intern 关 ACL", "事假最多几天？", "intern", False),
    ("hr 开 ACL", "事假最多几天？", "hr", True),
]


if __name__ == "__main__":
    print("业务痛点：口口相传把事假说成 5 天。两周切片只做制度问答，不做退款。")
    print("关 ACL 是对照病，用来证明门禁有效，不是上线开关。\n")
    texts = []
    for title, q, role, acl in ROWS:
        text, hits = answer(q, role, apply_acl=acl)
        ids = [d.id for d in hits]
        texts.append(text)
        print(f"{title:12} acl={acl!s:5} hits={ids} | {text}")
    print()
    checks = [
        ("范围内带引用", "10 个工作日" in texts[0] and "[leave10]" in texts[0]),
        ("年假不足", texts[1].startswith("【不足】")),
        ("退款不在范围", texts[2].startswith("【不足】")),
        ("关 ACL 漏 5 天", "5 天" in texts[3]),
        ("hr 仍优先现行", "10 个工作日" in texts[4] and "5 天" not in texts[4]),
    ]
    # hr 开 ACL 能看见废止稿，但答题应尽量用现行。retriever 对 deprecated 减分。
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：范围外说不足，比硬答加分。对照病要能复现，不能只在 PPT 里画一把锁。")
