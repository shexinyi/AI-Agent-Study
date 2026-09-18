"""成本与延迟教具。

教学假设：Token 按字符数计；单价是课堂约定的教学币，不是官网价。
流式只改首字时间，不改账单。缓存命中不计模型费。
"""

from __future__ import annotations

from dataclasses import dataclass, field

# 每 1000 教学 Token 的教学币；检索按次。
RATES = {
    "large_in": 3.0,
    "large_out": 15.0,
    "small_in": 0.2,
    "small_out": 0.8,
    "retrieve": 1.0,
}


def tokens(text: str) -> int:
    return max(1, len(text))


def fee(kind: str, n: int) -> float:
    if kind == "retrieve":
        return RATES["retrieve"] * n
    return RATES[kind] * n / 1000.0


@dataclass
class Line:
    bucket: str  # input | output | retry | retrieve
    model: str
    tokens: int
    amount: float
    note: str


@dataclass
class Latency:
    queue_ms: int = 0
    model_ms: int = 0
    tool_ms: int = 0
    net_ms: int = 0
    ttft_ms: int = 0

    @property
    def total_ms(self) -> int:
        return self.queue_ms + self.model_ms + self.tool_ms + self.net_ms


@dataclass
class Run:
    text: str
    model: str
    cached: bool = False
    lines: list[Line] = field(default_factory=list)
    latency: Latency = field(default_factory=Latency)
    tools: list[str] = field(default_factory=list)

    @property
    def amount(self) -> float:
        return round(sum(x.amount for x in self.lines), 4)


def add_llm(run: Run, model: str, prompt: str, output: str, *, retry: int = 0) -> None:
    inn, out = tokens(prompt), tokens(output)
    run.lines.append(Line("input", model, inn, fee(f"{model}_in", inn), "prompt"))
    run.lines.append(Line("output", model, out, fee(f"{model}_out", out), "completion"))
    for i in range(retry):
        run.lines.append(Line("retry", model, inn, fee(f"{model}_in", inn), f"retry#{i+1} in"))
        run.lines.append(Line("retry", model, out, fee(f"{model}_out", out), f"retry#{i+1} out"))


def add_retrieve(run: Run, n: int = 1) -> None:
    run.lines.append(Line("retrieve", "-", n, fee("retrieve", n), f"{n} 次检索"))


def summarize(runs: list[Run]) -> dict[str, float]:
    buckets = {"input": 0.0, "output": 0.0, "retry": 0.0, "retrieve": 0.0}
    for run in runs:
        for line in run.lines:
            buckets[line.bucket] += line.amount
    return {k: round(v, 4) for k, v in buckets.items()}
