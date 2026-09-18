"""示例 1：用约束给五种默认语言打分。"""

from catalog import FRAMEWORKS, NEEDS, rank

if __name__ == "__main__":
    print("权重是课堂约定：HITL/回边偏图，对打偏圆桌，工种流水线偏排班，三天演示才看托管。")
    print("不是官方排名。\n")
    header = f"{'场景':16} " + " ".join(f"{k:>7}" for k in FRAMEWORKS) + "  首选"
    print(header)
    for need in NEEDS:
        scores = dict(rank(need))
        pick = rank(need)[0][0]
        cells = " ".join(f"{scores[k]:7d}" for k in FRAMEWORKS)
        print(f"{need.id:16} {cells}  {FRAMEWORKS[pick]}")
        print(f"  {need.title} / {need.question}")
    print("\n读法：分最高的是「约束下的默认语言」，不是唯一真理。换约束，分会翻。")
