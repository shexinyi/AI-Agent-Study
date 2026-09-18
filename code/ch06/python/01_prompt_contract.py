"""
第 6 章示例 1：把 Prompt 渲染成真正会发给模型的 messages。

    python 01_prompt_contract.py
"""

from __future__ import annotations

import json

from specs import GOOD, VAGUE


def dump(spec, utterance: str) -> None:
    print("=" * 60)
    print(f"{spec.name} @ {spec.version}  约 {spec.tokenish_size(utterance=utterance)} 字符")
    print("=" * 60)
    for item in spec.messages(utterance=utterance):
        print(f"[{item.role}]\n{item.content}\n")
    print("OpenAI messages:")
    print(json.dumps([m.as_openai() for m in spec.messages(utterance=utterance)], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    text = "我是林秋，下周一到周三请假去上海办事，帮我走流程。"
    dump(VAGUE, text)
    dump(GOOD, text)
