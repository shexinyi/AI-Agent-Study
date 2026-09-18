"""模式 3：工具链。再故意看一层封装破裂时栈有多深。"""

from langchain_core.exceptions import OutputParserException
from langchain_core.language_models.fake_chat_models import FakeListChatModel
from langchain_core.output_parsers import JsonOutputParser, StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda

from kit import PolicyChat, graph_names, run_tool

intent_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            "你是路由。只输出 JSON，不要其它字：{{\"tool\":\"get_order\",\"order_id\":\"ORDER-xx\"}}",
        ),
        ("human", "{question}"),
    ]
)
answer_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", "根据工具结果用一句话回答用户。工具结果：{tool_result}"),
        ("human", "{question}"),
    ]
)

tool_chain = (
    {
        "question": RunnableLambda(lambda x: x["question"]),
        "payload": intent_prompt | PolicyChat() | JsonOutputParser(),
    }
    | RunnableLambda(lambda x: {**x, "tool_result": run_tool(x["payload"])})
    | answer_prompt
    | PolicyChat()
    | StrOutputParser()
)

print("模式 3：模型输出是意图，get_order 才是事实。和第 4、7 章同一句话。")
print("洋葱节点:", " → ".join(graph_names(tool_chain)))

for q in ("ORDER-88 能不能退", "TZ123 那张单能直接退吗"):
    print(f"\n问: {q}")
    print("答:", tool_chain.invoke({"question": q}))


def handwritten(question: str) -> str:
    """同一条业务，不套 Runnable。出了错你知道在哪一行。"""
    routed = PolicyChat().invoke(intent_prompt.invoke({"question": question}))
    payload = JsonOutputParser().invoke(routed)
    fact = run_tool(payload)
    return StrOutputParser().invoke(
        PolicyChat().invoke(answer_prompt.invoke({"question": question, "tool_result": fact}))
    )


print("\n手写 == 链 ?", handwritten("ORDER-88 能不能退") == tool_chain.invoke({"question": "ORDER-88 能不能退"}))

print("\n--- 把模型换成一句不是 JSON 的话 ---")
broken = intent_prompt | FakeListChatModel(responses=["当然可以退，三天到账。"]) | JsonOutputParser()
try:
    broken.invoke({"question": "ORDER-88 能不能退"})
except OutputParserException as exc:
    import traceback

    text = "".join(traceback.format_exception(exc))
    related = [ln.strip() for ln in text.splitlines() if "runnable" in ln.lower() or "JsonOutputParser" in ln]
    print("异常:", type(exc).__name__)
    print("根因: 模型吐了自然语言，JsonOutputParser 不认。")
    print("栈里 Runnable/Parser 相关行:", len(related))
    for ln in related[:8]:
        print("  ", ln[:120])

print("\n读法：封装省样板，也藏调用栈。拆不回手写，就不要写进简历「精通 LangChain」。")
