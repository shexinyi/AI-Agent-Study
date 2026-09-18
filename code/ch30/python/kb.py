"""两周切片：企业制度问答。检索是关键词打分，不是生产 Embedding。"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Doc:
    id: str
    title: str
    text: str
    acl: str
    deprecated: bool = False


CORPUS = [
    Doc("leave10", "现行事假", "事假每年累计不超过 10 个工作日。下午 5 点前补交材料。", "public"),
    Doc("leave5", "废止稿", "废止：事假每年 5 天。仅 HR 归档可见。", "hr", True),
    Doc("meal", "餐补", "出勤日餐补 30 元，须当月提交。", "public"),
]

SCOPE = ("事假", "请假", "工作日", "餐补")


def visible(role: str, apply_acl: bool) -> list[Doc]:
    out = []
    for doc in CORPUS:
        if apply_acl and role == "intern" and doc.acl != "public":
            continue
        out.append(doc)
    return out


def retrieve(question: str, role: str, *, apply_acl: bool = True, k: int = 2) -> list[Doc]:
    pool = visible(role, apply_acl)
    scored: list[tuple[int, Doc]] = []
    for doc in pool:
        blob = doc.title + doc.text
        score = sum(blob.count(w) for w in ("事假", "工作日", "餐补", "5 天", "10"))
        if "事假" in question or "请假" in question:
            score += 3 if "事假" in blob else 0
            if apply_acl:
                score -= 5 if doc.deprecated else 0
        if "餐补" in question:
            score += 3 if "餐补" in blob else 0
        scored.append((score, doc))
    scored.sort(key=lambda x: (-x[0], x[1].id))
    return [d for s, d in scored if s > 0][:k]


def answer(question: str, role: str, *, apply_acl: bool = True) -> tuple[str, list[Doc]]:
    if not any(k in question for k in SCOPE) or any(k in question for k in ("年假", "ORDER", "食堂", "退")):
        return "【不足】超出本项目范围：只答现行事假与餐补，并带引用。", []
    hits = retrieve(question, role, apply_acl=apply_acl)
    if not hits:
        return "【不足】资料不够，不能编。", []
    if not apply_acl:
        stale = next((d for d in hits if d.deprecated), None)
        if stale and any(k in question for k in ("事假", "请假", "工作日")):
            return f"根据[{stale.id}]，{stale.text}", hits
    top = hits[0]
    cite = f"[{top.id}]"
    if "餐补" in question:
        return f"根据{cite}，出勤日餐补 30 元，须当月提交。", hits
    if "5 天" in top.text and apply_acl:
        return "【不足】可见资料不足以回答现行天数。", hits
    if "10 个工作日" in top.text:
        return f"根据{cite}，事假每年累计不超过 10 个工作日。", hits
    return f"根据{cite}，{top.text}", hits
