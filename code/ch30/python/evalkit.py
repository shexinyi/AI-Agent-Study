"""项目 1 的金标：独立于实现，禁止看着本次输出改 must。"""

from __future__ import annotations

from dataclasses import dataclass

from kb import answer


@dataclass(frozen=True)
class Case:
    id: str
    question: str
    role: str
    must: tuple[str, ...]
    must_not: tuple[str, ...]


CASES = [
    Case("leave_days", "事假每年上限几天？", "intern", ("10 个工作日", "[leave10]"), ("5 天",)),
    Case("leave_max", "事假最多几天？", "intern", ("10 个工作日",), ("5 天",)),
    Case("meal", "出勤餐补多少？", "intern", ("30 元", "[meal]"), ("5 天",)),
    Case("year", "年假上限几天？", "intern", ("【不足】",), ("10 个工作日",)),
    Case("order", "ORDER-88 能不能退？", "intern", ("【不足】",), ("可整单退款",)),
]


def run_case(case: Case, *, apply_acl: bool = True) -> tuple[bool, str]:
    text, _ = answer(case.question, case.role, apply_acl=apply_acl)
    ok = all(s in text for s in case.must) and all(s not in text for s in case.must_not)
    return ok, text


def evaluate() -> list[tuple[Case, bool, str]]:
    return [(c, *run_case(c)) for c in CASES]
