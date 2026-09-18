"""教学托管运行时：Assistant / Thread / Run / Tool。不是 OpenAI 线上服务。"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any
from uuid import uuid4

SUNSET = "2026-08-26"


def _id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:8]}"


@dataclass
class Assistant:
    id: str
    instructions: str
    tools: list[str]


@dataclass
class Message:
    role: str
    content: str


@dataclass
class Run:
    id: str
    thread_id: str
    assistant_id: str
    status: str
    required_action: dict[str, Any] | None = None
    last_error: str | None = None


@dataclass
class Thread:
    id: str
    messages: list[Message] = field(default_factory=list)


class HostedRuntime:
    """
    模拟供应商侧的对象图。
    cloud_alive=False 表示日落之后：只剩你本地导出的副本。
    """

    def __init__(self, cloud_alive: bool = True) -> None:
        self.cloud_alive = cloud_alive
        self.assistants: dict[str, Assistant] = {}
        self.threads: dict[str, Thread] = {}
        self.runs: dict[str, Run] = {}

    def _live(self) -> None:
        if not self.cloud_alive:
            raise RuntimeError(f"Assistants API 已于 {SUNSET} 日落。thread_id 在供应商侧不再可取。")

    def create_assistant(self, instructions: str, tools: list[str]) -> Assistant:
        self._live()
        a = Assistant(_id("asst"), instructions, tools)
        self.assistants[a.id] = a
        return a

    def create_thread(self) -> Thread:
        self._live()
        t = Thread(_id("thread"))
        self.threads[t.id] = t
        return t

    def add_message(self, thread_id: str, role: str, content: str) -> Message:
        self._live()
        msg = Message(role, content)
        self.threads[thread_id].messages.append(msg)
        return msg

    def create_run(self, thread_id: str, assistant_id: str) -> Run:
        self._live()
        run = Run(_id("run"), thread_id, assistant_id, "in_progress")
        self.runs[run.id] = run
        self._step(run)
        return run

    def submit_tool_outputs(self, run_id: str, outputs: list[dict[str, str]]) -> Run:
        self._live()
        run = self.runs[run_id]
        if run.status != "requires_action":
            raise ValueError("当前 Run 不在 requires_action")
        blob = " ".join(o["output"] for o in outputs)
        self.threads[run.thread_id].messages.append(Message("tool", blob))
        run.required_action = None
        run.status = "in_progress"
        self._step(run)
        return run

    def export_thread(self, thread_id: str) -> list[dict[str, str]]:
        """你自己必须留下的副本。只存 thread_id 不够。"""
        self._live()
        return [{"role": m.role, "content": m.content} for m in self.threads[thread_id].messages]

    def sunset(self) -> None:
        self.cloud_alive = False
        self.assistants.clear()
        self.threads.clear()
        self.runs.clear()

    def _step(self, run: Run) -> None:
        asst = self.assistants[run.assistant_id]
        thread = self.threads[run.thread_id]
        question = next((m.content for m in reversed(thread.messages) if m.role == "user"), "")
        tools_done = [m.content for m in thread.messages if m.role == "tool"]
        if "食堂" in question or "年假" in question:
            thread.messages.append(Message("assistant", "【不足】资料不够。"))
            run.status = "completed"
            return
        if any(k in question for k in ("事假", "请假", "工作日")):
            thread.messages.append(Message("assistant", "根据制度，事假每年累计不超过 10 个工作日。"))
            run.status = "completed"
            return
        if "get_order" in asst.tools and not tools_done and ("ORDER" in question.upper() or "TZ" in question.upper() or "退" in question):
            oid = "ORDER-99" if ("TZ123" in question.upper() or "ORDER-99" in question.upper()) else "ORDER-88"
            run.status = "requires_action"
            run.required_action = {"tool": "get_order", "order_id": oid}
            return
        fact = tools_done[-1] if tools_done else ""
        if "未发货" in fact:
            thread.messages.append(Message("assistant", "根据工具，ORDER-88 未发货可退。写工具前须人工确认。"))
        elif "已发货" in fact or "TZ123" in fact:
            thread.messages.append(Message("assistant", "根据工具，TZ123 对应 ORDER-99，已发货须走售后。"))
        else:
            thread.messages.append(Message("assistant", "【不足】工具没有可用事实。"))
        run.status = "completed"
