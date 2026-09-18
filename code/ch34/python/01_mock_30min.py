"""示例 1：30 分钟模拟面试 + 谈薪对齐。"""

from kit import SLOTS, STRONG, WEAK, score_talk

if __name__ == "__main__":
    print("模拟面试是计时对词，不是聊天。分数是课堂尺子。\n")
    elapsed = 0
    print("=== 30 分钟脚本 ===")
    for mins, title, tip in SLOTS:
        elapsed += mins
        print(f"{elapsed:2d} 分内 · {title}: {tip}")
    print()

    weak = score_talk(WEAK)
    strong = score_talk(STRONG)
    print("=== 弱稿 ===")
    print(WEAK)
    print(f"得分 {weak.points} {weak.notes}")
    print()
    print("=== 强稿 ===")
    print(STRONG)
    print(f"得分 {strong.points} {strong.notes}")
    print()
    print("谈薪：用金标、失败路径、401/429 对齐职级。不编造年薪，不问『能加班吗换级别』。")
    print()

    checks = [
        ("六段时间加总 30", sum(m for m, _, _ in SLOTS) == 30),
        ("弱稿低于强稿", weak.points < strong.points),
        ("弱稿因 99% 被扣", any("99%" in n for n in weak.notes)),
        ("强稿含排障代号", strong.points >= 10 and "MODEL_LIE" in STRONG),
        ("强稿不报年薪", "年薪" not in STRONG),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：最后四分钟谈职级证据，不要报价。")
