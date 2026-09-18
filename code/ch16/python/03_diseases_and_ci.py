"""示例 3：三种常见病，以及一条会红灯的 CI 门禁。"""

from __future__ import annotations

import sys
from pathlib import Path

from metrics import dump_table, eval_case, eval_suite, load_gold, macro
from rag_kit import build_corpus

HERE = Path(__file__).resolve().parent
gold = load_gold(HERE / "goldenset.json")
heading = build_corpus(HERE / "kb", "heading")
coarse = build_corpus(HERE / "kb", "coarse")
cases = {c["id"]: c for c in gold["cases"]}
k = int(gold["k"])


def pass_row(row: dict) -> bool:
    return row["hit@1"] == 1 and row["correct"] and row["faithful"] and not row["leak"]


print("病 1 切块不当：整库糊成一块。5 天和 10 天捆在同一上下文。")
coarse_row = eval_case(cases["leave_days"], coarse, "vector", k)
print(f"  Hit@3={coarse_row['hit@k']:.0f}（金标标题已不存在）  正确={coarse_row['correct']}  答含5天? {'5 天' in coarse_row['answer']}")
print(f"  答摘录: {coarse_row['answer'][:80]}...")

print("\n病 2 过期文档：HR 能看见废止稿，若不过滤 kind=deprecated，「事假最多几天」会吃到 5 天。")
stale_case = {**cases["leave_max"], "role": "hr"}
stale = eval_case(stale_case, heading, "prod", k, drop_deprecated=False)
print(f"  Top={stale['headings']}  泄漏废止稿={stale['leak']}  正确={stale['correct']}")

print("\n病 3 权限泄漏：实习生若关掉 ACL 过滤，废止稿（acl=hr）会进 Top-K。")
leaky_case = {**cases["leave_max"], "role": "intern"}
leaky = eval_case(leaky_case, heading, "prod", k, drop_deprecated=False, apply_acl=False)
print(f"  Top={leaky['headings']}  泄漏废止稿={leaky['leak']}  正确={leaky['correct']}")

print("\n--- CI 门禁：生产管线 = 标题切 + 丢掉废止稿 + intern 只看 acl=all + 混合重排 ---")
rows = eval_suite(gold, heading, "prod", drop_deprecated=True)
dump_table("CI 金标套件", rows)

failed = [r["id"] for r in rows if not pass_row(r)]
hit = macro(rows, "hit@k")
correct = macro(rows, "correct")
print(f"\n门槛: 每条 Hit@1=1 且 正确且忠实且不泄漏。宏观 Hit@1={macro(rows, 'hit@1'):.2f} 正确率={correct:.2f}")
if failed:
    print("CI RED:", ", ".join(failed))
    sys.exit(1)
print("CI GREEN: 金标套件通过。改切块 / 过滤 / 融合后请再跑本脚本，不要只看感觉。")
