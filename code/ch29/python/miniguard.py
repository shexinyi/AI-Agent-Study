"""安全教具：身份来自会话，检索是资料，工具看白名单。

教学假设：注入句是课堂假数据，用来检验闸门，不是进攻手册。
策略引擎是 if/else，不是云上 WAF。Java 同构。
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Identity:
    user: str
    role: str
    tenant: str


@dataclass
class Doc:
    id: str
    text: str
    acl: str
    tenant: str
    deprecated: bool = False
    untrusted: bool = False


@dataclass
class Decision:
    allow: bool
    reason: str
    tool: str = ""


@dataclass
class Result:
    text: str
    tools: list[str] = field(default_factory=list)
    executed: list[str] = field(default_factory=list)
    seen_ids: list[str] = field(default_factory=list)
    decisions: list[Decision] = field(default_factory=list)


ROLES = {
    "intern": {"tools": ("search_policy", "get_order"), "acl": ("public",)},
    "hr": {"tools": ("search_policy", "get_order"), "acl": ("public", "hr")},
    "agent_ops": {"tools": ("search_policy", "get_order", "refund_order"), "acl": ("public", "hr")},
}


def allow_tool(ident: Identity, tool: str) -> Decision:
    allowed = ROLES.get(ident.role, {}).get("tools", ())
    if tool not in allowed:
        return Decision(False, f"{ident.role} 不在 {tool} 白名单", tool)
    return Decision(True, "白名单通过", tool)


def allow_doc(ident: Identity, doc: Doc, apply_acl: bool) -> Decision:
    if doc.tenant != ident.tenant:
        return Decision(False, "跨租户", doc.id)
    if apply_acl and doc.acl not in ROLES[ident.role]["acl"]:
        return Decision(False, f"ACL {doc.acl} 对 {ident.role} 不可见", doc.id)
    return Decision(True, "可见", doc.id)


class PolicyEngine:
    """护栏：工具白名单、ACL、租户、检索不得当指令、写工具须 HITL。"""

    def __init__(self, catalog: list[Doc]) -> None:
        self.catalog = catalog

    def retrieve(self, ident: Identity, apply_acl: bool = True) -> list[Doc]:
        seen: list[Doc] = []
        for doc in self.catalog:
            if allow_doc(ident, doc, apply_acl).allow:
                seen.append(doc)
        return seen

    def check_refund(self, ident: Identity, hitl_approved: bool) -> Decision:
        gate = allow_tool(ident, "refund_order")
        if not gate.allow:
            return gate
        if not hitl_approved:
            return Decision(False, "写工具须 HITL", "refund_order")
        return Decision(True, "HITL 已批准", "refund_order")
