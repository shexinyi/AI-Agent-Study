"""
第 9 章示例 3：长期记忆的写入过滤、冲突、遗忘。

    python 03_long_term_policy.py
"""

from __future__ import annotations

from memory import EpisodicMemory, SemanticMemory, should_remember

LINES = [
    "在吗",
    "我是凌秋，下周一到周三请假去上海办事",
    "我的密码是 123456，帮我登录邮箱",
    "我喜欢窗口靠过道的工位",
    "其实我是林秋",
]


if __name__ == "__main__":
    semantic = SemanticMemory()
    episodic = EpisodicMemory()

    print("=== 写入过滤 ===")
    for line in LINES:
        ok, reason, fact = should_remember(line)
        print(f"\n原文: {line}")
        print(f"写入? {ok}  理由: {reason}")
        if ok and fact:
            action = semantic.upsert(fact, policy="newer_wins")
            episodic.record("upsert", f"{action} {fact.key}={fact.value}")
            print(f"动作: {action}  {fact.key}={fact.value}  source={fact.source}")

    print("\n=== 当前语义事实 ===")
    for fact in semantic.facts.values():
        print(f" - {fact.key}={fact.value} ({fact.source}, c={fact.confidence})")

    print("\n检索「工位」:", [(f.key, f.value) for f in semantic.search("工位")])
    print("检索「林」:", [(f.key, f.value) for f in semantic.search("林")])

    semantic.forget("preference")
    print("\n遗忘 preference 后键集合:", list(semantic.facts))
    print("情节条数:", len(episodic.events), "（写成功才记一笔，密码那行不会出现）")
    print("\n读法：密码被拒写；偏好留下；员工名从凌秋被更正为林秋。")
