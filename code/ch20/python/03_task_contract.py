"""示例 3：任务的 expected 是合同；对照 AutoGen「聊完再看感觉」。"""

from kit import expected_ok, manager, researcher, writer
from minicrew import Crew, Process, Task

CASES = [
    ("事假上限几天", "must_10"),
    ("ORDER-88 能不能退", "must_88"),
    ("TZ123 是哪张单", "must_99"),
    ("食堂几点开门", "must_stop"),
]


def crew() -> Crew:
    leave, order, pen, boss = researcher("leave"), researcher("order"), writer(), manager()
    return Crew(
        [leave, order, pen],
        [Task("research", "检索对口资料", "ctx"), Task("write", "写回答", "ans", agent=pen)],
        Process.HIERARCHICAL,
        manager=boss,
    )


if __name__ == "__main__":
    c = crew()
    print("任务合同：expected 用来验收，不是写在 System Prompt 里盼模型自觉。\n")
    failed = []
    for q, exp in CASES:
        ans = c.kickoff({"question": q})
        ok = expected_ok(ans, exp)
        print(f"{'OK' if ok else 'NG'}  {q}")
        print(f"    日志 {c.log}")
        print(f"    答   {ans}")
        if not ok:
            failed.append(q)
    print("\n对照 AutoGen：那边选的是「下一句谁开口」；这边选的是「下一件事谁负责」。")
    print("退款仍须第 18 章人工节点。Crew 的写手说「可退」≠ 已执行 refund_order。")
    if failed:
        raise SystemExit("合同失败: " + ", ".join(failed))
    print("合同全过。")
