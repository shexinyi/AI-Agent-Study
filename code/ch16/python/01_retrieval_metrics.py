"""示例 1：同一金标，只向量 vs 混合+重排。检索指标单独看。"""

from pathlib import Path

from metrics import dump_table, eval_suite, load_gold
from rag_kit import build_corpus

HERE = Path(__file__).resolve().parent
gold = load_gold(HERE / "goldenset.json")
chunks = build_corpus(HERE / "kb", "heading")

print("金标写在 goldenset.json，不写在检索函数里。")
print("Hit@K = 金标块是否进 Top-K；MRR = 第一块金标的名次倒数。")
print("本脚本只报检索，不报「答得漂不漂亮」。")

dump_table("只向量（丢掉编号词项）", eval_suite(gold, chunks, "vector"))
dump_table("混合召回 + 教学重排", eval_suite(gold, chunks, "prod"))

print("\n读法：Hit@3 会骗人——ORDER-88 在向量里是第 3，Hit@3=1，但 Top-1 是 88 折，抽取答案就错。看 Hit@1 和 MRR。")
