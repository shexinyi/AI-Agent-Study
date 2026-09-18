"""蜂群教具：交接、路由白名单、进程内总线。

教学假设：交接是返回值，不是一次真实 Function Calling；
路由表写死在代码里；总线是内存列表。生产里交接常是 Agents SDK
的 handoff 工具，总线才是 Kafka / NATS。Java 同构，不是官方 SDK。
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable


@dataclass
class Message:
    text: str
    sender: str
    to: str = ""
    topic: str = ""
    hop: int = 0


@dataclass
class Reply:
    text: str
    handoff: str = ""
    publish_topic: str = ""


@dataclass
class Agent:
    name: str
    topics: tuple[str, ...]
    fn: Callable[[str], Reply]


class Bus:
    """消息总线（Message Bus）：点对点按名字投递，主题按订阅扇出。"""

    def __init__(self) -> None:
        self.inbox: dict[str, list[Message]] = {}
        self.subs: dict[str, list[str]] = {}
        self.log: list[str] = []

    def register(self, name: str, topics: tuple[str, ...]) -> None:
        self.inbox.setdefault(name, [])
        for topic in topics:
            self.subs.setdefault(topic, []).append(name)

    def send_p2p(self, to: str, text: str, sender: str) -> None:
        msg = Message(text=text, sender=sender, to=to)
        self.inbox.setdefault(to, []).append(msg)
        self.log.append(f"p2p {sender} → {to}: {text}")

    def publish(self, topic: str, text: str, sender: str) -> None:
        targets = list(self.subs.get(topic, []))
        self.log.append(f"pub {sender} → #{topic} [{', '.join(targets) or '无人订阅'}]: {text}")
        for name in targets:
            self.inbox.setdefault(name, []).append(
                Message(text=text, sender=sender, topic=topic)
            )

    def drain(self, name: str) -> list[Message]:
        items = list(self.inbox.get(name, []))
        self.inbox[name] = []
        return items


class Swarm:
    """当前拿话筒的人拥有会话。没有老板，也就没有汇总。"""

    def __init__(
        self,
        agents: list[Agent],
        allow: dict[str, tuple[str, ...]],
        max_hops: int = 6,
    ) -> None:
        self.agents = {a.name: a for a in agents}
        self.allow = allow
        self.max_hops = max_hops
        self.hops: list[str] = []
        self.log: list[str] = []

    def run(self, start: str, user: str) -> str:
        self.hops = []
        self.log = []
        current = start
        for hop in range(self.max_hops):
            agent = self.agents.get(current)
            if agent is None:
                return f"【路由失败】没有名叫 {current} 的人。"
            self.hops.append(current)
            reply = agent.fn(user)
            self.log.append(f"hop{hop} {current}: handoff={reply.handoff or '-'} | {reply.text}")
            if not reply.handoff:
                return reply.text
            allowed = self.allow.get(current, ())
            if reply.handoff not in allowed:
                return f"【非法交接】{current} 不能把话筒给 {reply.handoff}。白名单={list(allowed)}"
            if reply.handoff == current:
                return f"【路由失败】{current} 把话筒交给自己。"
            current = reply.handoff
        return f"【超过 max_hops={self.max_hops}】轨迹={' → '.join(self.hops)}"


@dataclass
class BusSwarm:
    agents: dict[str, Agent] = field(default_factory=dict)
    bus: Bus = field(default_factory=Bus)

    def add(self, agent: Agent) -> None:
        self.agents[agent.name] = agent
        self.bus.register(agent.name, agent.topics)

    def deliver(self) -> list[str]:
        """每个收件箱里的消息交给订阅者，返回他们的口头答复（不交接）。"""
        answers: list[str] = []
        for name, agent in self.agents.items():
            for msg in self.bus.drain(name):
                reply = agent.fn(msg.text)
                answers.append(f"{name}: {reply.text}")
        return answers
