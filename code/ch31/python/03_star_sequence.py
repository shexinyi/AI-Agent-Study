"""示例 3：STAR、时序提纲、90 秒演示。"""

STAR = [
    "情境：林秋同时问事假和 ORDER-88，旧客服要么只答一句，要么直接说已退款。",
    "任务：两周做成跨系统办理：查制度、查订单、写退款必须人批。",
    "行动：编排器分格；intern 不能退；ORDER-99 不建写单；食堂失败隔离；审批单可拒绝。",
    "结果：未确认的 ORDER-88 只有 pending；确认后才 refund_order；四条失败路径可当场复现。",
]

SEQ = """
时序（ORDER-88 未确认）：
林秋 → 编排器：事假 + 退款
编排器 → 事假工人：3 天 / 10 个工作日
编排器 → 订单工人：get_order(ORDER-88) 未发货
编排器 → HITL：建 pending 单
编排器 → 林秋：可退，须确认；未执行
时序（失败）：ORDER-99 无写单；intern 确认仍拒绝；食堂格 failed，另两格仍在。
""".strip()

SCRIPT = """
90 秒演示：
1. 同一句复合问法：闲聊只剩客套，办理两格都在且未执行。
2. 切换 agent_ops，不点确认——看见 pending。
3. 点拒绝——【已拒绝】。
4. ORDER-99 点批准——仍走售后，executed 为空。
5. 食堂超时——汇报里有【失败隔离】，事假和 88 还在。
追问：为什么不用 Swarm？一只话筒吃不掉两项目标。为什么不用三个 Chatbot？没有工单。
""".strip()


if __name__ == "__main__":
    print("简历四行 + 时序提纲。作业是把 SEQ 画成你自己的图。\n")
    for i, line in enumerate(STAR, 1):
        print(f"{i}. {line}")
    print()
    print(SEQ)
    print()
    print(SCRIPT)
    print()
    checks = [
        ("四行 STAR", len(STAR) == 4),
        ("结果含 pending", "pending" in STAR[3]),
        ("时序有 HITL", "HITL" in SEQ and "未执行" in SEQ),
        ("演示含 99 和隔离", "ORDER-99" in SCRIPT and "失败隔离" in SCRIPT),
        ("不写三个机器人很聪明", all("聪明" not in x for x in STAR)),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：架构图上必须能指出「人」出现在哪一条边上。")
