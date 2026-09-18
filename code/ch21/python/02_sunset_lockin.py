"""示例 2：只存 thread_id 的人，日落之后两手空空。"""

import json
from pathlib import Path

from hosted import SUNSET, HostedRuntime
from local_runtime import get_order

HERE = Path(__file__).resolve().parent
EXPORT = HERE / "exported_thread.json"


def finish(api: HostedRuntime, asst_id: str, question: str) -> str:
    th = api.create_thread()
    api.add_message(th.id, "user", question)
    run = api.create_run(th.id, asst_id)
    if run.status == "requires_action":
        oid = run.required_action["order_id"]
        api.submit_tool_outputs(run.id, [{"tool": "get_order", "output": get_order(oid)}])
    return th.id


if __name__ == "__main__":
    api = HostedRuntime()
    asst = api.create_assistant("客服", ["get_order"])
    tid = finish(api, asst.id, "ORDER-88 能不能退")
    print("云端 Thread", tid)
    print("偷懒的人只把这个 id 写进数据库。")

    snapshot = api.export_thread(tid)
    EXPORT.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
    print("勤快的人导出消息到", EXPORT.name)

    print(f"\n--- 模拟 {SUNSET} 日落 ---")
    api.sunset()
    try:
        api.threads[tid]
    except KeyError:
        print("云端字典已空。用旧 thread_id 取值：KeyError / 404")
    try:
        api.add_message(tid, "user", "还在吗")
    except RuntimeError as exc:
        print("再调 API:", exc)

    print("本地导出还在:")
    print(EXPORT.read_text(encoding="utf-8"))
    print("读法：托管记忆很省事，直到供应商改对象模型或关停。你没拷走的消息，不是你的。")
