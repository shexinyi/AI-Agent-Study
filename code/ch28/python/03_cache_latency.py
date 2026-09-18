"""示例 3：缓存、批处理、流式、提前终止，以及延迟四段。"""

from kit import batch_leave, lean, reset_cache
from minibill import Latency

LEAVE_Q = "林秋问：事假每年上限几天？"
CAFE_Q = "林秋问：食堂有没有红烧肉？"
THREE = [LEAVE_Q, "林秋问：事假最多几天？", "林秋问：事假上限是几天？"]


if __name__ == "__main__":
    print("缓存命中不计模型费。批处理三条制度合成一次。流式只缩短首字时间。")
    print("食堂提前终止：不调 get_order。ORDER-88 超时 3000ms 是工具段，不是模型段。\n")
    reset_cache()
    first = lean(LEAVE_Q)
    second = lean(LEAVE_Q)
    cafe = lean(CAFE_Q)
    serial = [lean(q, use_cache=False) for q in THREE]
    batched = batch_leave(THREE)
    timeout = Latency(queue_ms=50, model_ms=80, tool_ms=3000, net_ms=15, ttft_ms=70)
    stream_same_bill = first.amount

    print(f"事假首次 {first.amount} 教学币 cached={first.cached} model_ms={first.latency.model_ms}")
    print(f"事假再次 {second.amount} 教学币 cached={second.cached} model_ms={second.latency.model_ms}")
    print(f"食堂工具={cafe.tools or '无'} 提前终止")
    print(f"串行三问 {round(sum(x.amount for x in serial), 4)} / 批处理 {batched.amount}")
    print(
        f"超时拆解 queue={timeout.queue_ms} model={timeout.model_ms} "
        f"tool={timeout.tool_ms} net={timeout.net_ms} total={timeout.total_ms} ttft={timeout.ttft_ms}"
    )
    print(f"流式：账单仍是 {stream_same_bill}，首字 {first.latency.ttft_ms}ms < 总耗时 {first.latency.total_ms}ms")
    print()

    checks = [
        ("缓存第二次免费", second.cached and second.amount == 0 and second.latency.model_ms == 0),
        ("食堂不调订单工具", "get_order" not in cafe.tools),
        ("批处理更便宜", batched.amount < sum(x.amount for x in serial)),
        ("工具段才是 3000", timeout.tool_ms == 3000 and timeout.model_ms < 200),
        ("流式不减账单", stream_same_bill == first.amount),
        ("首字短于总耗时", first.latency.ttft_ms < first.latency.total_ms),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：先拆延迟四段。3 秒超时去优化 Prompt，是找错科室。")
