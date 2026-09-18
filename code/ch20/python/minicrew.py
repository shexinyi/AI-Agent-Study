"""CrewAI 产品语言的教学运行时：Agent / Task / Crew / Process。不是 pip 里的 crewai。"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable


@dataclass
class Agent:
    role: str
    goal: str
    backstory: str
    work: Callable[[str, dict[str, Any]], str]


@dataclass
class Task:
    name: str
    description: str
    expected: str
    agent: Agent | None = None
    output: str = ""


class Process:
    SEQUENTIAL = "sequential"
    HIERARCHICAL = "hierarchical"


@dataclass
class Crew:
    agents: list[Agent]
    tasks: list[Task]
    process: str = Process.SEQUENTIAL
    manager: Agent | None = None
    log: list[str] = field(default_factory=list)

    def kickoff(self, inputs: dict[str, Any]) -> str:
        question = str(inputs.get("question", ""))
        context: dict[str, str] = {}
        self.log.clear()
        for task in self.tasks:
            worker = task.agent
            if self.process == Process.HIERARCHICAL:
                if self.manager is None:
                    raise ValueError("层级流必须有 manager")
                if worker is not None:
                    self.log.append(f"经理尊重预绑定 {task.name} → {worker.role}")
                else:
                    pick = self.manager.work(task.description + "\n问:" + question, {"agents": [a.role for a in self.agents]})
                    worker = next((a for a in self.agents if a.role == pick), None)
                    self.log.append(f"经理指派 {task.name} → {pick}")
                    if worker is None:
                        task.output = f"【不足】经理指派了不存在的角色: {pick}"
                        context[task.name] = task.output
                        continue
            elif worker is None:
                raise ValueError(f"顺序流任务 {task.name} 必须预绑定 agent")
            else:
                self.log.append(f"顺序执行 {task.name} / {worker.role}")
            blob = {"question": question, "context": dict(context), "expected": task.expected}
            task.output = worker.work(task.description, blob)
            context[task.name] = task.output
        return self.tasks[-1].output
