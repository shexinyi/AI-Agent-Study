"""示例 2：检索过了不等于答对；答得顺不等于忠实。"""

from pathlib import Path

from metrics import eval_case, load_gold
from rag_kit import build_corpus

HERE = Path(__file__).resolve().parent
gold = load_gold(HERE / "goldenset.json")
chunks = build_corpus(HERE / "kb", "heading")
cases = {c["id"]: c for c in gold["cases"]}
k = int(gold["k"])


def show(title: str, row: dict) -> None:
    print(f"\n######## {title} ########")
    print(f"检索 Hit@1={row['hit@1']:.0f}  Hit@3={row['hit@k']:.0f}  MRR={row['mrr']:.2f}  泄漏={'是' if row['leak'] else '否'}")
    print(f"生成 正确={row['correct']}  忠实={row['faithful']}")
    print(f"Top: {row['headings']}")
    print(f"答: {row['answer']}")


show(
    "Hit@3 过了，Top-1 错了（只向量问 ORDER-88）",
    eval_case(cases["order88"], chunks, "vector", k),
)
show(
    "检索对了，却塞进无出处的一句",
    eval_case(cases["leave_days"], chunks, "prod", k, extra="通常三天到账。"),
)
show(
    "检索对、抽取也对",
    eval_case(cases["leave_days"], chunks, "prod", k),
)

print("\n读法：两列分数要分开挂。Hit@3 过了但 Hit@1 挂 = 金标挤进了候选，生成器却抄了错的第一块。")
print("正确率过、忠实度挂 = 数字碰巧对，但多编了资料里没有的句子。")
