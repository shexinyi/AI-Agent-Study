"""
第 15 章示例 2：倒数排名融合（RRF），两路召回合成一路。

    python 02_hybrid_rrf.py
"""

from __future__ import annotations

from pathlib import Path

from retrieve import bm25_search, build_corpus, dump, rrf_fuse, vector_search

CHUNKS = build_corpus(Path(__file__).resolve().parent / "kb")
Q = "ORDER-88 能不能退"


if __name__ == "__main__":
    print("问:", Q)
    vec = vector_search(CHUNKS, Q, 4)
    kw = bm25_search(CHUNKS, Q, 4)
    dump("向量", vec)
    dump("BM25", kw)
    dump("RRF 融合 k=60", rrf_fuse([vec, kw], k=60, top_k=3))
    print("\n读法：融合看名次。88 折若两路都靠前，RRF 仍会把它放第一——所以还要重排。")
