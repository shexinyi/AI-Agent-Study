package com.agentbook.ch28;

import java.util.ArrayList;
import java.util.List;

public final class ModelCascade {
    static final List<String> QS = List.of(
            "林秋问：事假每年上限几天？",
            "林秋问：ORDER-88 能不能退？",
            "林秋问：食堂有没有红烧肉？"
    );

    public static void main(String[] args) {
        System.out.println("小模型做分类，大模型做退款决策。分流不是降智，是把贵的算力留在闸门前。");
        System.out.println("小模型不许出现 refund_order。可退 ≠ 已退。");
        System.out.println();
        Kit.resetCache();
        List<MiniBill.Run> big = new ArrayList<>();
        List<MiniBill.Run> cas = new ArrayList<>();
        for (String q : QS) {
            big.add(Kit.allLarge(q));
            cas.add(Kit.lean(q, false));
        }
        System.out.printf("%-8s %-8s %-8s %s%n", "路由", "全大", "分级", "分级模型");
        for (int i = 0; i < QS.size(); i++) {
            System.out.printf("%-8s %-8s %-8s %s%n", Kit.route(QS.get(i)),
                    big.get(i).amount(), cas.get(i).amount(), cas.get(i).model);
        }
        System.out.println();
        MiniBill.Run cafe = cas.get(2);
        MiniBill.Run order = cas.get(1);
        MiniBill.Run leave = cas.get(0);
        Kit.check("分级更便宜", cas.stream().mapToDouble(MiniBill.Run::amount).sum()
                < big.stream().mapToDouble(MiniBill.Run::amount).sum());
        Kit.check("食堂不上大模型", cafe.lines.stream().noneMatch(l -> "large".equals(l.model)));
        Kit.check("退款问法用了大模型", order.lines.stream().anyMatch(l -> "large".equals(l.model)));
        Kit.check("事假只走小模型", leave.lines.stream().allMatch(l -> "small".equals(l.model) || "-".equals(l.model)));
        Kit.check("没有执行退款工具", java.util.stream.Stream.concat(cas.stream(), big.stream())
                .noneMatch(r -> r.tools.contains("refund_order")));
        System.out.println("读法：分级看的是动作风险，不是「小模型更笨」。资金决策仍要大模型 + HITL。");
    }
}
