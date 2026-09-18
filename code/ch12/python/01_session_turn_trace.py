"""
第 12 章示例 1：Session / Turn / Trace，多轮填槽。

    python 01_session_turn_trace.py
"""

from __future__ import annotations

from session import MemoryStore, SessionService

LINES = [
    "我是林秋",
    "下周一到周三请假去上海",
    "事假，通知经理就行",
]


if __name__ == "__main__":
    svc = SessionService(MemoryStore())
    sess = svc.start("linqiu", "sess-leave-1")
    print("新会话:", sess.session_id, "phase=", sess.phase)
    for line in LINES:
        sess = svc.handle("linqiu", sess.session_id, line)
        print(f"\n=== 用户: {line} ===")
        print(sess.pack_for_prompt())
        print("缺失:", sess.missing() or "无")
    print("\n轨迹:")
    for ev in sess.trace:
        print(f"  [{ev.kind}] {ev.text}")
    print("\n读法：轮次是原文，槽位是准星，轨迹是审计。上海出差进不了必填槽，那是情节不是验收。")
