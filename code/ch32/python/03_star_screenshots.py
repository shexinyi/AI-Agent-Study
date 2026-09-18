"""示例 3：STAR + 三张监控截图说明 + 90 秒演示。"""

from service import PIPELINE

STAR = """1. 情境：项目 2 只能本机跑脚本，联调把手机号打进日志，实习生也能把配额打穿。
2. 任务：两周把办理流收成服务：鉴权、日配额、审计脱敏，并留下 Trace 和成本看板。
3. 行动：按 FastAPI 中间件链做深（鉴权/配额/办理/审计）；金标 5 条进 CI；fat/lean 对照上看板。
4. 结果：无 token 返回 401；intern 第 4 次 429；同一问 lean 明显低于 fat；写退款仍须 HITL。"""

SHOTS = """【截图1 鉴权】标题：401 unauthorized
画面：无 Authorization 的 POST /agent/ask；响应 body 只有 error 和 trace_id。
你要指的：审计行 question 已是 [PHONE]，没有 13800138000。

【截图2 配额】标题：429 quota_exceeded
画面：intern-lin 日配额 3/3 后的第 4 次；quota_used=3 quota_limit=3。
你要指的：200 三次办理仍未执行 refund_order。限流不是把业务办砸。

【截图3 成本+回归】标题：教学币看板 + 金标 5/5
画面：fat 检索 3、重试 2；lean 检索 1、重试 0；CI 退出 0。
你要指的：数字是教学币，不是官网价；门禁看退出码，不写 99%。"""

SCRIPT = """90 秒演示：
1. 先跑脚本——手机号在 raw= 后面，这是对照病。
2. 去掉 token——401，指着 trace_id。
3. intern 连问 4 次——第 4 次 429，看板 3/3。
4. 打开 fat/lean 看板——同一句 ORDER-88。
5. 打开金标表——5/5，强调可退 ≠ 已退。
追问：为什么简历只写 FastAPI 不写 Spring？二选一做深。Java 同事把同一条管道画成 Filter。"""


if __name__ == "__main__":
    print("简历四行 + 三张截图说明。作业是用你的监控工具重拍这三张。\n")
    print(STAR)
    print()
    print("做深的管道：", " → ".join(PIPELINE))
    print("Spring 对照：OncePerRequestFilter / HandlerInterceptor / MDC+Micrometer / Actuator")
    print()
    print(SHOTS)
    print()
    print(SCRIPT)
    print()
    checks = [
        ("四行 STAR", STAR.count("\n") >= 3 and "401" in STAR and "429" in STAR),
        ("结果含 HITL", "HITL" in STAR),
        ("三张截图都有标题", SHOTS.count("【截图") == 3),
        ("截图含脱敏和教学币", "[PHONE]" in SHOTS and "教学币" in SHOTS),
        ("演示先对照病再门禁", SCRIPT.find("脚本") < SCRIPT.find("401") < SCRIPT.find("429")),
        ("不写两个框架都很熟", "二选一" in SCRIPT),
    ]
    for name, passed in checks:
        print(f"验收 {name}: {'OK' if passed else 'FAIL'}")
        if not passed:
            raise SystemExit(1)
    print("读法：架构图上要能指出鉴权、配额、人（HITL）各在哪一条边。")
