"""教学 RAG：标题切块 + 哈希向量 + BM25 + RRF + 规则重排。"""

from __future__ import annotations

import hashlib
import math
import re
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

DIM = 64
META_RE = re.compile(r"<!--\s*meta:\s*(.+?)\s*-->")


def tokens(text: str) -> list[str]:
    words = re.findall(r"[a-z0-9]+", text.lower())
    chars = [ch for ch in text if "\u4e00" <= ch <= "\u9fff"]
    bigrams = [chars[i] + chars[i + 1] for i in range(len(chars) - 1)]
    return words + chars + bigrams


def is_id_token(tok: str) -> bool:
    return bool(re.fullmatch(r"(order|tz)?\d+", tok))


def embed(text: str, drop_ids: bool = True) -> list[float]:
    vec = [0.0] * DIM
    for tok in tokens(text):
        if drop_ids and is_id_token(tok):
            continue
        digest = hashlib.md5(tok.encode("utf-8")).digest()
        idx = int.from_bytes(digest[0:2], "little") % DIM
        sign = 1.0 if digest[2] % 2 == 0 else -1.0
        vec[idx] += sign
    norm = math.sqrt(sum(x * x for x in vec)) or 1.0
    return [x / norm for x in vec]


def cosine(a: list[float], b: list[float]) -> float:
    return sum(x * y for x, y in zip(a, b))


def parse_meta(raw: str) -> dict[str, str]:
    meta = {"version": "unknown", "acl": "all", "kind": "current"}
    found = META_RE.search(raw)
    if not found:
        return meta
    for part in found.group(1).split():
        if "=" in part:
            key, value = part.split("=", 1)
            meta[key] = value
    return meta


@dataclass
class Chunk:
    chunk_id: str
    source: str
    heading: str
    text: str
    version: str
    acl: str
    kind: str
    embedding: list[float] = field(default_factory=list)
    terms: list[str] = field(default_factory=list)


@dataclass
class Hit:
    chunk: Chunk
    score: float
    channel: str
    rank: int = 0


def _make_chunk(source: str, heading: str, body: str, meta: dict[str, str], n: int) -> Chunk:
    text = f"{heading}。{body}".strip()
    return Chunk(
        f"{Path(source).stem}-{n}",
        source,
        heading,
        text,
        meta["version"],
        meta["acl"],
        meta["kind"],
        embed(text, drop_ids=True),
        tokens(text),
    )


def build_corpus(kb_dir: Path, mode: str = "heading") -> list[Chunk]:
    """
    heading: 按 ## 切（生产默认）。
    whole: 每文件一块。
    coarse: 全部 Markdown 糊成一块，用来演示切块病。
    """
    files = sorted(kb_dir.glob("*.md"))
    if mode == "coarse":
        blob = "\n\n".join(p.read_text(encoding="utf-8") for p in files)
        return [_make_chunk("ALL.md", "整库糊成一块", blob, parse_meta(blob), 1)]

    chunks: list[Chunk] = []
    for path in files:
        raw = path.read_text(encoding="utf-8")
        meta = parse_meta(raw)
        if mode == "whole":
            body = META_RE.sub("", raw)
            heading = next((ln[2:].strip() for ln in raw.splitlines() if ln.startswith("# ")), path.stem)
            chunks.append(_make_chunk(path.name, heading, body, meta, 1))
            continue

        heading = path.stem
        buf: list[str] = []
        n = 0

        def flush() -> None:
            nonlocal n
            body = "\n".join(ln for ln in buf if not ln.startswith("<!--")).strip()
            if not body:
                return
            n += 1
            chunks.append(_make_chunk(path.name, heading, body, meta, n))

        for line in raw.splitlines():
            if line.startswith("## "):
                flush()
                heading = line[3:].strip()
                buf = []
            elif line.startswith("# "):
                continue
            else:
                buf.append(line)
        flush()
    return chunks


def visible(
    chunks: list[Chunk],
    role: str,
    drop_deprecated: bool,
    apply_acl: bool = True,
) -> list[Chunk]:
    allowed = {"intern": {"all"}, "user": {"all"}, "hr": {"all", "hr"}}.get(role, {"all"})
    out: list[Chunk] = []
    for chunk in chunks:
        if drop_deprecated and chunk.kind == "deprecated":
            continue
        if apply_acl and chunk.acl not in allowed:
            continue
        out.append(chunk)
    return out


def vector_search(chunks: list[Chunk], query: str, top_k: int = 3) -> list[Hit]:
    q = embed(query, drop_ids=True)
    ranked = sorted(((cosine(q, c.embedding), c) for c in chunks), key=lambda x: x[0], reverse=True)
    return [Hit(c, s, "vector", i + 1) for i, (s, c) in enumerate(ranked[:top_k])]


def bm25_search(chunks: list[Chunk], query: str, top_k: int = 5, k1: float = 1.5, b: float = 0.75) -> list[Hit]:
    q_terms = tokens(query)
    df: Counter[str] = Counter()
    for c in chunks:
        df.update(set(c.terms))
    n_docs = max(len(chunks), 1)
    avgdl = sum(len(c.terms) for c in chunks) / n_docs
    q_tf = Counter(q_terms)
    scored: list[tuple[float, Chunk]] = []
    for c in chunks:
        tf = Counter(c.terms)
        dl = len(c.terms) or 1
        score = 0.0
        for term, qf in q_tf.items():
            if tf[term] == 0:
                continue
            idf = math.log((n_docs - df[term] + 0.5) / (df[term] + 0.5) + 1.0)
            denom = tf[term] + k1 * (1 - b + b * dl / avgdl)
            score += qf * idf * (tf[term] * (k1 + 1) / denom)
        scored.append((score, c))
    scored.sort(key=lambda x: x[0], reverse=True)
    return [Hit(c, s, "bm25", i + 1) for i, (s, c) in enumerate(scored[:top_k]) if s > 0]


def rrf_fuse(lists: list[list[Hit]], k: int = 60, top_k: int = 5) -> list[Hit]:
    bag: dict[str, tuple[float, Chunk, list[str]]] = {}
    for hits in lists:
        for hit in hits:
            add = 1.0 / (k + hit.rank)
            if hit.chunk.chunk_id not in bag:
                bag[hit.chunk.chunk_id] = (add, hit.chunk, [hit.channel])
            else:
                score, ch, chans = bag[hit.chunk.chunk_id]
                bag[hit.chunk.chunk_id] = (score + add, ch, chans + [hit.channel])
    fused = sorted(bag.values(), key=lambda x: x[0], reverse=True)
    return [Hit(ch, s, "+".join(dict.fromkeys(chans)), i + 1) for i, (s, ch, chans) in enumerate(fused[:top_k])]


def rerank(query: str, hits: list[Hit]) -> list[Hit]:
    ids = re.findall(r"ORDER-\d+|TZ\d+", query.upper())
    q_cjk = {ch for ch in query if "\u4e00" <= ch <= "\u9fff"}

    def score(hit: Hit) -> float:
        text = hit.chunk.text.upper()
        s = 0.0
        for oid in ids:
            if oid in text:
                s += 3.0
        t_cjk = {ch for ch in hit.chunk.text if "\u4e00" <= ch <= "\u9fff"}
        s += 0.15 * len(q_cjk & t_cjk)
        if "88 折" in hit.chunk.text or "会议室" in hit.chunk.text:
            s -= 2.5
        if any(oid in hit.chunk.heading.upper() for oid in ids):
            s += 1.0
        return s

    ranked = sorted(((score(h), h.chunk) for h in hits), key=lambda x: x[0], reverse=True)
    return [Hit(c, s, "rerank", i + 1) for i, (s, c) in enumerate(ranked)]


def retrieve(chunks: list[Chunk], query: str, pipeline: str, top_k: int = 3) -> list[Hit]:
    if pipeline == "vector":
        return vector_search(chunks, query, top_k)
    fused = rrf_fuse([vector_search(chunks, query, 5), bm25_search(chunks, query, 5)], top_k=5)
    if pipeline == "hybrid":
        return fused[:top_k]
    return rerank(query, fused)[:top_k]


def extractive_answer(hits: list[Hit], extra: str = "") -> str:
    if not hits:
        return "【不足】没有可引用的块。"
    return hits[0].chunk.text + extra
