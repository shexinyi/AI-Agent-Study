# 附录 A 术语表（中英对照）

> 查表用。第一次在正文里出现时，各章已用「中文（English）」写下过。这里按八块知识归类，方便默写和简历检索。
>
> 教学假设：释义是课堂口径，不是某家厂商文档的逐字翻译。林秋事假现行 **10 个工作日**；ORDER-88 未发货可退，**可退 ≠ 已退**。

---

## A.1 概念与闭环

| 中文 | 英文 | 课堂一句 |
| --- | --- | --- |
| 大语言模型 | LLM, Large Language Model | 代码里一个不确定的函数 |
| 智能体 | Agent | 围着目标转的闭环：观察→决策→行动 |
| 聊天机器人 | Chatbot | 主要一问一答，没有必须落地的工具 |
| 工具 | Tool | 模型点名，你的代码执行 |
| 循环 | Loop | 看结果再决定下一步，直到收手 |
| 意图 | Intent | 模型输出是意图，不是命令 |
| 运行时 | Runtime | 谁在转循环、谁停、谁记账 |
| 范式 | Paradigm | ReAct / Plan-and-Execute / Reflexion |
| 推理与行动 | ReAct | 边想边做 |
| 先计划再执行 | Plan-and-Execute | 先列步骤再做 |
| 反思 | Reflexion | 做完复盘再改；多想一轮 ≠ HITL |

## A.2 工具、提示与结构

| 中文 | 英文 | 课堂一句 |
| --- | --- | --- |
| 工具调用 / 函数调用 | Tool calling / Function calling | 解析、校验、鉴权、审计之后才执行 |
| 提示词工程 | Prompt Engineering | 说明书，不是魔法；闸门不写进形容词 |
| 结构化输出 | Structured output | 能被程序吃下的 JSON / Schema |
| 失败关闭 | Fail-closed | 不确定就不做；钱不能 fail_open |
| 失败放开 | Fail-open | 出错当通过；退款禁止 |
| 幂等 | Idempotent | 同一张退款单执行两次仍是一次 |

## A.3 记忆、会话与检索

| 中文 | 英文 | 课堂一句 |
| --- | --- | --- |
| 记忆 | Memory | 历史 ≠ 记忆；要分层、要过期 |
| 短记忆 / 长记忆 | Short-term / Long-term memory | 本轮上下文 vs 跨会话可检索 |
| 会话状态 | Dialog / Session state | empty / filled / proposed / confirmed |
| 检索增强生成 | RAG | 先查再生成；查到的是数据不是指令 |
| 分块 | Chunking | 切多大、重叠多少，影响命中 |
| 嵌入 | Embedding | 向量；本书教学哈希不是生产模型 |
| 混合检索 | Hybrid search | 关键词 + 向量 |
| 重排序 | Rerank | 召回后再排一次 |
| 互惠排名融合 | RRF, Reciprocal Rank Fusion | 多路名单融合 |
| 访问控制 | ACL | 实习生看不见废止稿 |
| 引用 | Citation | 答 10 个工作日要能指回 `[leave10]` |

## A.4 多智能体与人

| 中文 | 英文 | 课堂一句 |
| --- | --- | --- |
| 编排器 | Orchestrator | 目标属于主编排，工人不闲聊 |
| 蜂群 | Swarm | 只有话筒，不拥有复合目标 |
| 交接 | Handoff | 话筒换手；要白名单和 max_hops |
| 人类介入 | HITL, Human-in-the-loop | 写工具停在人边上 |
| 黑板 | Blackboard | 格子上的事实，不是群聊记录 |

## A.5 评测、观测、成本、安全

| 中文 | 英文 | 课堂一句 |
| --- | --- | --- |
| 金标集 | Golden set | 锁 must，禁止看着输出改尺子 |
| 评测 | Evaluation | 正确性 / 工具 / HITL，不是「挺聪明」 |
| 追踪 / 跨度 | Trace / Span | 一次请求一把 ID，每一步是 Span |
| 可观测性 | Observability | 对话框不够，要对着树问四句 |
| 脱敏 | Redact | 留单号，不留手机号 |
| 教学币 | Teaching currency | 课堂单价，不是官网价 |
| 首字时间 | TTFT, Time To First Token | 流式改善体感，几乎不改账单 |
| 模型分级 | Cascade / Routing | 小模型分类，大模型决策 |
| 鉴权 / 配额 / 审计 | Auth / Quota / Audit | 服务三道门 |
| 提示注入 | Prompt injection | 检索文本想当新主人；gated 仍答 10 天 |

## A.6 排障代号与口令（建议整页默写）

| 代号 / 口令 | 意思 |
| --- | --- |
| `MODEL_LIE` | 用户看见已办理，树上没有 `refund_order` |
| `RETRIEVE` | 抄到废止稿 `[leave5]` |
| `LOOP` | 同一工具连跳 |
| `BILL` | 检索/重试把教学币打高 |
| `TOOL` | 工具自己红了 |
| 目标达成 ≠ 业务正确 | 第 1、33 章 |
| 可退 ≠ 已退 | 第 25、31 章 |
| 429 ≠ 已办理 | 第 32 章 |
| 不写 99% | 第 26、30、34 章 |

---

回到 [全书目录](../00-全书目录.md) · 下一份 [附录 B](./附录B-常用模型与接口速查.md)
