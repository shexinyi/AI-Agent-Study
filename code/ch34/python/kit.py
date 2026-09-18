"""第 34 章收官教具：模拟面试、简历对照 JD、开源扫描、职级与缺口。

教学假设：分数是课堂尺子，不是某家 HR 官方分。
JD 是教学职位描述。扫描是正则教具，不是生产密钥扫描器。
可退 ≠ 已退。不写 99%。教学币不是官网价。
谈薪只对齐职级证据，不编造年薪数字。
"""

from __future__ import annotations

import re
from dataclasses import dataclass

SLOTS = [
    (3, "开场", "一分钟三个项目：只读+引用 / 办理+HITL / 服务门禁"),
    (7, "项目1", "事假 10 个工作日、关 ACL 会抄 5 天、金标退出码"),
    (6, "项目2", "主动打开 99 / pending / 拒绝 / 越权 / 隔离"),
    (6, "项目3", "401、429、[PHONE]、fat 9.28 / lean 1.96"),
    (4, "排障", "已办理且无 refund_order → MODEL_LIE"),
    (4, "代码+谈薪", "先写拒绝；用三项目证据对齐职级，不报虚构年薪"),
]

JD_MUST = [
    "引用",
    "ACL",
    "HITL",
    "金标",
    "Trace",
    "配额",
]

JD_BAN = [
    "99%",
    "99 分",
    "准确率 99",
    "FastAPI 和 Spring 都很熟",
    "sk-",
]

JUNIOR = {
    "能独立讲清一个项目的边界和金标",
    "能复现失败路径，不只能演示成功",
    "知道模型输出是意图",
}

MID = {
    "能把失败从 Trace 拆到 MODEL_LIE / RETRIEVE / LOOP / BILL",
    "能守住 money 不 fail_open，并解释 429 ≠ 已办理",
    "能拒绝同时做浅的双框架，管道顺序说得清",
}

GAPS = [
    ("W1-2", "闭环与工具", ("意图", "可退")),
    ("W3-4", "RAG 与项目1", ("引用", "ACL", "金标")),
    ("W5-6", "办理与项目2", ("HITL", "隔离")),
    ("W7", "服务与项目3", ("401", "429", "教学币")),
    ("W8", "面试收口", ("MODEL_LIE", "职级")),
]


@dataclass
class Score:
    points: int
    notes: list[str]


def score_talk(text: str) -> Score:
    notes: list[str] = []
    pts = 0
    checks = [
        ("10 个工作日", 2, "项目1 现行答案"),
        ("5 天", 1, "对照病"),
        ("HITL", 2, "写操作有人"),
        ("401", 1, "鉴权"),
        ("429", 1, "配额"),
        ("MODEL_LIE", 2, "排障代号"),
        ("意图", 1, "工具调用尺子"),
    ]
    for needle, w, why in checks:
        if needle in text:
            pts += w
            notes.append(f"+{w} {why}")
    if "99%" in text or "99 分" in text:
        pts -= 4
        notes.append("-4 写了 99%")
    if "FastAPI" in text and "Spring" in text and "二选一" not in text:
        pts -= 2
        notes.append("-2 双框架都浅")
    if "已退" in text and "可退" not in text and "HITL" not in text:
        pts -= 2
        notes.append("-2 可退说成已退")
    return Score(pts, notes)


def lint_resume(text: str) -> list[str]:
    issues: list[str] = []
    for must in JD_MUST:
        if must not in text:
            issues.append(f"缺 JD 关键词: {must}")
    for ban in JD_BAN:
        if ban in text:
            issues.append(f"禁写: {ban}")
    if "STAR" not in text and not any(k in text for k in ("情境", "结果", "对照")):
        issues.append("缺 STAR / 对照结果")
    return issues


PHONE_RE = re.compile(r"1\d{10}")
KEY_RE = re.compile(r"sk-[A-Za-z0-9]{8,}")
ENV_RE = re.compile(r"(?i)(api_key|secret_key)\s*=\s*\S+")


def scan_text(name: str, text: str) -> list[str]:
    hits: list[str] = []
    if PHONE_RE.search(text):
        hits.append(f"{name}: 明文手机号")
    if KEY_RE.search(text):
        hits.append(f"{name}: 疑似 API Key")
    if ENV_RE.search(text) and ".example" not in name:
        hits.append(f"{name}: 疑似密钥赋值")
    if "林秋" in text and "教学" not in text and "课堂" not in text:
        hits.append(f"{name}: 业务名未标明教学")
    return hits


def gap_of(text: str) -> list[str]:
    missing: list[str] = []
    for week, title, needles in GAPS:
        if not any(n in text for n in needles):
            missing.append(f"{week} {title}")
    return missing


WEAK = (
    "我用 LangChain 和 Spring Boot 做了智能客服，准确率 99%，"
    "FastAPI 和 Spring 都很熟，已经帮用户退款。"
)

STRONG = (
    "三个项目：制度问答 10 个工作日，带引用和 ACL，关 ACL 会抄 5 天；"
    "办理流 HITL，ORDER-88 可退未执行；"
    "服务 401/429，审计 [PHONE]，fat 9.28 对 lean 1.96 教学币。"
    "已办理且无 refund_order 是 MODEL_LIE。模型输出是意图。框架二选一。"
    "初级对齐：能守金标和失败路径。谈薪只谈职级证据，不报价。"
)

DIRTY_RESUME = (
    "熟悉 FastAPI 和 Spring 都很熟。准确率 99%。"
    "对接 sk-TEACHFAKE01。多智能体很聪明。"
)

CLEAN_RESUME = (
    "情境：5 天还在流传。结果：金标 5 条进 CI，引用 [leave10]，ACL 过滤废止稿。"
    "办理：HITL，可退 ≠ 已退。服务：Trace、配额 429。"
    "对照：关 ACL 会抄 5 天。框架二选一做深 FastAPI。"
)

DIRTY_REPO = {
    "readme.md": "林秋请假系统",
    ".env": "API_KEY=sk-TEACHFAKE01",
    "log.txt": "用户手机 13800138000 已退款",
}

CLEAN_REPO = {
    "README.md": "课堂教具：林秋 / ORDER-88。教学假设见正文。",
    ".env.example": "API_KEY=",
    "audit.md": "question=[PHONE] executed=[] 可退须 HITL",
}
