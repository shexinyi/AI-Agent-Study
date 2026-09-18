"""示例 2：面试加分句是「为什么不用另外四个」。"""

from catalog import FRAMEWORKS, NEEDS, rank, why_not, winner

if __name__ == "__main__":
    for need in NEEDS:
        pick = winner(need)
        print(f"######## {need.title} ########")
        print(f"问: {need.question}")
        print(f"我选 {FRAMEWORKS[pick]}。")
        print("为什么不用其它：")
        for key, _ in rank(need)[1:]:
            print(f"  - 不用 {FRAMEWORKS[key]}：{why_not(need, key)}")
        print()
    print("读法：能讲清不用 X，比清单里写过 X 更加分。")
