"""与第 10 章同一套教学哈希嵌入。本章可独立运行。"""

from __future__ import annotations

import hashlib
import math
import re

DIM = 64


def tokens(text: str) -> list[str]:
    words = re.findall(r"[a-z0-9]+", text.lower())
    chars = [ch for ch in text if "\u4e00" <= ch <= "\u9fff"]
    bigrams = [chars[i] + chars[i + 1] for i in range(len(chars) - 1)]
    return words + chars + bigrams


def embed(text: str, dim: int = DIM) -> list[float]:
    vec = [0.0] * dim
    for tok in tokens(text):
        digest = hashlib.md5(tok.encode("utf-8")).digest()
        idx = int.from_bytes(digest[0:2], "little") % dim
        sign = 1.0 if digest[2] % 2 == 0 else -1.0
        vec[idx] += sign
    norm = math.sqrt(sum(x * x for x in vec)) or 1.0
    return [x / norm for x in vec]


def dot(a: list[float], b: list[float]) -> float:
    return sum(x * y for x, y in zip(a, b))
