"""
第 9 章示例 2：短期三件套——窗口挤出、摘要滚动、槽位更新。

    python 02_short_term.py
"""

from __future__ import annotations

from memory import WorkingMemory

TURNS = [
    ("user", "我是凌秋"),
    ("assistant", "你好，凌秋。"),
    ("user", "下周一到周三请假去上海"),
    ("assistant", "3 天已记录。"),
    ("user", "改成通知经理"),
    ("assistant", "好。"),
]


if __name__ == "__main__":
    wm = WorkingMemory(max_turns=3)
    for i, (role, text) in enumerate(TURNS, start=1):
        wm.add(role, text)
        print(f"\n--- 第 {i} 轮后 ---")
        print(f"窗口条数={len(wm.turns)} 摘要={wm.summary or '（空）'}")
        print(f"槽位={wm.slots}")
        print(wm.pack_for_prompt())
    print("\n观察：第 4 轮起窗口顶掉最早的话，但 employee / 日期仍在槽位里。")
