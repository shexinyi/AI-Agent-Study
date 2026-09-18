"""主编排教具：编排器、工人、共享黑板。

教学假设：拆解是规则，不是大模型规划器；工人串行执行，方便对拍；
黑板活在进程里。生产里拆解常是一次结构化输出，工人可进线程池，
黑板可换成 Redis / 业务库。
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable, Literal

Policy = Literal["isolate", "fail_fast"]


@dataclass
class Task:
    id: str
    kind: str
    query: str
    status: str = "pending"  # pending | ok | failed | skipped
    result: str = ""
    error: str = ""


@dataclass
class Blackboard:
    """共享黑板（Shared Blackboard）：工人只写自己的格子，不互改。"""

    slots: dict[str, dict] = field(default_factory=dict)
    log: list[str] = field(default_factory=list)

    def write(self, task_id: str, **fields: object) -> None:
        slot = self.slots.setdefault(task_id, {})
        slot.update(fields)
        shown = ", ".join(f"{k}={v}" for k, v in fields.items())
        self.log.append(f"{task_id}: {shown}")


class Worker:
    def __init__(self, name: str, kind: str, fn: Callable[[Task], str]) -> None:
        self.name = name
        self.kind = kind
        self.fn = fn

    def run(self, task: Task) -> str:
        return self.fn(task)


class Orchestrator:
    """编排器（Orchestrator）拥有目标：拆解、派工、汇总、决定失败怎么收场。"""

    def __init__(self, workers: list[Worker], policy: Policy = "isolate") -> None:
        self.workers = {w.kind: w for w in workers}
        self.policy = policy
        self.board = Blackboard()

    def decompose(self, goal: str) -> list[Task]:
        tasks: list[Task] = []
        n = 1
        if any(k in goal for k in ("事假", "请假", "工作日")):
            tasks.append(Task(f"t{n}", "leave", goal))
            n += 1
        if "ORDER" in goal.upper() or "TZ123" in goal.upper() or "退" in goal:
            tasks.append(Task(f"t{n}", "order", goal))
            n += 1
        if "食堂" in goal:
            tasks.append(Task(f"t{n}", "cafe", goal))
            n += 1
        if not tasks:
            tasks.append(Task("t1", "unknown", goal))
        return tasks

    def run(self, goal: str) -> str:
        self.board = Blackboard()
        tasks = self.decompose(goal)
        failed = False
        for task in tasks:
            if self.policy == "fail_fast" and failed:
                task.status = "skipped"
                self.board.write(task.id, kind=task.kind, status="skipped")
                continue
            worker = self.workers.get(task.kind)
            if worker is None:
                task.status = "failed"
                task.error = f"没有 {task.kind} 工人"
                self.board.write(task.id, kind=task.kind, status="failed", error=task.error)
                failed = True
                continue
            try:
                text = worker.run(task)
                task.status = "ok"
                task.result = text
                self.board.write(task.id, kind=task.kind, status="ok", worker=worker.name, result=text)
            except Exception as exc:  # noqa: BLE001 — 隔离的就是工人异常
                task.status = "failed"
                task.error = f"{type(exc).__name__}: {exc}"
                self.board.write(task.id, kind=task.kind, status="failed", error=task.error)
                failed = True
        return self.gather(tasks)

    def gather(self, tasks: list[Task]) -> str:
        if self.policy == "fail_fast" and any(t.status == "failed" for t in tasks):
            return "【整单失败】有工人未交卷，按快速失败不返回部分结果。"
        lines = ["林秋工单汇总："]
        for task in tasks:
            if task.status == "ok":
                lines.append(f"- [{task.kind}/{task.id}] {task.result}")
            elif task.status == "failed":
                lines.append(f"- [{task.kind}/{task.id}] 【失败隔离】{task.error}")
            else:
                lines.append(f"- [{task.kind}/{task.id}] 【跳过】未派工")
        return "\n".join(lines)
