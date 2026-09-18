"""示例 2：数据集上跑两次实验，对比，再用基线做回归。"""

from kit import CASES, clever, gated
from minismith import compare, regress, run_experiment

if __name__ == "__main__":
    print("Dataset = 锁死的 CASES。Experiment = 某只 Agent 在这张表上的一次跑分。")
    print("Compare = 逐条看谁过。Regression = 候选不得比基线少过任何一条。")
    print("这就是 LangSmith 那套名词，不必先开账号。\n")

    baseline = run_experiment("gated", CASES, gated)
    demo = run_experiment("clever", CASES, clever)
    print("对比 gated(A) vs clever(B)：")
    for line in compare(baseline, demo):
        print(" ", line)
    print()

    ok_bad, bad = regress(baseline, demo)
    print("回归 基线=gated 候选=clever：", "绿" if ok_bad else "红")
    for line in bad:
        print(" ", line)
    print()

    ok_same, same_bad = regress(baseline, baseline)
    print("回归 自己比自己：", "绿" if ok_same else "红")
    print()

    checks = [
        ("对比能看出 B 没赢", all("B赢" not in line for line in compare(baseline, demo))),
        ("clever 相对 gated 回归红灯", not ok_bad and len(bad) >= 3),
        ("自己比自己绿灯", ok_same and not same_bad),
        ("数据集条数锁死", len(CASES) == 7),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：改 Prompt / 换框架，先开新实验，再跟绿灯基线比。不许改 must 来刷分。")
