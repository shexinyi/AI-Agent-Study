"""
第 10 章示例 2：集合上的写入、Top-K、元数据过滤、删除。

    python 02_chroma_crud.py
"""

from __future__ import annotations

import shutil
from pathlib import Path

from store import open_store

DATA = Path(__file__).resolve().parent / "data" / "crud"


DOCS = [
    (
        "linqiu-pref",
        "我喜欢窗口靠过道的工位",
        {"user_id": "linqiu", "kind": "preference"},
    ),
    (
        "linqiu-order",
        "ORDER-88 未发货，用户询问能否退款",
        {"user_id": "linqiu", "kind": "episode"},
    ),
    (
        "zhangsan-pref",
        "张三喜欢靠墙的工位，不要过道",
        {"user_id": "zhangsan", "kind": "preference"},
    ),
    (
        "linqiu-secret-should-not",
        "我的密码是 123456",
        {"user_id": "linqiu", "kind": "secret"},
    ),
]


def dump(title: str, hits) -> None:
    print(f"\n=== {title} ===")
    if not hits:
        print("  （空）")
        return
    for hit in hits:
        print(f"  d={hit.distance:.3f}  {hit.id:22}  {hit.metadata.get('user_id')}  {hit.document}")


if __name__ == "__main__":
    if DATA.exists():
        shutil.rmtree(DATA)
    store, backend = open_store(DATA)
    print(f"后端: {backend}  目录: {DATA}")

    store.upsert(
        ids=[d[0] for d in DOCS],
        documents=[d[1] for d in DOCS],
        metadatas=[d[2] for d in DOCS],
    )
    print(f"写入 {store.count()} 条（演示里故意把密码也写进去，下一例会在闸门拦住）")

    dump("查询「工位靠哪边」Top-3，不过滤", store.query("工位靠哪边", n_results=3))
    dump(
        "同一查询 + where user_id=linqiu",
        store.query("工位靠哪边", n_results=3, where={"user_id": "linqiu"}),
    )
    dump(
        "同一查询 + where user_id=zhangsan",
        store.query("工位靠哪边", n_results=3, where={"user_id": "zhangsan"}),
    )

    store.delete(ids=["linqiu-secret-should-not"])
    print(f"\n删除密码条后 count={store.count()}")
    dump("再查「密码」", store.query("密码 123456", n_results=2))
    print("\n读法：相似度会跨用户；租户隔离靠 metadata，不靠「模型自己知道」。")
