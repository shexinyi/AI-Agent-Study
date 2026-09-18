"""
第 12 章示例 3：落盘、租户隔离、乐观锁。

    python 03_store_and_isolation.py
"""

from __future__ import annotations

from pathlib import Path

from session import FileStore, MemoryStore, SessionService, VersionConflict

DATA = Path(__file__).resolve().parent / "data" / "sessions.json"


def isolation_demo() -> None:
    print("=== 隔离：同一进程里两个人 ===")
    store = MemoryStore()
    svc = SessionService(store)
    leave = svc.start("linqiu", "sess-leave")
    refund = svc.start("zhangsan", "sess-refund")
    svc.handle("linqiu", leave.session_id, "我是林秋，下周一到周三请假")
    svc.handle("zhangsan", refund.session_id, "ORDER-88 要退款")
    try:
        svc.handle("zhangsan", leave.session_id, "把林秋的假单改了")
        print("  误伤：张三改到了林秋的会话（不该发生）")
    except PermissionError:
        print("  张三拿着林秋的 session_id 被拒绝 isolation_or_missing")
    again = store.get("linqiu", leave.session_id)
    print("  林秋槽位仍在:", again.slots["employee"].value, again.slots["days"].value)


def persist_demo() -> None:
    print("\n=== 落盘：进程重启 ===")
    if DATA.exists():
        DATA.unlink()
    svc = SessionService(FileStore(DATA))
    sess = svc.start("linqiu", "sess-disk")
    svc.handle("linqiu", sess.session_id, "我是林秋，下周一到周三请假")
    print("  写入后 phase=", FileStore(DATA).get("linqiu", "sess-disk").phase)
    svc2 = SessionService(FileStore(DATA))
    sess2 = svc2.handle("linqiu", "sess-disk", "确认")
    print("  新进程确认后 phase=", sess2.phase, "employee=", sess2.slots["employee"].value)


def version_demo() -> None:
    print("\n=== 乐观锁：两个请求读到同一版本 ===")
    store = MemoryStore()
    svc = SessionService(store)
    svc.start("linqiu", "sess-race")
    a = store.get("linqiu", "sess-race")
    b = store.get("linqiu", "sess-race")
    print(f"  两个快照都是 v={a.version}")
    store.save(a, a.version)
    print(f"  请求 A 写入成功，现在库版本={store.get('linqiu', 'sess-race').version}")
    try:
        store.save(b, b.version)
        print("  请求 B 也写入了（不该发生）")
    except VersionConflict as exc:
        print(f"  请求 B 冲突: {exc}")
    print("  读法：多实例要用 Redis/DB 的版本号或 SET NX，不能各改各的内存。")


if __name__ == "__main__":
    isolation_demo()
    persist_demo()
    version_demo()
