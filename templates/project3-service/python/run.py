"""项目 3 最小演示：401 / 429。脱敏完整对照见第 32 章。"""

TOKENS = {"intern-lin": 3}


def handle(token: str, question: str, used: dict[str, int]) -> dict:
    if token not in TOKENS:
        return {"status": 401, "trace_id": "tr-0001"}
    n = used.get(token, 0)
    if n >= TOKENS[token]:
        return {"status": 429, "quota_used": n, "quota_limit": 3}
    used[token] = n + 1
    return {"status": 200, "answer": "事假 10 个工作日。可退 ≠ 已退。"}


if __name__ == "__main__":
    used: dict[str, int] = {}
    q = "林秋事假几天（教学）"
    denied = handle("", q, used)
    ok = handle("intern-lin", q, used)
    handle("intern-lin", q, used)
    handle("intern-lin", q, used)
    fourth = handle("intern-lin", q, used)
    print(denied)
    print(ok)
    print(fourth)
    assert denied["status"] == 401
    assert ok["status"] == 200
    assert fourth["status"] == 429
    print("验收 OK（教学）")
