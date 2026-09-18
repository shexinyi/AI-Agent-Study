"""示例 2：群聊。路由选人，订单岗开口前必须经过用户代理的工具。"""

from kit import clerk_agent, policy_agent, proxy_agent
from minichat import last_user


def select(messages, agents):
    by = {a.name: a for a in agents}
    last = messages[-1]
    q = last_user(messages)
    if last.name == "林秋":
        if any(k in q for k in ("事假", "请假", "年假", "食堂")):
            return by["政策岗"]
        return by["订单岗"]
    if last.content.startswith("请用户代理") or last.content.startswith("TOOL:"):
        return by["用户代理"]
    if last.name == "用户代理":
        return by["订单岗"]
    return None


if __name__ == "__main__":
    from minichat import GroupChat

    chat = GroupChat([policy_agent(), clerk_agent(), proxy_agent()], select, max_round=8)
    print("群聊不是大家抢麦。发言人选择（Speaker Selection）是产品规则。\n")
    for q in ("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单"):
        print("--------")
        chat.run(q)
        print()
    print("读法：角色分工。政策岗不碰单号，订单岗不编库存，用户代理只跑工具。")
