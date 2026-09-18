"""
第 13 章示例 1：闭卷胡编 vs 先检索再摘录。

    python 01_why_rag.py
"""

from __future__ import annotations

from pathlib import Path

from rag import build_index, generate_closed_book, generate_extractive, retrieve

KB = Path(__file__).resolve().parent / "kb"
QUESTION = "事假最多几天？当天能申请吗？"


if __name__ == "__main__":
    print("问:", QUESTION)
    print("\n=== 闭卷（没有检索）===")
    print(generate_closed_book(QUESTION))
    print("  ↑ 5 天、当天申请：都是编的，和制度相反。")

    chunks = build_index(KB)
    hits = retrieve(chunks, QUESTION, top_k=2)
    print("\n=== Top-2 检索 ===")
    for score, ch in hits:
        print(f"  {score:.3f}  {ch.source} / {ch.heading}")
    print("\n=== RAG（只准引用命中块）===")
    print(generate_extractive(QUESTION, hits))
    print("\n读法：RAG 不是更聪明的模型，是先把书翻开再准许说话。")
