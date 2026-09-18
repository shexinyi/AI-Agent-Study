"""把页眉页脚、围栏代码、废止提示从制度正文里拿掉。"""

from __future__ import annotations

import re

NOISE = re.compile(r"机密|作废|Page \d|打印于|内部作废")


def clean_document(raw: str) -> str:
    raw = re.sub(r"```.*?```", "", raw, flags=re.S)
    kept: list[str] = []
    for line in raw.splitlines():
        if NOISE.search(line):
            continue
        if re.match(r"^=+$", line.strip()):
            continue
        kept.append(line)
    text = "\n".join(kept)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def tag_kind(text: str) -> str:
    if text.strip().startswith("|"):
        return "table"
    if "MAX_LEAVE" in text or "```" in text:
        return "code"
    if "已废止" in text or "旧稿" in text:
        return "deprecated"
    return "policy"
