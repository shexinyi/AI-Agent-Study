"""
第 11 章示例 1：四个名词 + 三种度量必须和集合一致。

    python 01_four_nouns.py
"""

from __future__ import annotations

from embed import embed
from mini_milvus import distance

A = "我喜欢窗口靠过道的工位"
B = "工位请安排在走廊一侧"
VA, VB = embed(A), embed(B)


def show(metric: str) -> None:
    d = distance(metric, VA, VB)  # type: ignore[arg-type]
    print(f"  {metric:7}  距离(越小越近)={d:+.3f}")


if __name__ == "__main__":
    print("=== 一张生产集合的最低配置 ===")
    print("Collection : agent_memory          # 一张「表」")
    print("Partition  : tenant=acme / user=linqiu   # 先剪货架，再比距离")
    print("Index      : FLAT | IVF_FLAT | HNSW      # 怎么加速，会不会漏")
    print("Metric     : COSINE | L2 | IP            # 和 Embedding 绑定，写进集合")
    print()
    print("两句座位话在三种度量下的距离（向量已 L2 归一化）：")
    for metric in ("COSINE", "L2", "IP"):
        show(metric)
    print()
    print("读法：同一对点，三种度量数字不同，排序也可能不同。")
    print("集合用 COSINE 建成，查询却按 L2 理解，就是在看一张画错比例尺的地图。")
    print()
    print("=== 索引选型（教学口径）===")
    print("FLAT     全比一遍，召回高，数据小或 Lite 常用")
    print("IVF_FLAT 先入桶再比 nprobe 个桶，快，但 nprobe 太小会漏")
    print("HNSW     图索引，生产常见默认；本章用 IVF 演示「漏检」")
    print("AUTOINDEX 托管服务代选。简历里要写清你选了什么、为什么")
