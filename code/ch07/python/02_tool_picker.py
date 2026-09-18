"""
第 7 章示例 2：多工具选择。同一套好菜单，三句用户话点出不同菜。

    python 02_tool_picker.py
"""

from __future__ import annotations

from catalog import bad_specs, good_specs
from picker import pick, to_calls

CASES = [
    "查一下 ORDER-88 发货了吗",
    "未发货缺货，能不能退款？",
    "ORDER-88 确认未发货，给我退。",
]


if __name__ == "__main__":
    good = good_specs()
    bad = bad_specs()
    print("=== 坏菜单：关键词对不上这些问法，往往点不到任何菜 ===")
    for text in CASES:
        print(f"{text!r} -> {pick(text, bad)}")

    print("\n=== 好菜单：咨询不碰资金，执行才点退款 ===")
    for text in CASES:
        ranked = pick(text, good)
        calls = to_calls(text, good)
        print(f"\n用户: {text}")
        print(f"打分: {ranked}")
        print("点菜:", [(c.name, c.arguments) for c in calls])
