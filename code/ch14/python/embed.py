"""与第 10 / 13 章同一套哈希嵌入，另附一个「只吃 ASCII」的坏编目员。"""

from __future__ import annotations

import hashlib
import math
import re

DIM = 64


def tokens(text: str, ascii_only: bool = False) -> list[str]:
    words = re.findall(r"[a-z0-9]+", text.lower())
    if ascii_only:
        return words
    chars = [ch for ch in text if "\u4e00" <= ch <= "\u9fff"]
    bigrams = [chars[i] + chars[i + 1] for i in range(len(chars) - 1)]
    return words + chars + bigrams


def embed(text: str, dim: int = DIM, ascii_only: bool = False) -> list[float]:
    vec = [0.0] * dim
    for tok in tokens(text, ascii_only=ascii_only):
        digest = hashlib.md5(tok.encode("utf-8")).digest()
        idx = int.from_bytes(digest[0:2], "little") % dim
        sign = 1.0 if digest[2] % 2 == 0 else -1.0
        vec[idx] += sign
    norm = math.sqrt(sum(x * x for x in vec)) or 1.0
    return [x / norm for x in vec]


def cosine(a: list[float], b: list[float]) -> float:
    return sum(x * y for x, y in zip(a, b))
