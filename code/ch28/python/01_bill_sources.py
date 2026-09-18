"""示例 1：同一组问法，浪费路径 vs 节俭路径的账单四列。"""

from kit import fat, lean, reset_cache
from minibill import summarize

QS = [
    "林秋问：事假每年上限几天？",
    "林秋问：ORDER-88 能不能退？",
    "林秋问：食堂有没有红烧肉？",
]


def dump(title: str, runs) -> None:
    bag = summarize(runs)
    total = round(sum(bag.values()), 4)
    print(f"=== {title} 合计 {total} 教学币 ===")
    print(f"  输入={bag['input']}  输出={bag['output']}  重试={bag['retry']}  检索={bag['retrieve']}")
    for q, run in zip(QS, runs):
        print(f"  - {q} → {run.amount} / {run.model} / {run.text[:22]}")
    print()
    return total


if __name__ == "__main__":
    print("账单四列：输入、输出、重试、检索。单价是课堂教学币，不是官网价。")
    print("Token 按字符数计。fat 会长 Prompt、检索 3 次、再重试 2 次。\n")
    reset_cache()
    fat_runs = [fat(q) for q in QS]
    lean_runs = [lean(q, use_cache=False) for q in QS]
    t_fat = dump("fat 浪费", fat_runs)
    t_lean = dump("lean 节俭", lean_runs)

    fat_bag = summarize(fat_runs)
    lean_bag = summarize(lean_runs)
    checks = [
        ("浪费更贵", t_fat > t_lean),
        ("浪费的重试不是 0", fat_bag["retry"] > 0),
        ("节俭的重试是 0", lean_bag["retry"] == 0),
        ("浪费检索更贵", fat_bag["retrieve"] > lean_bag["retrieve"]),
        ("四列都能看见", all(k in fat_bag for k in ("input", "output", "retry", "retrieve"))),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：先拆四列，再谈优化。重试和长 Prompt 往往比「换更贵的模型」更烧钱。")
