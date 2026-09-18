"""
第 11 章示例 2：insert → flush → index → load → search。
少一步就会「库是空的」。

    python 02_write_read_path.py
"""

from __future__ import annotations

from pathlib import Path

from embed import embed
from mini_milvus import MiniMilvus, NotLoaded

DATA = Path(__file__).resolve().parent / "data" / "write_path.json"
DOC = "我喜欢窗口靠过道的工位"


def try_search(db: MiniMilvus, title: str) -> None:
    print(f"\n--- {title} ---")
    try:
        hits, stats = db.search("agent_memory", embed("他坐哪边靠过道"), top_k=2)
        if not hits:
            print(f"  命中 0  compared={stats['compared']}  skipped_unflushed={stats['skipped_unflushed']}")
            return
        for dist, row in hits:
            print(f"  d={dist:.3f}  {row.id}  {row.fields.get('text')}")
        print(f"  compared={stats['compared']}  skipped_unflushed={stats['skipped_unflushed']}")
    except (NotLoaded, RuntimeError) as exc:
        print(f"  失败: {exc}")


if __name__ == "__main__":
    db = MiniMilvus()
    db.create_collection("agent_memory", index_type="FLAT")
    db.insert("agent_memory", "linqiu-pref", embed(DOC), {"user_id": "linqiu", "text": DOC}, partition="linqiu")
    print(f"insert 后条数={db.count('agent_memory')}  但还在 growing segment")
    try_search(db, "未 flush / 未 index / 未 load")

    n = db.flush("agent_memory")
    print(f"\nflush {n} 条 → sealed。仍不能搜。")
    try_search(db, "已 flush，未 index")

    db.create_index("agent_memory")
    try_search(db, "已建索引，未 load")

    db.load("agent_memory")
    try_search(db, "load 之后")

    db.save(DATA)
    db2 = MiniMilvus.open(DATA)
    print(f"\n模拟进程重启，从 {DATA.name} 打开。loaded 被清掉。")
    try_search(db2, "重启后直接搜")
    db2.create_index("agent_memory")
    db2.load("agent_memory")
    try_search(db2, "重启后重新 index + load")
    print("\n读法：生产事故第一名不是算法，是忘了 load。")
