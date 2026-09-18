"""把 Prompt 当成带版本号的接口契约，而不是一段藏在代码里的长字符串。"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Message:
    role: str
    content: str

    def as_openai(self) -> dict[str, str]:
        return {"role": self.role, "content": self.content}


@dataclass
class Example:
    user: str
    assistant: str


@dataclass
class PromptSpec:
    """一份可版本化的提示契约。"""

    name: str
    version: str
    system: str
    user_template: str
    examples: list[Example] = field(default_factory=list)
    constraints: tuple[str, ...] = ()
    output_contract: str = ""

    def render_system(self) -> str:
        parts = [self.system]
        if self.constraints:
            parts.append("约束：\n- " + "\n- ".join(self.constraints))
        if self.output_contract:
            parts.append("输出契约：\n" + self.output_contract)
        return "\n\n".join(parts)

    def render_user(self, **variables: Any) -> str:
        try:
            return self.user_template.format(**variables)
        except KeyError as exc:
            raise ValueError(f"模板缺少变量 {exc}，契约 {self.name}@{self.version}") from exc

    def messages(self, **variables: Any) -> list[Message]:
        items = [Message("system", self.render_system())]
        for ex in self.examples:
            items.append(Message("user", ex.user))
            items.append(Message("assistant", ex.assistant))
        items.append(Message("user", self.render_user(**variables)))
        return items

    def tokenish_size(self, **variables: Any) -> int:
        return sum(len(item.content) for item in self.messages(**variables))
