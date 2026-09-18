"""检索指标与生成指标分开算。金标来自独立 JSON，不写进检索函数。"""

from __future__ import annotations

import json
from pathlib import Path

from rag_kit import Chunk, Hit, extractive_answer, retrieve, visible


def load_gold(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def hit_at_k(hits: list[Hit], gold: list[str], k: int) -> float:
    top = {h.chunk.heading for h in hits[:k]}
    return 1.0 if top & set(gold) else 0.0


def recall_at_k(hits: list[Hit], gold: list[str], k: int) -> float:
    if not gold:
        return 1.0
    top = {h.chunk.heading for h in hits[:k]}
    return len(top & set(gold)) / len(gold)


def mrr_score(hits: list[Hit], gold: list[str]) -> float:
    for i, hit in enumerate(hits, start=1):
        if hit.chunk.heading in gold:
            return 1.0 / i
    return 0.0


def leaked(hits: list[Hit], forbidden: list[str]) -> bool:
    return any(h.chunk.heading in forbidden for h in hits)


def answer_correct(answer: str, must: list[str], must_not: list[str]) -> bool:
    return all(p in answer for p in must) and not any(p in answer for p in must_not)


def faithful(answer: str, hits: list[Hit]) -> bool:
    """教学忠实度：答案里的每个句子必须能在某块原文里找到。"""
    if answer.startswith("【不足】"):
        return True
    body = answer.replace("。", "。\n")
    sentences = [s.strip() for s in body.splitlines() if s.strip()]
    corpus = "\n".join(h.chunk.text for h in hits)
    for sent in sentences:
        sent = sent.rstrip("。")
        if sent and sent not in corpus:
            return False
    return True


def eval_case(
    case: dict,
    chunks: list[Chunk],
    pipeline: str,
    k: int,
    extra: str = "",
    drop_deprecated: bool = True,
    apply_acl: bool = True,
) -> dict:
    pool = visible(chunks, case.get("role", "user"), drop_deprecated, apply_acl)
    hits = retrieve(pool, case["query"], pipeline, top_k=k)
    gold = case["gold_headings"]
    answer = extractive_answer(hits, extra)
    return {
        "id": case["id"],
        "query": case["query"],
        "pipeline": pipeline,
        "headings": [h.chunk.heading for h in hits],
        "hit@1": hit_at_k(hits, gold, 1),
        "hit@k": hit_at_k(hits, gold, k),
        "recall@k": recall_at_k(hits, gold, k),
        "mrr": mrr_score(hits, gold),
        "leak": leaked(hits, case.get("forbidden_headings", [])),
        "correct": answer_correct(answer, case["must_contain"], case["must_not_contain"]),
        "faithful": faithful(answer, hits),
        "answer": answer,
    }


def eval_suite(
    gold: dict,
    chunks: list[Chunk],
    pipeline: str,
    extra: str = "",
    drop_deprecated: bool = True,
    apply_acl: bool = True,
) -> list[dict]:
    k = int(gold.get("k", 3))
    return [eval_case(c, chunks, pipeline, k, extra, drop_deprecated, apply_acl) for c in gold["cases"]]


def macro(rows: list[dict], key: str) -> float:
    return sum(float(r[key]) for r in rows) / max(len(rows), 1)


def dump_table(title: str, rows: list[dict]) -> None:
    print(f"\n=== {title} ===")
    print(f"{'id':12} {'H@1':5} {'H@3':5} {'MRR':6} {'正确':4} {'忠实':4} {'泄漏':4}  Top")
    for row in rows:
        leak = "是" if row["leak"] else "否"
        ok = "Y" if row["correct"] else "N"
        faith = "Y" if row["faithful"] else "N"
        print(
            f"{row['id']:12} {row['hit@1']:<5.0f} {row['hit@k']:<5.0f} {row['mrr']:<6.2f} "
            f"{ok:4} {faith:4} {leak:4}  {row['headings'][:3]}"
        )
    print(
        f"{'MACRO':12} {macro(rows, 'hit@1'):<5.2f} {macro(rows, 'hit@k'):<5.2f} "
        f"{macro(rows, 'mrr'):<6.2f} {macro(rows, 'correct'):<4.2f} {macro(rows, 'faithful'):<4.2f}"
    )
