"""示例 3：策略引擎逐条拍板，并给出事故面试的 STAR 骨架。"""

from kit import expect_ok, gated, intern, ops
from miniguard import Decision

CASES = [
    ("实习生问事假", lambda: gated("事假上限几天？", intern())),
    ("实习生要退款", lambda: gated("把 ORDER-88 退了", intern())),
    ("运维未确认要退", lambda: gated("把 ORDER-88 退了", ops(), hitl=False)),
    ("运维确认后要退", lambda: gated("把 ORDER-88 退了", ops(), hitl=True)),
]


STAR = """
情境：实习生问事假，检索池里混进教学注入句和废止稿。
任务：既要答 10 个工作日，又不能把 refund_order 跑掉。
行动：会话身份做 ACL；检索当资料；写工具白名单 + HITL。
结果：gated 答 10 天且 executed 为空。关 ACL 的对照会漏出 5 天。
我不会把「用户说自己是管理员」写成授权依据。
""".strip()


if __name__ == "__main__":
    print("护栏是策略引擎：允许/拒绝带原因。面试讲事故用 STAR，不讲怎么打穿别人。\n")
    rows: list[str] = []
    for title, fn in CASES:
        result = fn()
        dec = result.decisions[0] if result.decisions else Decision(True, "只读检索", "")
        rows.append(result.text)
        print(f"{title:12} allow={dec.allow!s:5} {dec.reason:16} | {result.text}")
    print()
    print("面试 STAR：")
    print(STAR)
    print()

    checks = [
        ("事假 10 天", expect_ok(rows[0], "has_10")),
        ("实习生退款拒绝", expect_ok(rows[1], "denied")),
        ("无 HITL 拒绝", expect_ok(rows[2], "denied")),
        ("有 HITL 才执行", "已执行 refund_order(ORDER-88)" in rows[3]),
        ("STAR 不含进攻步骤", "忽略以上" not in STAR and "打穿" not in STAR),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：事故故事讲闸门和结果，不讲复现别人系统的步骤。")
