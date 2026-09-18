"""示例 1：自拍分高不等于可上线。"""

from kit import CASES, clever, gated
from minismith import run_experiment


def show(exp) -> None:
    print(f"=== {exp.name} ===")
    print(
        f"通过率={exp.pass_rate:.0%}  正确={exp.rate('correctness'):.0%}  "
        f"工具={exp.rate('tool_ok'):.0%}  HITL={exp.rate('hitl_ok'):.0%}  "
        f"自拍={exp.rate('vanity'):.0%}"
    )
    for row in exp.rows:
        flag = "PASS" if row.passed else "FAIL"
        print(f"  {row.case_id:16} {flag}  正确={int(row.correctness)} 工具={int(row.tool_ok)} "
              f"HITL={int(row.hitl_ok)} 自拍={int(row.vanity)}")
        if not row.passed:
            print(f"    → {row.text}")
    print()


if __name__ == "__main__":
    print("clever 会说话：爱把可退说成已退，复合问法只答后一句。")
    print("gated 带闸门：须确认、不执行写工具、食堂【不足】。")
    print("自拍分 = 像一句完整人话。它不是上线尺子。\n")

    a = run_experiment("clever", CASES, clever)
    b = run_experiment("gated", CASES, gated)
    show(a)
    show(b)

    checks = [
        ("clever 自拍更高", a.rate("vanity") > b.rate("vanity")),
        ("clever 通过率低", a.pass_rate < 0.5),
        ("gated 全过", b.pass_rate == 1.0),
        ("gated 自拍可以低", b.rate("vanity") < 1.0),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：在线看着挺聪明，只是自拍。闸门三列才是验血。")
