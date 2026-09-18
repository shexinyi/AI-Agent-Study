"""
第 12 章示例 2：先确认再改；已确认槽位不能默默覆盖。

    python 02_confirm_and_correct.py
"""

from __future__ import annotations

from session import MemoryStore, SessionService


def show(sess, title: str) -> None:
    print(f"\n=== {title}  phase={sess.phase} ===")
    print("  " + ", ".join(s.show() for s in sess.slots.values()))


if __name__ == "__main__":
    svc = SessionService(MemoryStore())
    sid = "sess-leave-2"
    svc.start("linqiu", sid)
    svc.handle("linqiu", sid, "我是林秋，下周一到周三请假，事假")
    sess = svc.handle("linqiu", sid, "确认")
    show(sess, "确认提交")

    sess = svc.handle("linqiu", sid, "其实只要两天")
    show(sess, "提出修改（旧值还在，新值是 proposed）")

    sess = svc.handle("linqiu", sid, "还是原来的")
    show(sess, "拒绝修改")

    sess = svc.handle("linqiu", sid, "改成周四结束")
    show(sess, "再次提出改结束日")
    sess = svc.handle("linqiu", sid, "确认")
    show(sess, "接受修改")

    print("\n读法：已确认的天数不是聊天记录里最后一句说了算，要过确认策略。")
