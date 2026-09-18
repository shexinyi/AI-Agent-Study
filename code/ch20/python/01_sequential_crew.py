"""示例 1：顺序流。任务列表就是流水线，上一截的输出是下一截的上下文。"""

from kit import researcher, writer
from minicrew import Crew, Process, Task

leave = researcher("leave")
pen = writer()
crew = Crew(
    [leave, pen],
    [
        Task("research", "检索事假制度", "must_10", agent=leave),
        Task("write", "根据检索写一句可引用的回答", "must_10", agent=pen),
    ],
    Process.SEQUENTIAL,
)

if __name__ == "__main__":
    print("顺序流：你是计划员。任务写死顺序，角色预先绑好。\n")
    for q in ("事假上限几天", "年假能卖吗"):
        ans = crew.kickoff({"question": q})
        print(f"问: {q}")
        print("日志:", " → ".join(crew.log))
        print("答:", ans)
        print()
    print("读法：CrewAI 的产品语言是「谁负责哪件事」，不是「下一句谁开口」。")
