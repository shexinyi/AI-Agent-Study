"""带超时、幂等、审计的工具执行器。"""

from __future__ import annotations

import json
import time
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeout
from typing import Any

from toolkit import AuditEvent, ToolCall, ToolResult, ToolSpec


def validate_arguments(schema: dict[str, Any], args: dict[str, Any]) -> str | None:
    required = schema.get("required") or []
    missing = [key for key in required if key not in args]
    if missing:
        return f"缺少参数: {missing}"
    properties = schema.get("properties") or {}
    extra = [key for key in args if key not in properties]
    if extra:
        return f"未声明参数: {extra}"
    return None


class ProductionExecutor:
    def __init__(self, specs: dict[str, ToolSpec], granted_write: set[str] | None = None) -> None:
        self.specs = specs
        self.granted_write = granted_write or set()
        self.audit: list[AuditEvent] = []
        self._idem_cache: dict[str, ToolResult] = {}

    def execute(self, call: ToolCall) -> ToolResult:
        spec = self.specs.get(call.name)
        started = time.perf_counter()
        if spec is None:
            return self._finish(call, None, ToolResult(False, {}, "UNKNOWN_TOOL"), started, False)

        if spec.risk in ("write", "money") and call.name not in self.granted_write:
            return self._finish(
                call, spec, ToolResult(False, {}, f"PERMISSION_DENIED: {call.name}"), started, False
            )

        schema_error = validate_arguments(spec.parameters, call.arguments)
        if schema_error:
            return self._finish(call, spec, ToolResult(False, {}, f"SCHEMA_ERROR: {schema_error}"), started, False)

        cache_key = self._cache_key(call, spec)
        if spec.idempotent and cache_key in self._idem_cache:
            cached = self._idem_cache[cache_key]
            replayed = ToolResult(cached.ok, dict(cached.data), cached.error, replayed=True)
            return self._finish(call, spec, replayed, started, True)

        try:
            with ThreadPoolExecutor(max_workers=1) as pool:
                future = pool.submit(spec.handler, **call.arguments)
                data = future.result(timeout=spec.timeout_seconds)
            if not isinstance(data, dict):
                data = {"value": data}
            result = ToolResult(True, data)
        except FuturesTimeout:
            result = ToolResult(False, {}, f"TIMEOUT: {spec.timeout_seconds}s")
        except Exception as exc:  # noqa: BLE001
            result = ToolResult(False, {}, f"HANDLER_ERROR: {exc}")

        if spec.idempotent and result.ok and cache_key:
            self._idem_cache[cache_key] = result
        return self._finish(call, spec, result, started, False)

    def _cache_key(self, call: ToolCall, spec: ToolSpec) -> str:
        if call.idempotency_key:
            return f"{call.name}:{call.idempotency_key}"
        return f"{call.name}:{json.dumps(call.arguments, sort_keys=True, ensure_ascii=False)}"

    def _finish(
        self,
        call: ToolCall,
        spec: ToolSpec | None,
        result: ToolResult,
        started: float,
        replayed: bool,
    ) -> ToolResult:
        latency = int((time.perf_counter() - started) * 1000)
        self.audit.append(
            AuditEvent(
                tool=call.name,
                ok=result.ok,
                risk=spec.risk if spec else "unknown",
                latency_ms=latency,
                idempotency_key=call.idempotency_key,
                error=result.error,
                replayed=replayed or result.replayed,
            )
        )
        return result
