"""模式 2：LCEL 把检索接进同一条管道。"""

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda, RunnablePassthrough

from kit import PolicyChat, retrieve

prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            "只根据资料回答。没有资料就说【不足】，不许用童年记忆。\n资料：\n{context}",
        ),
        ("human", "{question}"),
    ]
)


def with_context(question: str) -> dict:
    ctx = retrieve(question)
    return {"question": question, "context": ctx if ctx else "【无资料】"}


chain = RunnableLambda(with_context) | prompt | PolicyChat() | StrOutputParser()

print("LCEL：左边的输出是右边的输入。检索是链上的一截，不是另一种魔法。")

for q in ("事假每年累计不超过几个工作日", "ORDER-88 能不能退", "食堂几点开门"):
    print(f"\n问: {q}")
    print("检索:", retrieve(q) or "（空）")
    print("答:", chain.invoke(q))

# 对照：不用 | ，自己调三步，结果必须一样
q = "事假每年累计不超过几个工作日"
hand = StrOutputParser().invoke(PolicyChat().invoke(prompt.invoke(with_context(q))))
print("\n手写三步 == LCEL ?", hand == chain.invoke(q))
print("读法：| 是语法糖。面试时能把链拆回函数调用，才算会。")
