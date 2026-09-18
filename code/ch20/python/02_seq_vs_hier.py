"""示例 2：同一张工单，顺序流绑死研究员 vs 层级流由经理指派。"""

from kit import manager, researcher, writer
from minicrew import Crew, Process, Task


def sequential_leave_only() -> Crew:
    leave, pen = researcher("leave"), writer()
    return Crew(
        [leave, pen],
        [
            Task("research", "检索", "ctx", agent=leave),
            Task("write", "写回答", "ans", agent=pen),
        ],
        Process.SEQUENTIAL,
    )


def hierarchical() -> Crew:
    leave, order, pen, boss = researcher("leave"), researcher("order"), writer(), manager()
    return Crew(
        [leave, order, pen],
        [
            Task("research", "检索对口资料", "ctx"),
            Task("write", "写回答", "ans", agent=pen),
        ],
        Process.HIERARCHICAL,
        manager=boss,
    )


if __name__ == "__main__":
    print("顺序流把研究员写死成「事假岗」时，订单问题会检索落空。")
    seq = sequential_leave_only()
    for q in ("事假上限几天", "ORDER-88 能不能退"):
        print(f"\n[顺序] 问: {q}")
        print("答:", seq.kickoff({"question": q}))
        print("日志:", " → ".join(seq.log))

    print("\n层级流：经理看问题再指派。写手仍预绑定，避免经理把写作派给研究员。")
    hier = hierarchical()
    for q in ("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单", "食堂几点开门"):
        print(f"\n[层级] 问: {q}")
        print("答:", hier.kickoff({"question": q}))
        print("日志:", " → ".join(hier.log))
    print("\n读法：层级流多一次经理调用。选错经理，比选错一条顺序边更难查。")
