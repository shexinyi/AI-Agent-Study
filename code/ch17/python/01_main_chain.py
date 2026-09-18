"""模式 1：Prompt | Model | Parser。制度写进模板，不检索。"""

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

from kit import PolicyChat, graph_names

prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            "只根据下面制度回答。不许编数字。\n制度：事假每年累计不超过 10 个工作日。",
        ),
        ("human", "{question}"),
    ]
)
chain = prompt | PolicyChat() | StrOutputParser()

print("主链五件套：Prompt → Model → Output Parser。Tool / 检索这一步还没上场。")
print("图节点:", " → ".join(graph_names(chain)))

for q in ("事假上限几天", "年假能卖吗"):
    print(f"\n问: {q}")
    print("答:", chain.invoke({"question": q}))

print("\n读法：LangChain 省的是胶水。智力仍来自模型（本章是脚本模型）。")
