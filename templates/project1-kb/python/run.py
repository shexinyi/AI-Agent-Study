"""项目 1 最小演示：引用 + ACL。教学假设见 README。"""

DOCS = [
    {"id": "[leave10]", "acl": "intern", "text": "事假每年累计不超过 10 个工作日。"},
    {"id": "[leave5]", "acl": "hr", "text": "【废止】事假不超过 5 天。"},
]


def ask(question: str, role: str, apply_acl: bool = True) -> str:
    visible = [d for d in DOCS if (not apply_acl) or d["acl"] == "intern" or role == "hr"]
    if "事假" not in question and "工作日" not in question:
        return "【不足】不在本项目范围。"
    if apply_acl:
        hit = next(d for d in visible if d["id"] == "[leave10]")
        return f"{hit['text']} 引用 {hit['id']}"
    leaked = next(d for d in visible if d["id"] == "[leave5]")
    return f"{leaked['text']} 引用 {leaked['id']}"


if __name__ == "__main__":
    intern = ask("事假上限几天", "intern")
    leak = ask("事假上限几天", "intern", apply_acl=False)
    refuse = ask("ORDER-88 能退吗", "intern")
    print(intern)
    print("对照病:", leak)
    print(refuse)
    assert "10 个工作日" in intern and "[leave10]" in intern
    assert "5 天" in leak
    assert "【不足】" in refuse
    print("验收 OK（教学）")
