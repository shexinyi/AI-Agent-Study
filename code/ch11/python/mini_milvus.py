"""
教学用 MiniMilvus：把 Collection / Partition / Index / Metric
以及 insert → flush → index → load → search 做成可观察的状态机。

不是 Milvus 内核。IVF 用向量前几维的符号分桶，不是 k-means。
生产请用 pymilvus 连 Lite / Standalone / 集群。
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Literal

from embed import DIM, dot

Metric = Literal["COSINE", "L2", "IP"]
IndexType = Literal["FLAT", "IVF_FLAT"]


class NotLoaded(RuntimeError):
    pass


class NotFlushed(RuntimeError):
    pass


def distance(metric: Metric, a: list[float], b: list[float]) -> float:
    """统一成「越小越近」，便于排序。"""
    if metric == "COSINE":
        return 1.0 - dot(a, b)
    if metric == "L2":
        return sum((x - y) ** 2 for x, y in zip(a, b))
    if metric == "IP":
        return -dot(a, b)
    raise ValueError(metric)


def ivf_bucket(vec: list[float], nlist: int) -> int:
    """用绝对值最大的维做桶。教具用，不是 k-means。"""
    idx = max(range(len(vec)), key=lambda i: abs(vec[i]))
    return idx % nlist


@dataclass
class Row:
    id: str
    vector: list[float]
    fields: dict[str, Any]
    flushed: bool = False
    bucket: int | None = None


@dataclass
class Collection:
    name: str
    dim: int = DIM
    metric: Metric = "COSINE"
    index_type: IndexType = "FLAT"
    nlist: int = 4
    nprobe: int = 2
    loaded: bool = False
    indexed: bool = False
    partitions: dict[str, list[Row]] = field(default_factory=lambda: {"_default": []})


class MiniMilvus:
    def __init__(self) -> None:
        self.collections: dict[str, Collection] = {}

    def create_collection(
        self,
        name: str,
        dim: int = DIM,
        metric: Metric = "COSINE",
        index_type: IndexType = "FLAT",
        nlist: int = 4,
        nprobe: int = 2,
    ) -> Collection:
        col = Collection(name, dim, metric, index_type, nlist, nprobe)
        self.collections[name] = col
        return col

    def create_partition(self, name: str, partition: str) -> None:
        col = self.collections[name]
        col.partitions.setdefault(partition, [])

    def insert(self, name: str, row_id: str, vector: list[float], fields: dict[str, Any], partition: str = "_default") -> None:
        col = self.collections[name]
        if len(vector) != col.dim:
            raise ValueError(f"维度不一致: 集合 {col.dim} 收到 {len(vector)}")
        col.partitions.setdefault(partition, [])
        col.partitions[partition] = [r for r in col.partitions[partition] if r.id != row_id]
        col.partitions[partition].append(Row(row_id, vector, fields, flushed=False))
        col.indexed = False
        col.loaded = False

    def flush(self, name: str) -> int:
        col = self.collections[name]
        n = 0
        for rows in col.partitions.values():
            for row in rows:
                if not row.flushed:
                    row.flushed = True
                    n += 1
        return n

    def create_index(self, name: str) -> None:
        col = self.collections[name]
        for rows in col.partitions.values():
            for row in rows:
                if row.flushed:
                    row.bucket = ivf_bucket(row.vector, col.nlist)
        col.indexed = True

    def load(self, name: str) -> None:
        col = self.collections[name]
        if not col.indexed:
            raise RuntimeError("索引未建：先 create_index，再 load（生产里未 load 的集合不能搜）")
        col.loaded = True

    def search(
        self,
        name: str,
        vector: list[float],
        top_k: int = 3,
        partition: str | None = None,
        where: dict[str, Any] | None = None,
        nprobe: int | None = None,
        include_unflushed: bool = False,
    ) -> tuple[list[tuple[float, Row]], dict[str, int]]:
        col = self.collections[name]
        if not col.indexed:
            raise RuntimeError("索引未建：先 create_index。Lite/集群都不会在空索引上给你「刚好能用」的错觉。")
        if not col.loaded:
            raise NotLoaded("集合未 load。Milvus 重启后也要重新 load，否则搜不到。")
        parts = [partition] if partition else [p for p, rows in col.partitions.items() if rows]
        candidates: list[Row] = []
        skipped_unflushed = 0
        skipped_ivf = 0
        probe = nprobe if nprobe is not None else col.nprobe
        q_bucket = ivf_bucket(vector, col.nlist)
        allowed = {(q_bucket + i) % col.nlist for i in range(probe)}

        for part in parts:
            for row in col.partitions.get(part, []):
                if not row.flushed and not include_unflushed:
                    skipped_unflushed += 1
                    continue
                if where and any(row.fields.get(k) != v for k, v in where.items()):
                    continue
                if col.index_type == "IVF_FLAT" and row.bucket not in allowed:
                    skipped_ivf += 1
                    continue
                candidates.append(row)

        ranked = sorted(((distance(col.metric, vector, r.vector), r) for r in candidates), key=lambda x: x[0])
        stats = {
            "compared": len(candidates),
            "skipped_unflushed": skipped_unflushed,
            "skipped_ivf": skipped_ivf,
            "partitions": len(parts),
            "nprobe": probe,
            "q_bucket": q_bucket,
        }
        return ranked[:top_k], stats

    def count(self, name: str) -> int:
        return sum(len(rows) for rows in self.collections[name].partitions.values())

    def save(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        blob: dict[str, Any] = {}
        for name, col in self.collections.items():
            blob[name] = {
                "dim": col.dim,
                "metric": col.metric,
                "index_type": col.index_type,
                "nlist": col.nlist,
                "nprobe": col.nprobe,
                "indexed": col.indexed,
                "partitions": {
                    p: [
                        {
                            "id": r.id,
                            "vector": r.vector,
                            "fields": r.fields,
                            "flushed": r.flushed,
                            "bucket": r.bucket,
                        }
                        for r in rows
                    ]
                    for p, rows in col.partitions.items()
                },
            }
        path.write_text(json.dumps(blob, ensure_ascii=False), encoding="utf-8")

    @classmethod
    def open(cls, path: Path) -> "MiniMilvus":
        db = cls()
        raw = json.loads(path.read_text(encoding="utf-8"))
        for name, spec in raw.items():
            col = db.create_collection(
                name,
                dim=spec["dim"],
                metric=spec["metric"],
                index_type=spec["index_type"],
                nlist=spec["nlist"],
                nprobe=spec["nprobe"],
            )
            col.indexed = spec["indexed"]
            col.loaded = False
            col.partitions = {}
            for p, rows in spec["partitions"].items():
                col.partitions[p] = [
                    Row(r["id"], r["vector"], r["fields"], r["flushed"], r["bucket"]) for r in rows
                ]
        return db
