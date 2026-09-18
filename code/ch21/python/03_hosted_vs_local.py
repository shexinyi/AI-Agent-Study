"""示例 3：同一批问法，托管 Run vs 自建 Runtime。谁拥有状态？"""

from hosted import HostedRuntime
from local_runtime import answer, get_order

QUESTIONS = ("事假上限几天", "ORDER-88 能不能退", "TZ123 是哪张单", "年假能卖吗")


def hosted_answer(api: HostedRuntime, asst_id: str, q: str) -> tuple[str, str]:
    th = api.create_thread()
    api.add_message(th.id, "user", q)
    run = api.create_run(th.id, asst_id)
    if run.status == "requires_action":
        api.submit_tool_outputs(run.id, [{"tool": "get_order", "output": get_order(run.required_action["order_id"])}])
    last = api.threads[th.id].messages[-1].content
    return run.status, last


if __name__ == "__main__":
    api = HostedRuntime()
    asst = api.create_assistant("客服", ["get_order"])
    print(f"{'问法':20} {'托管status':16} {'自建拥有状态'}")
    for q in QUESTIONS:
        status, hosted = hosted_answer(api, asst.id, q)
        local = answer(q)
        same = hosted == local["answer"]
        print(f"{q:20} {status:16} 答案一致={same}")
        print(f"  托管最后一句: {hosted}")
        print(f"  自建 session : {local}")

    print("\n对照：托管省了轮询样板和对象存储；自建能在供应商日落后天仍回答。")
    print("排障：托管要查 Run/Step；自建查你自己的日志。面试两句都要会。")
