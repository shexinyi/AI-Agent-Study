"""
第 4 章：OpenAI 兼容的工具协议 + 带校验、权限、重试的执行器。

只依赖 Python 3.10+ 标准库。对象形状对齐 Chat Completions 的 tools / tool_calls。
"""

from __future__ import annotations

import json
import time
from dataclasses import dataclass, field
from typing import Any, Callable, Literal

Risk = Literal["read", "write", "money"]


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    parameters: dict[str, Any]
    risk: Risk
    handler: Callable[..., Any]

    def as_openai_tool(self) -> dict[str, Any]:
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.parameters,
            },
        }


@dataclass
class ToolCall:
    id: str
    name: str
    arguments: dict[str, Any]

    @staticmethod
    def from_openai(raw: dict[str, Any]) -> "ToolCall":
        fn = raw["function"]
        args = fn.get("arguments") or "{}"
        parsed = json.loads(args) if isinstance(args, str) else dict(args)
        return ToolCall(id=raw["id"], name=fn["name"], arguments=parsed)


@dataclass
class ToolResult:
    tool_call_id: str
    name: str
    ok: bool
    data: dict[str, Any]
    error: str | None = None

    def as_openai_message(self) -> dict[str, Any]:
        payload = {"ok": self.ok, "data": self.data}
        if self.error:
            payload["error"] = self.error
        return {
            "role": "tool",
            "tool_call_id": self.tool_call_id,
            "content": json.dumps(payload, ensure_ascii=False),
        }


def validate_arguments(schema: dict[str, Any], args: dict[str, Any]) -> str | None:
    """最小 JSON Schema 校验：required + 基本 type。生产请换 jsonschema / jackson。"""
    required = schema.get("required") or []
    missing = [key for key in required if key not in args]
    if missing:
        return f"缺少参数: {missing}"
    properties = schema.get("properties") or {}
    for key, value in args.items():
        declared = properties.get(key, {}).get("type")
        if declared == "string" and not isinstance(value, str):
            return f"{key} 应为 string"
        if declared == "integer" and not isinstance(value, int):
            return f"{key} 应为 integer"
        if declared == "number" and not isinstance(value, (int, float)):
            return f"{key} 应为 number"
        if declared == "boolean" and not isinstance(value, bool):
            return f"{key} 应为 boolean"
    extra = [key for key in args if key not in properties]
    if extra:
        return f"未声明参数: {extra}"
    return None


@dataclass
class ToolExecutor:
    specs: dict[str, ToolSpec]
    granted_write: set[str] = field(default_factory=set)
    max_retries: int = 2
    retry_wait_seconds: float = 0.05

    def execute(self, call: ToolCall) -> ToolResult:
        spec = self.specs.get(call.name)
        if spec is None:
            return ToolResult(call.id, call.name, False, {}, "UNKNOWN_TOOL")

        if spec.risk in ("write", "money") and call.name not in self.granted_write:
            return ToolResult(
                call.id,
                call.name,
                False,
                {},
                f"PERMISSION_DENIED: {call.name} 风险={spec.risk}，未在授权名单",
            )

        schema_error = validate_arguments(spec.parameters, call.arguments)
        if schema_error:
            return ToolResult(call.id, call.name, False, {}, f"SCHEMA_ERROR: {schema_error}")

        last_error = "UNKNOWN"
        for attempt in range(1, self.max_retries + 1):
            try:
                data = spec.handler(**call.arguments)
                if not isinstance(data, dict):
                    data = {"value": data}
                return ToolResult(call.id, call.name, True, data)
            except TransientError as exc:
                last_error = str(exc)
                if attempt < self.max_retries:
                    time.sleep(self.retry_wait_seconds)
                    continue
                return ToolResult(call.id, call.name, False, {"attempts": attempt}, f"TRANSIENT: {last_error}")
            except Exception as exc:  # noqa: BLE001 - 执行器必须把业务异常变成观察
                return ToolResult(call.id, call.name, False, {}, f"HANDLER_ERROR: {exc}")
        return ToolResult(call.id, call.name, False, {}, f"TRANSIENT: {last_error}")


class TransientError(RuntimeError):
    """下游超时、限流等可重试错误。权限拒绝和参数错误不要用它。"""


def extract_tool_calls(assistant_message: dict[str, Any]) -> list[ToolCall]:
    raw_calls = assistant_message.get("tool_calls") or []
    return [ToolCall.from_openai(item) for item in raw_calls]
