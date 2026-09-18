"""业务函数 → 可点菜单的工具规格。"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable, Literal

Risk = Literal["read", "write", "money"]


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    when_to_use: str
    when_not: str
    parameters: dict[str, Any]
    risk: Risk
    timeout_seconds: float
    idempotent: bool
    handler: Callable[..., Any]
    keywords: tuple[str, ...] = ()

    def menu_text(self) -> str:
        return (
            f"{self.name}: {self.description} "
            f"使用时机: {self.when_to_use} "
            f"不要用于: {self.when_not}"
        )

    def as_openai_tool(self) -> dict[str, Any]:
        description = f"{self.description} 使用时机: {self.when_to_use} 不要用于: {self.when_not}"
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": description,
                "parameters": self.parameters,
            },
        }


@dataclass
class ToolCall:
    id: str
    name: str
    arguments: dict[str, Any]
    idempotency_key: str | None = None


@dataclass
class AuditEvent:
    tool: str
    ok: bool
    risk: str
    latency_ms: int
    idempotency_key: str | None
    error: str | None
    replayed: bool = False


@dataclass
class ToolResult:
    ok: bool
    data: dict[str, Any] = field(default_factory=dict)
    error: str | None = None
    replayed: bool = False
