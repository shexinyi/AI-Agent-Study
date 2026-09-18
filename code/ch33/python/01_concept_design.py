"""示例 1：概念题 + 现场设计三张草图。"""

from kit import CONCEPTS, DESIGNS, concept_ok

if __name__ == "__main__":
    print("面试先答闭环和尺子，再报框架名。\n")
    print("=== 概念 ===")
    for card in CONCEPTS:
        print(f"Q: {card['q']}")
        print(f"浅：{card['shallow']}")
        print(f"深：{card['deep']}")
        print()

    print("=== 设计（90 秒画框）===")
    for key, d in DESIGNS.items():
        print(f"[{key}] {d['name']}: {d['one']}")
        print(f"  必须有: {d['must']}")
        print(f"  不准有: {d['ban']}")
    print()

    cs, research, ops = DESIGNS["cs"], DESIGNS["research"], DESIGNS["ops"]
    checks = [
        ("四张概念卡都有加分句", all(concept_ok(c) for c in CONCEPTS)),
        ("客服有 HITL 和编排器", "hitl" in cs["must"] and "orchestrator" in cs["must"]),
        ("客服禁止自动退款", "auto_refund" in cs["ban"]),
        ("研报只读带引用", "cite" in research["must"] and "write_tool" in research["ban"]),
        ("运维变更须人批", "hitl" in ops["must"] and "fail_open" in ops["ban"]),
        ("不把 Swarm 当目标主人", "swarm_owns_goal" in cs["ban"]),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：设计题先画门禁和人，再画模型框。")
