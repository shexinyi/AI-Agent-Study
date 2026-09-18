# 附录 B 常用模型与接口速查

> 查表用。**截止日期：2026-09。** 模型 slug 会变，面试说「我对供应商模型页和账单」，不要把本表当永久官网。
>
> 教学假设：全书主路径 **无 API Key**。接真模型前先跑章内 Mock。单价以账单为准，不用教学币对账。Assistants API 已于 **2026-08-26** 关停。

---

## B.1 先选接口，再选模型

| 接口 | 英文 / 路径 | 现在怎么用 |
| --- | --- | --- |
| Responses | `POST /v1/responses` | **新项目首选**。工具、推理、多轮用它 |
| Conversations | Conversations API | 继任 Thread：会话权威副本仍建议自建库 |
| Chat Completions | `POST /v1/chat/completions` | 仍支持；存量代码常见。新 Agent 不优先 |
| Assistants | `v1/assistants` 等 | **已日落**。简历不要写「线上还在跑 Assistants」 |
| Embeddings | `v1/embeddings` | RAG 向量；本书教学哈希不是它 |

旧对象 → 新对象（第 21 章）：

| 已日落 | 继任 |
| --- | --- |
| Assistant | Prompt |
| Thread | Conversation |
| Run | Response |
| Run Step | Item |

`previous_response_id` 用来续轮。公司若要求不落供应商盘，看官方 `store: false` / 零保留条款，不要猜。

---

## B.2 模型怎么开口（2026-09 快照）

开口先报 **用途**，再报 slug。slug 以官网 [Models](https://developers.openai.com/api/docs/models) 为准。

| 用途 | 课堂怎么说 | 常见去处（会变） |
| --- | --- | --- |
| 办理决策 / 退款闸门前 | 大模型 + HITL，不自动执行 | GPT-5.x 推理档；Responses |
| 分类 / 路由 / 食堂【不足】 | 小模型分流，白名单无 `refund_order` | 低成本档；第 28 章 cascade |
| 制度问答 | 先检索再生成，闭卷不准报天数 | 任意生成模型 + 你的检索 |
| 向量 | 生产 embedding，不是 MD5 教学哈希 | `v1/embeddings` 或开源向量模型 |

OpenAI 推理模型官方倾向 **Responses**。Chat Completions 在部分高 `reasoning_effort` + 工具组合上已收窄——以当周文档为准。

国产 / 开源兼容口：只要是 **OpenAI 兼容** 的 `base_url`，章内协议（`tools` / `tool_calls`）仍能对上。换供应商，执行器和金标不换。

Anthropic、Google 等各有 Messages / 原生工具协议。映射层写在你进程里，不要让业务代码直接绑一家字段名。

---

## B.3 账单四列（对接口时仍用）

输入、输出、重试、检索。流式几乎只改 **TTFT**。3 秒超时先看工具切片。教学币 9.28 / 1.96 只出现在第 32 章对照，**不能写进报价单**。

---

## B.4 最小请求形状（教学，不保证字段永不改）

Responses 思路：`model` + `input` + `tools`，读 `output` 里的 `function_call`，把结果当 `function_call_output` 交回。

Chat Completions 思路：`messages` + `tools`，读 `tool_calls`，`arguments` 可能是 **字符串或对象**（第 33 章解析题）。

无论哪一家：**解析失败要有错误码，money 工具 fail-closed。**

---

回到 [附录 A](./附录A-术语表.md) · 下一份 [附录 C](./附录C-推荐阅读与官方文档导航.md)
