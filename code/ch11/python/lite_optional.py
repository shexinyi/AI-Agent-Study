"""
可选：连真正的 Milvus Lite。失败则打印原因，不影响前三例。

    python lite_optional.py
"""

from __future__ import annotations

from pathlib import Path

from embed import DIM, embed

DB = Path(__file__).resolve().parent / "data" / "lite.db"


def main() -> None:
    try:
        from pymilvus import MilvusClient
    except Exception as exc:
        print(f"未安装或无法导入 pymilvus: {type(exc).__name__}: {exc}")
        print("安装：python -m pip install -r requirements.txt")
        return
    try:
        if DB.exists():
            DB.unlink()
        DB.parent.mkdir(parents=True, exist_ok=True)
        client = MilvusClient(uri=str(DB))
        name = "agent_memory"
        if client.has_collection(name):
            client.drop_collection(name)
        client.create_collection(collection_name=name, dimension=DIM, metric_type="COSINE")
        text = "我喜欢窗口靠过道的工位"
        client.insert(name, [{"id": 1, "vector": embed(text), "text": text}])
        hits = client.search(name, data=[embed("靠过道的工位")], limit=1, output_fields=["text"])
        print("Lite 后端: 本地 .db 文件")
        print("search:", hits)
        print("注意：Milvus Lite 不支持 Partition。生产分区请上 Standalone / 集群。")
    except Exception as exc:
        print(f"Lite 不可用（{type(exc).__name__}: {exc}）")
        print("常见原因：Windows 缺运行库、公司策略拦截本地引擎。用前三例即可学完本章。")


if __name__ == "__main__":
    main()
