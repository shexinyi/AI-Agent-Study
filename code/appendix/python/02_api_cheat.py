"""示例 2：Assistants 已日落，新项目走 Responses。"""

from root import repo_root

MAP = {
    "Assistant": "Prompt",
    "Thread": "Conversation",
    "Run": "Response",
    "Run Step": "Item",
}


if __name__ == "__main__":
    text = (repo_root() / "docs" / "附录B-常用模型与接口速查.md").read_text(encoding="utf-8")
    print("接口速查（2026-09 教学快照）\n")
    for old, new in MAP.items():
        print(f"  {old} → {new}")
    print("  Assistants 关停：2026-08-26")
    print("  新项目：POST /v1/responses")
    print()
    checks = [
        ("写明日落日", "2026-08-26" in text),
        ("Responses 是首选", "Responses" in text and "新项目首选" in text),
        ("四组映射都在", all(v in text for v in MAP.values())),
        ("教学币不对账单", "不能写进报价单" in text),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：简历不要写线上还在跑 Assistants。")
