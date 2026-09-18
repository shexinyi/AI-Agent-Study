"""
第 15 章示例 3：多召回 → 重排 → 带 [n] 的引用回答。

    python 03_rerank_and_cite.py
"""

from __future__ import annotations

from pathlib import Path

from retrieve import (
    answer_with_citations,
    bm25_search,
    build_corpus,
    dump,
    rerank,
    rrf_fuse,
    vector_search,
)

CHUNKS = build_corpus(Path(__file__).resolve().parent / "kb")


def pipeline(question: str) -> None:
    print(f"\n######## {question} ########")
    pool = rrf_fuse(
        [vector_search(CHUNKS, question, 4), bm25_search(CHUNKS, question, 4)],
        top_k=5,
    )
    dump("融合池（重排前）", pool)
    ranked = rerank(question, pool)
    dump("教学重排后", ranked)
    print()
    print(answer_with_citations(question, ranked[:2]))


if __name__ == "__main__":
    pipeline("ORDER-88 能不能退")
    pipeline("TZ123 是哪张单")
    pipeline("事假上限几天")
    print("\n读法：重排是第二道闸。回答里的每句事实必须能指回 [n]，指不回去就删。")
