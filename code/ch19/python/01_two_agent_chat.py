"""示例 1：两名可对话智能体对打，直到 TERMINATE。"""

from kit import policy_agent
from minichat import GroupChat


def select(messages, agents):
    names = {a.name for a in messages}
    if "政策岗" not in names:
        return next(a for a in agents if a.name == "政策岗")
    return None


if __name__ == "__main__":
    chat = GroupChat([policy_agent()], select, max_round=4)
    print("两人对打：用户一问，政策岗一答就停。这是 AutoGen 最薄的形状。\n")
    for q in ("事假上限几天", "年假能卖吗"):
        print("--------")
        chat.run(q)
        print()
    print("读法：多智能体首先是一场对话，不是一条链，也不是一张图。")
