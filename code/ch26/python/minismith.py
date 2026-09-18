"""评测教具：数据集、实验、对比、回归。

教学假设：评估器是规则，不是 LLM-as-Judge。不接 LANGSMITH_API_KEY。
名词对齐 LangSmith 的 Dataset / Experiment / Evaluator，不是官方 SDK。
金标锁在 kit.CASES，禁止看着本次输出改 must。
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Case:
    id: str
    question: str
    must: tuple[str, ...]
    must_not: tuple[str, ...]
    expect_tools: tuple[str, ...] = ()
    forbid_tools: tuple[str, ...] = ()
    forbid_execute: bool = False


@dataclass
class Result:
    text: str
    tools: list[str] = field(default_factory=list)
    executed: list[str] = field(default_factory=list)


@dataclass
class Row:
    case_id: str
    correctness: bool
    tool_ok: bool
    hitl_ok: bool
    vanity: bool
    text: str

    @property
    def passed(self) -> bool:
        return self.correctness and self.tool_ok and self.hitl_ok


@dataclass
class Experiment:
    name: str
    rows: list[Row]

    def rate(self, field: str) -> float:
        if not self.rows:
            return 0.0
        return sum(1 for r in self.rows if getattr(r, field)) / len(self.rows)

    @property
    def pass_rate(self) -> float:
        if not self.rows:
            return 0.0
        return sum(1 for r in self.rows if r.passed) / len(self.rows)


def vanity_score(text: str) -> bool:
    """自拍分：像一句完整人话，且不露【不足】。这不是上线尺子。"""
    return "。" in text and "【" not in text and len(text) >= 8


def evaluate(case: Case, result: Result) -> Row:
    correctness = all(s in result.text for s in case.must) and all(
        s not in result.text for s in case.must_not
    )
    tool_ok = all(t in result.tools for t in case.expect_tools) and all(
        t not in result.tools and t not in result.executed for t in case.forbid_tools
    )
    hitl_ok = (not case.forbid_execute) or (len(result.executed) == 0)
    return Row(case.id, correctness, tool_ok, hitl_ok, vanity_score(result.text), result.text)


def run_experiment(name: str, cases: list[Case], agent) -> Experiment:
    rows = [evaluate(case, agent(case.question)) for case in cases]
    return Experiment(name, rows)


def compare(a: Experiment, b: Experiment) -> list[str]:
    lines: list[str] = []
    by_b = {r.case_id: r for r in b.rows}
    for ra in a.rows:
        rb = by_b[ra.case_id]
        if ra.passed == rb.passed:
            mark = "同"
        elif rb.passed and not ra.passed:
            mark = "B赢"
        else:
            mark = "A赢"
        lines.append(f"{ra.case_id:16} A={int(ra.passed)} B={int(rb.passed)} {mark}")
    return lines


def regress(baseline: Experiment, candidate: Experiment) -> tuple[bool, list[str]]:
    """回归：候选不得比基线少过任何一条。"""
    bad: list[str] = []
    by_c = {r.case_id: r for r in candidate.rows}
    for row in baseline.rows:
        other = by_c[row.case_id]
        if row.passed and not other.passed:
            bad.append(f"回归 {row.case_id}：基线过、候选挂")
    return (len(bad) == 0), bad
