"""
集合门面：优先用 Chroma PersistentClient；装不上时退回 JSON 文件库。

两边接口一致：upsert / query / delete / count。
教学假设：始终自行传入 embedding，不触发 DefaultEmbeddingFunction 下模型。
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from embed import cosine_distance, embed

COLLECTION = "semantic_facts"


class Hit:
    def __init__(self, id: str, document: str, metadata: dict[str, Any], distance: float) -> None:
        self.id = id
        self.document = document
        self.metadata = metadata
        self.distance = distance

    def __repr__(self) -> str:
        return f"Hit({self.id!r}, d={self.distance:.3f}, {self.document})"


def _meta_match(meta: dict[str, Any], where: dict[str, Any] | None) -> bool:
    if not where:
        return True
    return all(meta.get(k) == v for k, v in where.items())


class FileCollection:
    """Chroma 的教学替身：同一套坐标，JSON 落盘。"""

    def __init__(self, path: Path) -> None:
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.rows: dict[str, dict[str, Any]] = {}
        if self.path.exists():
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            self.rows = raw.get("rows", {})

    def _flush(self) -> None:
        self.path.write_text(
            json.dumps({"rows": self.rows}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def upsert(
        self,
        ids: list[str],
        documents: list[str],
        metadatas: list[dict[str, Any]],
    ) -> None:
        for i, doc, meta in zip(ids, documents, metadatas):
            self.rows[i] = {
                "id": i,
                "document": doc,
                "metadata": meta,
                "embedding": embed(doc),
            }
        self._flush()

    def query(self, text: str, n_results: int = 3, where: dict[str, Any] | None = None) -> list[Hit]:
        q = embed(text)
        hits = []
        for row in self.rows.values():
            if not _meta_match(row["metadata"], where):
                continue
            hits.append(
                Hit(row["id"], row["document"], row["metadata"], cosine_distance(q, row["embedding"]))
            )
        hits.sort(key=lambda h: h.distance)
        return hits[:n_results]

    def delete(self, ids: list[str]) -> None:
        for i in ids:
            self.rows.pop(i, None)
        self._flush()

    def count(self) -> int:
        return len(self.rows)


class ChromaCollection:
    def __init__(self, persist_dir: Path, name: str = COLLECTION) -> None:
        import chromadb

        persist_dir.mkdir(parents=True, exist_ok=True)
        self._client = chromadb.PersistentClient(path=str(persist_dir))
        ef = _hash_embedding_function()
        try:
            self._col = self._client.get_or_create_collection(
                name=name,
                embedding_function=ef,
                metadata={"hnsw:space": "cosine"},
            )
        except Exception:
            self._col = self._client.get_or_create_collection(
                name=name,
                embedding_function=ef,
            )

    def upsert(
        self,
        ids: list[str],
        documents: list[str],
        metadatas: list[dict[str, Any]],
    ) -> None:
        self._col.upsert(
            ids=ids,
            documents=documents,
            metadatas=metadatas,
            embeddings=[embed(doc) for doc in documents],
        )

    def query(self, text: str, n_results: int = 3, where: dict[str, Any] | None = None) -> list[Hit]:
        kwargs: dict[str, Any] = {
            "query_embeddings": [embed(text)],
            "n_results": n_results,
            "include": ["documents", "metadatas", "distances"],
        }
        if where:
            kwargs["where"] = where
        raw = self._col.query(**kwargs)
        hits: list[Hit] = []
        ids = raw.get("ids") or [[]]
        docs = raw.get("documents") or [[]]
        metas = raw.get("metadatas") or [[]]
        dists = raw.get("distances") or [[]]
        if not ids or not ids[0]:
            return []
        for i, doc, meta, dist in zip(ids[0], docs[0], metas[0], dists[0]):
            hits.append(Hit(i, doc or "", dict(meta or {}), float(dist)))
        return hits

    def delete(self, ids: list[str]) -> None:
        self._col.delete(ids=ids)

    def count(self) -> int:
        return int(self._col.count())


def _hash_embedding_function():
    from chromadb.api.types import Documents, EmbeddingFunction, Embeddings

    class HashEmbeddingFunction(EmbeddingFunction[Documents]):
        def __call__(self, input: Documents) -> Embeddings:
            return [embed(text) for text in input]

        @staticmethod
        def name() -> str:
            return "local_hash_v1"

        def get_config(self) -> dict[str, str]:
            return {"algo": "md5-ngram-64"}

        @staticmethod
        def build_from_config(config: dict[str, str]) -> "HashEmbeddingFunction":
            return HashEmbeddingFunction()

    return HashEmbeddingFunction()


def open_store(data_dir: Path, prefer_chroma: bool = True):
    """返回 (store, backend_name)。"""
    if prefer_chroma:
        try:
            store = ChromaCollection(data_dir / "chroma")
            return store, "chroma"
        except Exception as exc:
            print(f"[提示] Chroma 不可用（{type(exc).__name__}: {exc}），改用 JSON 文件库。")
            print("       未安装时：python -m pip install -r requirements.txt")
            print("       已安装仍失败时：常见原因是 Windows 策略拦截 grpcio 的 cygrpc DLL。")
    store = FileCollection(data_dir / "memory.json")
    return store, "file"
