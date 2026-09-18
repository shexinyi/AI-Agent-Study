"""示例 2：点对点、主题、广播。广播会让食堂岗听见订单。"""

from kit import cafe_expert, expect_ok, leave_expert, order_expert
from miniswarm import BusSwarm

ORDER_Q = "查一下 ORDER-88 能不能退。"


def run(mode: str) -> list[str]:
    world = BusSwarm()
    hallucinate = mode == "broadcast"
    world.add(leave_expert())
    world.add(order_expert())
    world.add(cafe_expert(hallucinate=hallucinate))
    if mode == "p2p":
        world.bus.send_p2p("订单专家", ORDER_Q, "前台")
    elif mode == "topic":
        world.bus.publish("orders", ORDER_Q, "前台")
    else:
        world.bus.publish("all", ORDER_Q, "前台")
    answers = world.deliver()
    print(f"=== {mode} ===")
    for line in world.bus.log:
        print(f"  总线 {line}")
    if not answers:
        print("  无人回")
    for line in answers:
        print(f"  {line}")
    print()
    return answers


if __name__ == "__main__":
    print("点对点必须知道对方名字。主题按订阅扇出。广播 #all 是污染源。")
    print("教学总线在进程里，不是 Kafka。\n")

    p2p = run("p2p")
    topic = run("topic")
    broadcast = run("broadcast")

    def joined(rows: list[str]) -> str:
        return "\n".join(rows)

    checks = [
        ("点对点只有订单岗", len(p2p) == 1 and expect_ok(joined(p2p), "has_88")),
        ("主题只有订单岗", len(topic) == 1 and expect_ok(joined(topic), "has_88")),
        ("广播食堂也开口", any(x.startswith("食堂专家:") for x in broadcast)),
        ("广播编了红烧肉", expect_ok(joined(broadcast), "has_pork")),
        ("点对点不编菜单", expect_ok(joined(p2p), "no_pork")),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：消息发给谁，比「我们上了多智能体」更重要。默认点对点或窄主题。")
