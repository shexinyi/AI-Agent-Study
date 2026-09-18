"""
第 14 章示例 3：元数据过滤 + 中英混合语料不该用 ASCII 编目员。

    python 03_metadata_and_embed.py
"""

from __future__ import annotations

from pathlib import Path

from chunkers import Chunk, chunk_by_heading, retrieve, retrieve_filtered
from clean import clean_document, tag_kind
from embed import embed

DOC = clean_document((Path(__file__).resolve().parent / "kb" / "messy_leave.md").read_text(encoding="utf-8"))


def with_meta() -> list[Chunk]:
    chunks = chunk_by_heading(DOC)
    for ch in chunks:
        heading = str(ch.meta.get("heading", ""))
        ch.meta["kind"] = "deprecated" if "历史" in heading else tag_kind(ch.text)
        ch.meta["acl"] = "hr" if ("现行" in heading or "对照" in heading) else "all"
        ch.meta["version"] = "2024" if "历史" in heading else "2026-03"
        if "对照表" in heading:
            ch.meta["kind"] = "table"
    return chunks


def reembed(chunks: list[Chunk], ascii_only: bool) -> list[Chunk]:
    out = []
    for ch in chunks:
        out.append(
            Chunk(ch.chunk_id, ch.text, embed(ch.text, ascii_only=ascii_only), dict(ch.meta))
        )
    return out


if __name__ == "__main__":
    chunks = with_meta()
    print("=== 块元数据 ===")
    for ch in chunks:
        print(f"  {ch.chunk_id:8} acl={ch.meta['acl']:3} ver={ch.meta['version']:7} kind={ch.meta['kind']:10} {ch.meta['heading']}")

    q = "事假上限几天"
    print("\n问:", q)
    print("实习生（acl=all）Top-1:", retrieve_filtered(chunks, q, 1, {"acl": "all"})[0][1].preview(42))
    print("HR（acl=hr）Top-1:    ", retrieve_filtered(chunks, q, 1, {"acl": "hr"})[0][1].preview(42))
    print("只要 2026-03:          ", retrieve_filtered(chunks, q, 1, {"version": "2026-03"})[0][1].preview(42))

    mixed = "personal leave 上限几天"
    print("\n中英混合问:", mixed)
    full = retrieve(chunks, mixed, 1)[0]
    ascii_chunks = reembed(chunks, ascii_only=True)
    bad = retrieve(ascii_chunks, mixed, 1, ascii_only=True)[0]
    print(f"  全语种编目  sim={full[0]:.3f}  {full[1].preview(36)}")
    print(f"  仅 ASCII    sim={bad[0]:.3f}  {bad[1].preview(36)}")
    print("\n读法：权限和版本是过滤条件，不是 Embedding 自己会的。中文制度用只吃英文的模型，等于瞎编目。")
