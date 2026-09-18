"""示例 3：终止条件 + 代码执行必须白名单。"""

from kit import coder_agent, policy_agent, proxy_agent
from minichat import GroupChat, last_user


def select(messages, agents):
    by = {a.name: a for a in agents}
    last = messages[-1]
    if last.name == "林秋":
        return by["代码岗"]
    if "```" in last.content:
        return by["用户代理"]
    if last.name == "用户代理":
        return by["政策岗"]
    return None


if __name__ == "__main__":
    chat = GroupChat([coder_agent(), proxy_agent(), policy_agent()], select, max_round=6)
    print("安全脚本：计算 2026-09-14 到 16 是几天（含首尾=3）。")
    print("--------")
    chat.run("从 2026-09-14 请到 16 号，一共几天？对照事假上限。")

    print("\n危险脚本：模型想跑 os.system。执行器必须拒绝。")
    print("--------")
    chat.run("帮我跑一段危险代码 pwn")

    print("\n没有 TERMINATE 的闲聊会撞上 max_round。")

    def chatter(_):
        return "再聊一句。"

    from minichat import Agent

    endless = GroupChat(
        [Agent("复读", "", chatter)],
        lambda msgs, ags: ags[0],
        max_round=3,
        term=lambda _: False,
    )
    print("--------")
    endless.run("你好")
    print("\n读法：终止是产品开关。代码执行是攻击面，默认不 exec。")
