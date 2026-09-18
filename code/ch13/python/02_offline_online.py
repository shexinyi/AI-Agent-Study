"""
第 13 章示例 2：离线建索引，在线只检索。换进程后索引还在。

    python 02_offline_online.py
"""

from __future__ import annotations

from pathlib import Path

from rag import build_index, generate_extractive, load_index, retrieve, save_index

ROOT = Path(__file__).resolve().parent
KB = ROOT / "kb"
INDEX = ROOT / "data" / "index.json"


if __name__ == "__main__":
    print("=== 离线索引流 ===")
    chunks = build_index(KB)
    save_index(chunks, INDEX)
    print(f"扫描 {KB} → {len(chunks)} 块 → {INDEX.name}")
    for c in chunks:
        print(f"  {c.chunk_id:20} {c.source} / {c.heading}")

    print("\n=== 在线问答流（重新 load_index，不再读 Markdown）===")
    loaded = load_index(INDEX)
    for q in ("未发货能不能退", "靠过道工位怎么申请", "明年国庆放几天"):
        hits = retrieve(loaded, q, top_k=2)
        print(f"\n问: {q}")
        print("  命中:", ", ".join(f"{c.heading}({s:.2f})" for s, c in hits))
        print(generate_extractive(q, hits))
    print("\n读法：国庆不在三份制度里，生成器必须说不足，而不是补一个日历。")
