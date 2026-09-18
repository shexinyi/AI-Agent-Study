"""示例 2：全程大模型 vs 小模型分流。食堂不上大模型；退款决策才上。"""

from kit import all_large, lean, reset_cache, route

QS = [
    "林秋问：事假每年上限几天？",
    "林秋问：ORDER-88 能不能退？",
    "林秋问：食堂有没有红烧肉？",
]


if __name__ == "__main__":
    print("小模型做分类，大模型做退款决策。分流不是降智，是把贵的算力留在闸门前。")
    print("小模型不许出现 refund_order。可退 ≠ 已退。\n")
    reset_cache()
    big = [all_large(q) for q in QS]
    cas = [lean(q, use_cache=False) for q in QS]
    print(f"{'问法':8} {'路由':6} {'全大':8} {'分级':8} 分级模型")
    for q, a, b in zip(QS, big, cas):
        print(f"{route(q):8} {route(q):6} {a.amount:<8} {b.amount:<8} {b.model}")
    print()

    cafe = cas[2]
    order = cas[1]
    leave = cas[0]
    checks = [
        ("分级更便宜", sum(x.amount for x in cas) < sum(x.amount for x in big)),
        ("食堂不上大模型", all(ln.model != "large" for ln in cafe.lines)),
        ("退款问法用了大模型", any(ln.model == "large" for ln in order.lines)),
        ("事假只走小模型", all(ln.model in ("small", "-") for ln in leave.lines)),
        ("没有执行退款工具", all("refund_order" not in x.tools for x in cas + big)),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：分级看的是动作风险，不是「小模型更笨」。资金决策仍要大模型 + HITL。")
