"""向量 / BM25 / RRF 混合 / 教学重排 / 引用。"""

from __future__ import annotations

import math
import re
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

from embed import cosine, embed, tokens


@dataclass
class Chunk:
    chunk_id: str
    source: str
    heading: str
    text: str
    embedding: list[float] = field(default_factory=list)
    terms: list[str] = field(default_factory=list)


@dataclass
class Hit:
    chunk: Chunk
    score: float
    channel: str
    rank: int = 0

    def cite(self, n: int) -> str:
        return f"[{n}] {self.chunk.source} / {self.chunk.heading}"


def build_corpus(kb_dir: Path) -> list[Chunk]:
    chunks: list[Chunk] = []
    for path in sorted(kb_dir.glob("*.md")):
        heading = path.stem
        buf: list[str] = []
        n = 0

        def flush() -> None:
            nonlocal n
            body = "\n".join(buf).strip()
            if not body:
                return
            n += 1
            text = f"{heading}。{body}"
            chunks.append(
                Chunk(
                    f"{path.stem}-{n}",
                    path.name,
                    heading,
                    text,
                    embed(text, drop_ids=True),
                    tokens(text),
                )
            )

        for line in path.read_text(encoding="utf-8").splitlines():
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


def vector_search(chunks: list[Chunk], query: str, top_k: int = 3) -> list[Hit]:
    q = embed(query, drop_ids=True)
    ranked = sorted(((cosine(q, c.embedding), c) for c in chunks), key=lambda x: x[0], reverse=True)
    return [Hit(c, s, "vector", i + 1) for i, (s, c) in enumerate(ranked[:top_k])]


def bm25_search(chunks: list[Chunk], query: str, top_k: int = 3, k1: float = 1.5, b: float = 0.75) -> list[Hit]:
    """教学 BM25：词项 = 英文词 + 中文单字双字。k1/b 取常见默认值。"""
    q_terms = tokens(query)
    df: Counter[str] = Counter()
    for c in chunks:
        df.update(set(c.terms))
    n_docs = len(chunks)
    avgdl = sum(len(c.terms) for c in chunks) / n_docs
    scored: list[tuple[float, Chunk]] = []
    q_tf = Counter(q_terms)
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


def rrf_fuse(lists: list[list[Hit]], k: int = 60, top_k: int = 3) -> list[Hit]:
    """倒数排名融合（Reciprocal Rank Fusion）。k=60 是论文常用值。"""
    bag: dict[str, tuple[float, Chunk, list[str]]] = {}
    for hits in lists:
        for hit in hits:
            cid = hit.chunk.chunk_id
            add = 1.0 / (k + hit.rank)
            if cid not in bag:
                bag[cid] = (add, hit.chunk, [hit.channel])
            else:
                score, ch, chans = bag[cid]
                bag[cid] = (score + add, ch, chans + [hit.channel])
    fused = sorted(bag.values(), key=lambda x: x[0], reverse=True)
    return [Hit(ch, s, "+".join(dict.fromkeys(chans)), i + 1) for i, (s, ch, chans) in enumerate(fused[:top_k])]


def rerank(query: str, hits: list[Hit]) -> list[Hit]:
    """
    教学重排，不是 Cross-Encoder。
    精确 ID、汉字重叠加分；折扣口误 / 会议室等干扰减分。
    """
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


def answer_with_citations(question: str, hits: list[Hit]) -> str:
    if not hits:
        return "【不足】没有可引用的块。"
    lines = [f"问: {question}", "依据:"]
    for i, hit in enumerate(hits, start=1):
        lines.append(f"  {hit.cite(i)}  {hit.chunk.text}")
    top = hits[0]
    lines.append(f"回答: 根据{top.cite(1)}。{top.chunk.text}")
    lines.append("规则: 每句事实必须能指回 [n]；指不回去就删掉这句。")
    return "\n".join(lines)


def dump(title: str, hits: list[Hit]) -> None:
    print(f"\n=== {title} ===")
    if not hits:
        print("  （空）")
        return
    for hit in hits:
        print(f"  #{hit.rank} {hit.score:7.3f}  {hit.channel:12}  {hit.chunk.heading}")
