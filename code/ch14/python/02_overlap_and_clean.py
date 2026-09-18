"""
第 14 章示例 2：重叠救回跨窗句子；清洗拿掉页眉和调试代码。

    python 02_overlap_and_clean.py
"""

from __future__ import annotations

from pathlib import Path

from chunkers import chunk_window, retrieve
from clean import clean_document

RAW = (Path(__file__).resolve().parent / "kb" / "messy_leave.md").read_text(encoding="utf-8")
QUERY = "累计不超过几个工作日"


def report(title: str, text: str, size: int, overlap: int) -> None:
    chunks = chunk_window(text, size=size, overlap=overlap)
    hits = retrieve(chunks, QUERY, top_k=2)
    print(f"\n=== {title}  size={size} overlap={overlap} 块数={len(chunks)} ===")
    for score, ch in hits:
        flag = []
        if "10 个工作日" in ch.text:
            flag.append("GOLD")
        if "99" in ch.text:
            flag.append("CODE99")
        if "机密" in ch.text or "作废" in ch.text:
            flag.append("HEADER")
        if "5 天" in ch.text:
            flag.append("OLD5")
        print(f"  {score:.3f}  {ch.chunk_id}  {','.join(flag) or '-'}  {ch.preview(40)}")


if __name__ == "__main__":
    print("问:", QUERY)
    report("未清洗", RAW, size=36, overlap=0)
    cleaned = clean_document(RAW)
    report("已清洗、无重叠", cleaned, size=36, overlap=0)
    report("已清洗、重叠 18", cleaned, size=36, overlap=18)
    print("\n读法：页眉和 MAX_LEAVE=99 会变成『很近的垃圾』。重叠让跨窗的 10 个工作日重新完整。")
