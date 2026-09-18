# 附录 C 推荐阅读与官方文档导航

> 缺演示回对应章，不要靠再买一本书。下列是 **官方入口** 和本教程章节的对照。链接会变，以域名首页为准。
>
> 教学假设：不收录破解、盗版、提示词注入攻击手册。第 29 章只讲防御口径。

---

## C.1 官方文档（先这些）

| 主题 | 入口 | 对照章节 |
| --- | --- | --- |
| OpenAI Responses / 迁移 | [Migrate to Responses](https://developers.openai.com/api/docs/guides/migrate-to-responses) | 第 5、7、21 章 |
| 模型页 | [Models](https://developers.openai.com/api/docs/models) | 附录 B |
| 推理模型 | [Reasoning](https://developers.openai.com/api/docs/guides/reasoning) | 第 28 章 |
| LangChain | [python.langchain.com](https://python.langchain.com/) | 第 17 章 |
| LangGraph | [LangGraph 文档](https://docs.langchain.com/langgraph) | 第 18 章 |
| LangSmith | [LangSmith](https://docs.smith.langchain.com/) | 第 26 章 |
| Chroma | [docs.trychroma.com](https://docs.trychroma.com/) | 第 10 章 |
| Milvus | [milvus.io/docs](https://milvus.io/docs) | 第 11 章 |
| OpenTelemetry | [opentelemetry.io](https://opentelemetry.io/) | 第 27 章 |
| FastAPI | [fastapi.tiangolo.com](https://fastapi.tiangolo.com/) | 第 32 章 |
| Spring | [spring.io/projects/spring-boot](https://spring.io/projects/spring-boot) | 第 32 章 |

AutoGen / CrewAI：第 19、20 章用的是课堂 Mini 实现，不是要求你 `pip install` 某一个快照。选型理由看第 22 章。

---

## C.2 按缺口回书，不按热度追更

| 你卡住的口令 | 回哪一章 |
| --- | --- |
| Agent vs Chatbot，意图 | 第 1–4、33 章 |
| RAG、引用、ACL、5 天 | 第 13–16、30 章 |
| 编排 vs Swarm、HITL | 第 23–25、31 章 |
| Trace、账单、401/429 | 第 26–28、32 章 |
| 注入、越权 | 第 29 章 |
| 30 分钟模拟、简历、开源 | 第 34 章 |

---

## C.3 不建议当成「唯一圣经」的

- 只讲框架商标、不讲金标和 HITL 的清单文  
- 把 99% 当卖点的博客  
- 教你复现攻击 payload 的帖子（简历 STAR 只写防御）  
- 过期的 Assistants 教程还当现行架构  

---

回到 [附录 B](./附录B-常用模型与接口速查.md) · 下一份 [附录 D](./附录D-三个简历项目开源仓库模板.md)
