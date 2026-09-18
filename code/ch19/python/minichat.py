"""经典 AutoGen 对话协议的教学运行时：发言、选人、终止。不是 pip 里的 autogen 包。"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable


@dataclass
class Message:
    name: str
    content: str


@dataclass
class Agent:
    name: str
    system: str
    reply: Callable[[list[Message]], str]


def last_user(messages: list[Message]) -> str:
    for msg in reversed(messages):
        if msg.name in {"林秋", "user", "User"}:
            return msg.content
    return messages[0].content if messages else ""


def is_terminate(text: str) -> bool:
    return "TERMINATE" in text or text.startswith("【不足】")


class GroupChat:
    def __init__(
        self,
        agents: list[Agent],
        select: Callable[[list[Message], list[Agent]], Agent | None],
        max_round: int = 8,
        term: Callable[[str], bool] = is_terminate,
    ) -> None:
        self.agents = agents
        self.select = select
        self.max_round = max_round
        self.term = term

    def run(self, opening: str, user: str = "林秋") -> list[Message]:
        history = [Message(user, opening)]
        print(f"{user}: {opening}")
        for _ in range(self.max_round):
            speaker = self.select(history, self.agents)
            if speaker is None:
                print("（无人接话，停）")
                break
            text = speaker.reply(history)
            history.append(Message(speaker.name, text))
            print(f"{speaker.name}: {text}")
            if self.term(text):
                break
        else:
            print(f"（触及 max_round={self.max_round}，强制停）")
        return history
