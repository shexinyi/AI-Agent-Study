"""从模型散文、Markdown 代码块里抠 JSON。抠出来仍要交给 Pydantic。"""

from __future__ import annotations

import json
import re
from typing import Any


def extract_json_text(raw: str) -> str:
    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw, flags=re.S)
    if fenced:
        return fenced.group(1)
    start = raw.find("{")
    end = raw.rfind("}")
    if start >= 0 and end > start:
        return raw[start : end + 1]
    raise ValueError("输出里找不到 JSON 对象")


def try_load(raw: str) -> Any:
    return json.loads(extract_json_text(raw))
