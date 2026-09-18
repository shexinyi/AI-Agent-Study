"""
第 14 章示例 1：同一句话，切太粗 / 切太细 / 按标题，命中完全不同。

    python 01_chunk_size.py
"""

from __future__ import annotations

from pathlib import Path

from chunkers import chunk_by_heading, chunk_whole, chunk_window, retrieve
from clean import clean_document

DOC = clean_document((Path(__file__).resolve().parent / "kb" / "messy_leave.md").read_text(encoding="utf-8"))
QUERY = "事假每年最多几天"


def report(name: str, chunks) -> None:
    hits = retrieve(chunks, QUERY, top_k=1)
    score, ch = hits[0]
    gold = "10 个工作日" in ch.text
    poison = ("5 天" in ch.text) or ("99" in ch.text)
    print(f"\n=== {name}  块数={len(chunks)} ===")
    print(f"  Top-1 sim={score:.3f}  {ch.chunk_id}")
    print(f"  预览: {ch.preview(48)}")
    print(f"  含「10 个工作日」? {gold}   掺进 5天/99? {poison}")


if __name__ == "__main__":
    print("问:", QUERY)
    print("原文已清洗。观察切块策略如何改写 Top-1。")
    report("整篇一块（太粗）", chunk_whole(DOC))
    report("固定窗 28 字 / 重叠 0（太细）", chunk_window(DOC, size=28, overlap=0))
    report("按 ## 标题", chunk_by_heading(DOC))
    print("\n读法：太粗把 5 天和 10 天捆在一起；太细和按标题都可能先命中旧稿「最多 5 天」——问句和废止句更像。所以还要 version 过滤。")
