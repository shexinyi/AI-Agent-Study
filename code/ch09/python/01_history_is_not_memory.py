"""
第 9 章示例 1：把全部聊天记录塞回去，和打包后的工作记忆比「体积」。

    python 01_history_is_not_memory.py
"""

from __future__ import annotations

from memory import WorkingMemory, tokenish

DIALOGUE = [
    ("user", "在吗"),
    ("assistant", "在的，我是请假助手。"),
    ("user", "我是凌秋，下周一到周三请假去上海办事，帮我走流程。"),
    ("assistant", "收到，3 天事假，需要经理审批。"),
    ("user", "对，通知经理就行。对了天气好像要下雨。"),
    ("assistant", "这不影响请假单。窗口期已记下。"),
    ("user", "嗯嗯"),
    ("assistant", "还需要改日期吗？"),
    ("user", "不用。就这样。"),
    ("assistant", "好，我按已有槽位提交。"),
]


def dump_all() -> str:
    return "\n".join(f"[{role}] {text}" for role, text in DIALOGUE)


if __name__ == "__main__":
    raw = dump_all()
    wm = WorkingMemory(max_turns=4)
    for role, text in DIALOGUE:
        wm.add(role, text)

    print("=== 全量聊天记录（常被误当成记忆）===")
    print(raw)
    print(f"\n体积: {tokenish(raw)} 字符")

    print("\n=== 工作记忆打包（摘要 + 槽位 + 最近窗口）===")
    print(wm.pack_for_prompt())
    print(f"\n体积: {wm.size()} 字符  槽位: {wm.slots}")
    print("\n读法：下游真正需要的是槽位和摘要，不是每一句「嗯嗯」。")
