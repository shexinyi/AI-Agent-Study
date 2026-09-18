"""
第 10 章示例 3：写入闸门 + 重启后仍能检索。

    python 03_remember_across_restart.py
"""

from __future__ import annotations

import shutil
from pathlib import Path

from remember import should_remember
from store import open_store

DATA = Path(__file__).resolve().parent / "data" / "ltm"
USER = "linqiu"

LINES = [
    "在吗",
    "我是凌秋，下周一到周三请假去上海办事",
    "我的密码是 123456，帮我登录邮箱",
    "我喜欢窗口靠过道的工位",
    "其实我是林秋",
]


def ingest(store) -> None:
    print("=== 写入（带闸门）===")
    for line in LINES:
        ok, reason, fact = should_remember(line)
        print(f"\n原文: {line}")
        print(f"写入? {ok}  理由: {reason}")
        if not ok or fact is None:
            continue
        doc_id = f"{USER}:{fact.key}"
        store.upsert(
            ids=[doc_id],
            documents=[f"{fact.key}={fact.value}"],
            metadatas=[
                {
                    "user_id": USER,
                    "kind": fact.key,
                    "source": fact.source,
                    "confidence": fact.confidence,
                }
            ],
        )
        print(f"upsert {doc_id}")


if __name__ == "__main__":
    if DATA.exists():
        shutil.rmtree(DATA)

    store, backend = open_store(DATA)
    print(f"后端: {backend}  第一次打开")
    ingest(store)
    print(f"\n落盘后条数: {store.count()}")
    del store

    store2, backend2 = open_store(DATA)
    print(f"\n=== 重启进程，重新打开（{backend2}）===")
    print(f"条数仍是: {store2.count()}")
    hits = store2.query("他工位喜欢靠哪边", n_results=3, where={"user_id": USER})
    print("查询「他工位喜欢靠哪边」:")
    for hit in hits:
        print(f"  d={hit.distance:.3f}  {hit.id}  {hit.document}")

    store2.delete(ids=[f"{USER}:preference"])
    print(f"\n遗忘 preference 后条数: {store2.count()}")
    hits2 = store2.query("工位靠过道", n_results=3, where={"user_id": USER})
    print("再查工位：")
    for hit in hits2:
        print(f"  d={hit.distance:.3f}  {hit.id}  {hit.document}")
    print("\n读法：密码从未入库；偏好跨「重启」仍在；forget 后向量也要删。")
