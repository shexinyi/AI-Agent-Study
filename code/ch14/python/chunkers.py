"""四种教学切块。语义切 = 按句合并，不是 embedding 聚类。"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

from embed import cosine, embed


@dataclass
class Chunk:
    chunk_id: str
    text: str
    embedding: list[float]
    meta: dict[str, Any] = field(default_factory=dict)

    def preview(self, n: int = 36) -> str:
        one = re.sub(r"\s+", " ", self.text).strip()
        return one if len(one) <= n else one[:n] + "…"


def chunk_whole(text: str, source: str = "doc") -> list[Chunk]:
    return [_make("whole-1", text, source, "whole", "document")]


def chunk_by_heading(text: str, source: str = "doc") -> list[Chunk]:
    heading = source
    buf: list[str] = []
    out: list[Chunk] = []
    n = 0

    def flush() -> None:
        nonlocal n
        body = "\n".join(buf).strip()
        if not body:
            return
        n += 1
        out.append(_make(f"head-{n}", f"{heading}。{body}", source, "heading", heading))

    for line in text.splitlines():
        if line.startswith("## "):
            flush()
            heading = line[3:].strip()
            buf = []
        elif line.startswith("# "):
            continue
        else:
            buf.append(line)
    flush()
    return out


def chunk_window(text: str, size: int, overlap: int, source: str = "doc") -> list[Chunk]:
    if overlap >= size:
        raise ValueError("overlap 必须小于 size")
    step = size - overlap
    compact = re.sub(r"\n+", "\n", text).strip()
    out: list[Chunk] = []
    i, n = 0, 0
    while i < len(compact):
        piece = compact[i : i + size]
        if piece.strip():
            n += 1
            out.append(_make(f"win-{n}", piece, source, "window", f"{i}:{i + len(piece)}"))
        if i + size >= len(compact):
            break
        i += step
    return out


def chunk_semantic(text: str, target: int = 80, source: str = "doc") -> list[Chunk]:
    """按句号切开再拼到约 target 字。教学用，不是语义向量聚类。"""
    sentences = [s.strip() for s in re.split(r"(?<=[。！？\n])", text) if s.strip()]
    out: list[Chunk] = []
    buf = ""
    n = 0
    for sent in sentences:
        if buf and len(buf) + len(sent) > target:
            n += 1
            out.append(_make(f"sem-{n}", buf, source, "semantic", "sentence-merge"))
            buf = sent
        else:
            buf += sent
    if buf.strip():
        n += 1
        out.append(_make(f"sem-{n}", buf, source, "semantic", "sentence-merge"))
    return out


def retrieve(chunks: list[Chunk], query: str, top_k: int = 2, ascii_only: bool = False) -> list[tuple[float, Chunk]]:
    q = embed(query, ascii_only=ascii_only)
    ranked = sorted(((cosine(q, c.embedding), c) for c in chunks), key=lambda x: x[0], reverse=True)
    return ranked[:top_k]


def retrieve_filtered(
    chunks: list[Chunk],
    query: str,
    top_k: int = 2,
    where: dict[str, Any] | None = None,
) -> list[tuple[float, Chunk]]:
    allowed = chunks
    if where:
        allowed = [c for c in chunks if all(c.meta.get(k) == v for k, v in where.items())]
    return retrieve(allowed, query, top_k=top_k)


def _make(cid: str, text: str, source: str, strategy: str, heading: str, ascii_only: bool = False) -> Chunk:
    return Chunk(
        cid,
        text,
        embed(text, ascii_only=ascii_only),
        {
            "source": source,
            "strategy": strategy,
            "heading": heading,
            "acl": "hr",
            "version": "2026-03",
            "kind": "policy",
        },
    )
