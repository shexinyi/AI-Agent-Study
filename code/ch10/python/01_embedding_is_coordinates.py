"""
第 10 章示例 1：Embedding 是坐标，不是「懂中文」。

    python 01_embedding_is_coordinates.py
"""

from __future__ import annotations

from embed import DIM, cosine, embed, tokens

SENTENCES = [
    "我喜欢窗口靠过道的工位",
    "工位请安排在走廊一侧",
    "ORDER-88 还没发货，申请退款",
    "我的密码是 123456",
]


def preview(vec: list[float], n: int = 6) -> str:
    head = ", ".join(f"{x:+.2f}" for x in vec[:n])
    return f"[{head}, ... ] dim={len(vec)}"


if __name__ == "__main__":
    print(f"教学嵌入维度: {DIM}（哈希坐标，不是 OpenAI / MiniLM）\n")
    vectors = []
    for text in SENTENCES:
        vec = embed(text)
        vectors.append(vec)
        print(f"文本: {text}")
        print(f"  词元: {tokens(text)}")
        print(f"  向量: {preview(vec)}\n")

    query = "他坐哪边比较靠过道？"
    q = embed(query)
    print(f"查询: {query}")
    ranked = sorted(
        ((cosine(q, vec), text) for vec, text in zip(vectors, SENTENCES)),
        reverse=True,
    )
    print("按余弦相似度从高到低：")
    for score, text in ranked:
        print(f"  {score:+.3f}  {text}")
    print("\n读法：措辞不同但都在说座位的两句更近；订单和密码更远。")
    print("哈希嵌入能演示几何，不能当生产语义模型用。")
