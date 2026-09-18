"""请假抽取的输出契约：用 Pydantic 接住模型，而不是 json.loads 后祈祷。"""

from __future__ import annotations

from datetime import date
from typing import Literal

from pydantic import BaseModel, Field, model_validator


class LeaveSlots(BaseModel):
    employee: str = Field(min_length=2, description="员工姓名")
    start_date: date
    end_date: date
    days: int = Field(ge=1, le=30)
    leave_type: Literal["年假", "事假", "病假"] | None = None

    @model_validator(mode="after")
    def days_match_range(self) -> "LeaveSlots":
        delta = (self.end_date - self.start_date).days + 1
        if delta != self.days:
            raise ValueError(f"days={self.days} 与日期区间 {delta} 天不一致")
        return self


class LeaveExtract(BaseModel):
    ok: bool
    slots: LeaveSlots | None = None
    error: Literal["need_clarification", "refused"] | None = None

    @model_validator(mode="after")
    def consistent(self) -> "LeaveExtract":
        if self.ok and self.slots is None:
            raise ValueError("ok=true 时必须带 slots")
        if not self.ok and self.error is None:
            raise ValueError("ok=false 时必须带 error")
        return self
