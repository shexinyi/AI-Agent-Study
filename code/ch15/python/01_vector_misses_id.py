"""
第 15 章示例 1：稠密向量丢掉编号后，ORDER-88 / TZ123 会让位给「88 折」。

    python 01_vector_misses_id.py
"""

from __future__ import annotations

from pathlib import Path

from retrieve import bm25_search, build_corpus, dump, vector_search

CHUNKS = build_corpus(Path(__file__).resolve().parent / "kb")


if __name__ == "__main__":
    print("向量通道故意丢掉 order/tz/纯数字词项，模拟稠密模型把稀有编号当噪声。")
    print("BM25 保留全部词项。")
    for q in ("ORDER-88 能不能退", "TZ123 是哪张单"):
        print(f"\n问: {q}")
        dump("向量 Top-3", vector_search(CHUNKS, q, 3))
        dump("BM25 Top-3", bm25_search(CHUNKS, q, 3))
    print("\n读法：专有名词和单号是关键词的主场。只上向量，ORDER-88 可能输给 88 折。")
