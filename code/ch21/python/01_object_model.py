"""示例 1：Assistant / Thread / Run / requires_action。"""

from hosted import HostedRuntime
from local_runtime import get_order

if __name__ == "__main__":
    api = HostedRuntime()
    asst = api.create_assistant(
        "只根据制度或工具回答。不编天数。写退款须确认。",
        tools=["get_order"],
    )
    print("Assistant", asst.id, "tools=", asst.tools)

    for q in ("事假上限几天", "ORDER-88 能不能退"):
        th = api.create_thread()
        api.add_message(th.id, "user", q)
        run = api.create_run(th.id, asst.id)
        print(f"\n问: {q}")
        print("Run", run.id, "status=", run.status, "required=", run.required_action)
        if run.status == "requires_action":
            oid = run.required_action["order_id"]
            run = api.submit_tool_outputs(run.id, [{"tool": "get_order", "output": get_order(oid)}])
            print("submit_tool_outputs →", run.status)
        print("Thread 消息:")
        for m in api.threads[th.id].messages:
            print(f"  {m.role}: {m.content}")

    print("\n读法：托管 Runtime 把循环收成 Run 状态机。requires_action 仍是第 4 章那句话：模型输出是意图。")
