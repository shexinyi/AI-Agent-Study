"""
第 7 章示例 1：同一业务，坏菜单和好菜单。

    python 01_tool_design.py
"""

from __future__ import annotations

import json

from catalog import bad_specs, good_specs


def dump(title: str, specs) -> None:
    print("=" * 60)
    print(title)
    print("=" * 60)
    for spec in specs.values():
        print(f"\n# {spec.name}  risk={spec.risk}  timeout={spec.timeout_seconds}s  idempotent={spec.idempotent}")
        print("description:", spec.description)
        print("when_to_use:", spec.when_to_use)
        print("when_not:", spec.when_not)
        print("openai.function.description:")
        print(" ", spec.as_openai_tool()["function"]["description"])
    tools = [spec.as_openai_tool() for spec in specs.values()]
    print("\n将发给模型的 tools 数量:", len(tools))
    print(json.dumps(tools[0], ensure_ascii=False, indent=2)[:400], "...")


if __name__ == "__main__":
    dump("坏菜单：一个 do_stuff 包打天下", bad_specs())
    dump("好菜单：查单 / 政策 / 退款 分开，写清何时用、何时不用", good_specs())
    print("\n读法：模型点菜只看见 description。when_not 必须写进 description，不能只写在 wiki。")
