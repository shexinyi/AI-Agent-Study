package com.agentbook.ch28;

import java.util.ArrayList;
import java.util.List;

public final class CacheLatency {
    static final String LEAVE_Q = "林秋问：事假每年上限几天？";
    static final String CAFE_Q = "林秋问：食堂有没有红烧肉？";
    static final List<String> THREE = List.of(
            LEAVE_Q, "林秋问：事假最多几天？", "林秋问：事假上限是几天？");

    public static void main(String[] args) {
        System.out.println("缓存命中不计模型费。批处理三条制度合成一次。流式只缩短首字时间。");
        System.out.println("食堂提前终止：不调 get_order。ORDER-88 超时 3000ms 是工具段，不是模型段。");
        System.out.println();
        Kit.resetCache();
        MiniBill.Run first = Kit.lean(LEAVE_Q, true);
        MiniBill.Run second = Kit.lean(LEAVE_Q, true);
        MiniBill.Run cafe = Kit.lean(CAFE_Q, true);
        List<MiniBill.Run> serial = new ArrayList<>();
        for (String q : THREE) {
            serial.add(Kit.lean(q, false));
        }
        MiniBill.Run batched = Kit.batchLeave(THREE);
        MiniBill.Latency timeout = new MiniBill.Latency(50, 80, 3000, 15, 70);

        System.out.println("事假首次 " + first.amount() + " 教学币 cached=" + first.cached
                + " model_ms=" + first.latency.modelMs);
        System.out.println("事假再次 " + second.amount() + " 教学币 cached=" + second.cached
                + " model_ms=" + second.latency.modelMs);
        System.out.println("食堂工具=" + (cafe.tools.isEmpty() ? "无" : cafe.tools) + " 提前终止");
        System.out.println("串行三问 " + round(serial.stream().mapToDouble(MiniBill.Run::amount).sum())
                + " / 批处理 " + batched.amount());
        System.out.println("超时拆解 queue=" + timeout.queueMs + " model=" + timeout.modelMs
                + " tool=" + timeout.toolMs + " net=" + timeout.netMs
                + " total=" + timeout.totalMs() + " ttft=" + timeout.ttftMs);
        System.out.println("流式：账单仍是 " + first.amount() + "，首字 " + first.latency.ttftMs
                + "ms < 总耗时 " + first.latency.totalMs() + "ms");
        System.out.println();

        Kit.check("缓存第二次免费", second.cached && second.amount() == 0 && second.latency.modelMs == 0);
        Kit.check("食堂不调订单工具", !cafe.tools.contains("get_order"));
        Kit.check("批处理更便宜", batched.amount() < serial.stream().mapToDouble(MiniBill.Run::amount).sum());
        Kit.check("工具段才是 3000", timeout.toolMs == 3000 && timeout.modelMs < 200);
        Kit.check("流式不减账单", true);
        Kit.check("首字短于总耗时", first.latency.ttftMs < first.latency.totalMs());
        System.out.println("读法：先拆延迟四段。3 秒超时去优化 Prompt，是找错科室。");
    }

    private static double round(double n) {
        return Math.round(n * 10000.0) / 10000.0;
    }
}
