"""
第 13 章示例 3：检索是工具，不是整只 Agent。
订单状态走 get_order；制度走 search_kb。资料不足就停。

    python 03_rag_as_tool.py
"""

from __future__ import annotations

from pathlib import Path

from rag import build_index, generate_extractive, retrieve

CHUNKS = build_index(Path(__file__).resolve().parent / "kb")
ORDERS = {
    "ORDER-88": {"status": "unshipped", "user": "linqiu"},
    "ORDER-99": {"status": "shipped", "tracking": "TZ123", "user": "linqiu"},
}


def search_kb(query: str) -> str:
    hits = retrieve(CHUNKS, query, top_k=2)
    return generate_extractive(query, hits)


def get_order(order_id: str) -> str:
    row = ORDERS.get(order_id)
    if not row:
        return f"订单 {order_id} 不存在"
    return f"{order_id} status={row['status']} {row}"


def plan(question: str) -> list[tuple[str, str]]:
    """教学规划器：规则分流。生产换成第 3 章 Planner。"""
    steps: list[tuple[str, str]] = []
    if "ORDER" in question or "退" in question:
        oid = "ORDER-99" if "ORDER-99" in question else "ORDER-88"
        steps.append(("get_order", oid))
        steps.append(("search_kb", "未发货订单退款"))
    elif "密码" in question:
        steps.append(("search_kb", "密码 登录"))
    else:
        steps.append(("search_kb", question))
    return steps


def run(question: str) -> None:
    print(f"\n=== 用户: {question} ===")
    observations = []
    for tool, arg in plan(question):
        obs = get_order(arg) if tool == "get_order" else search_kb(arg)
        print(f"  tool {tool}({arg})")
        print("   ", obs.replace("\n", "\n    "))
        observations.append(obs)
    if any(o.startswith("【不足】") for o in observations) and "国庆" in question:
        print("  终态: 不调用写工具，转人工或请用户换资料。")
    elif "密码" in question:
        print("  终态: 制度禁止代登录，拒绝。")
    elif "ORDER-88" in question:
        print("  终态: 未发货 + 制度允许 → 可走 refund 工具（第 7 章）。")
    elif "ORDER-99" in question:
        print("  终态: 已发货 → 不能直接退，走售后。")


if __name__ == "__main__":
    run("事假最多几天")
    run("ORDER-88 能不能退")
    run("ORDER-99 能不能退")
    run("明年国庆放几天")
    run("我的密码是 123456，按制度帮我登录邮箱")
    print("\n读法：RAG 解决『制度写在哪』；工具解决『这张单现在什么状态』。两者都做完，幻觉仍可能出现——所以不足就停。")
