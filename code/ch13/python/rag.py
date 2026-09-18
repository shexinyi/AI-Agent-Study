"""最小 RAG：按二级标题切块 → 哈希向量 → Top-K → 抽取式生成。"""

from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

from embed import cosine, embed


@dataclass
class Chunk:
    chunk_id: str
    source: str
    heading: str
    text: str
    embedding: list[float]


def chunk_markdown(path: Path) -> list[Chunk]:
    """按二级标题切。第 14 章再谈重叠与语义切分。"""
    heading = path.stem
    buf: list[str] = []
    chunks: list[Chunk] = []
    n = 0

    def flush() -> None:
        nonlocal n
        body = "\n".join(buf).strip()
        if not body:
            return
        text = f"{heading}。{body}"
        n += 1
        chunks.append(Chunk(f"{path.stem}-{n}", path.name, heading, text, embed(text)))

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


def build_index(kb_dir: Path) -> list[Chunk]:
    chunks: list[Chunk] = []
    for path in sorted(kb_dir.glob("*.md")):
        chunks.extend(chunk_markdown(path))
    return chunks


def save_index(chunks: list[Chunk], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps([asdict(c) for c in chunks], ensure_ascii=False, indent=2), encoding="utf-8")


def load_index(path: Path) -> list[Chunk]:
    return [Chunk(**row) for row in json.loads(path.read_text(encoding="utf-8"))]


def retrieve(chunks: list[Chunk], query: str, top_k: int = 2) -> list[tuple[float, Chunk]]:
    q = embed(query)
    ranked = sorted(((cosine(q, c.embedding), c) for c in chunks), key=lambda x: x[0], reverse=True)
    return ranked[:top_k]


def overlap(query: str, text: str) -> int:
    q = {ch for ch in query if "\u4e00" <= ch <= "\u9fff"}
    t = {ch for ch in text if "\u4e00" <= ch <= "\u9fff"}
    return len(q & t)


def generate_extractive(question: str, hits: list[tuple[float, Chunk]], min_overlap: int = 3) -> str:
    """只引用命中块。重叠不够就承认资料不足——这是在对抗幻觉，不是无能。"""
    usable = [(s, c) for s, c in hits if overlap(question, c.text) >= min_overlap]
    if not usable:
        return "【不足】检索结果里没有能支撑的句子，不能编造。"
    lines = ["【依据资料】"]
    for score, chunk in usable:
        lines.append(f"- ({chunk.source} / {chunk.heading}, sim={score:.3f}) {chunk.text}")
    lines.append("【回答】请只根据上面加粗的制度原文作答，不要补充制度里没有的数字。")
    best = usable[0][1]
    lines.append(f"结论摘录：{best.text}")
    return "\n".join(lines)


def generate_closed_book(question: str) -> str:
    """闭卷胡编：专门演示『没有检索时模型会发明数字』。"""
    return f"闭卷回答（无资料）：{question} → 按惯例事假最多 5 天，当天申请即可。"
