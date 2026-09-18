"""
第 11 章示例 3：分区剪枝 + IVF 漏检 + 和 Chroma 的选型对照。

    python 03_partition_and_ivf.py
"""

from __future__ import annotations

from embed import embed
from mini_milvus import MiniMilvus

DOCS = [
    ("linqiu", "linqiu-pref", "我喜欢窗口靠过道的工位"),
    ("linqiu", "linqiu-order", "ORDER-88 未发货，用户询问能否退款"),
    ("zhangsan", "zhang-pref", "张三喜欢靠墙的工位，不要过道"),
    ("lisi", "lisi-pref", "李四坐电梯口，要安静"),
    ("lisi", "lisi-note", "李四下周去上海出差"),
]


def fill(index_type: str, nlist: int = 4, nprobe: int = 2) -> MiniMilvus:
    db = MiniMilvus()
    db.create_collection("agent_memory", index_type=index_type, nlist=nlist, nprobe=nprobe)
    for user, rid, text in DOCS:
        db.create_partition("agent_memory", user)
        db.insert("agent_memory", rid, embed(text), {"user_id": user, "text": text}, partition=user)
    db.flush("agent_memory")
    db.create_index("agent_memory")
    db.load("agent_memory")
    return db


def dump(title: str, hits, stats) -> None:
    print(f"\n=== {title} ===")
    print(f"  compared={stats['compared']}  skipped_ivf={stats['skipped_ivf']}  "
          f"partitions={stats['partitions']}  nprobe={stats['nprobe']}")
    for dist, row in hits:
        print(f"  d={dist:.3f}  {row.id:16}  {row.fields['text']}")


if __name__ == "__main__":
    q = embed("他工位喜欢靠哪边")
    flat = fill("FLAT")
    hits, stats = flat.search("agent_memory", q, top_k=3)
    dump("FLAT + 全分区", hits, stats)
    hits, stats = flat.search("agent_memory", q, top_k=3, partition="linqiu")
    dump("FLAT + 只搜 partition=linqiu", hits, stats)

    ivf = fill("IVF_FLAT", nlist=4, nprobe=1)
    hits, stats = ivf.search("agent_memory", q, top_k=3, nprobe=1)
    dump("IVF nprobe=1（可能漏掉真近邻）", hits, stats)
    hits, stats = ivf.search("agent_memory", q, top_k=3, nprobe=4)
    dump("IVF nprobe=4（等于全桶，接近 FLAT）", hits, stats)

    print("\n=== 选型对照（教学口径，不是报价单）===")
    print("尺度     Chroma 嵌入式        Mini/Lite              集群 Milvus / 云托管")
    print("数据量   笔记本、十万内        单机百万级体验          千万到十亿")
    print("分区     metadata 过滤        Lite 官方不支持分区     一等公民 Partition")
    print("索引     主要 HNSW            Lite 多 FLAT           IVF / HNSW / 磁盘索引")
    print("一致性   进程内可见            Lite 仅 Strong         Strong / Bounded / Session")
    print("Java     HTTP 为主            仍要连服务             gRPC SDK 一等公民")
    print("运维     拷目录               一个 .db 文件          etcd + 对象存储 + 监控")
    print("\n读法：Chroma 把记忆跑起来；Milvus 把规模、分区和索引手刹交给你。")
    print("Lite 能让你熟悉 API，但不能拿 Lite 的能力清单当生产架构。")
