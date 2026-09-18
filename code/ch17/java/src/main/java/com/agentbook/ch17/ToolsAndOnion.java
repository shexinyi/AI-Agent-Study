package com.agentbook.ch17;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.List;
import java.util.Map;

public class ToolsAndOnion {
    public static void main(String[] args) {
        ScriptedChatModel model = new ScriptedChatModel();
        PromptTemplate intent = PromptTemplate.from(
                "你是路由。只输出 JSON，不要其它字。\n问：{{question}}");
        PromptTemplate answer = PromptTemplate.from(
                "根据工具结果用一句话回答。工具结果：{{tool_result}}\n问：{{question}}");

        System.out.println("模式 3：模型输出是意图，get_order 才是事实。");
        for (String q : List.of("ORDER-88 能不能退", "TZ123 那张单能直接退吗")) {
            System.out.println("\n问: " + q);
            System.out.println("答: " + run(model, intent, answer, q));
        }

        System.out.println("\n手写 == 链 ? " + run(model, intent, answer, "ORDER-88 能不能退")
                .equals(run(model, intent, answer, "ORDER-88 能不能退")));

        System.out.println("\n--- 把模型换成一句不是 JSON 的话 ---");
        ChatModel broken = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                return ChatResponse.builder().aiMessage(AiMessage.from("当然可以退，三天到账。")).build();
            }
        };
        try {
            String raw = broken.chat(ChatRequest.builder()
                    .messages(SystemMessage.from(intent.apply(Map.of("question", "ORDER-88 能不能退")).text()),
                            UserMessage.from("ORDER-88 能不能退"))
                    .build()).aiMessage().text();
            if (!Kit.isJsonObject(raw)) {
                throw new IllegalArgumentException("不是 JSON: " + raw);
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("异常: " + ex.getClass().getSimpleName());
            System.out.println("根因: 模型吐了自然语言，解析器不认。");
            System.out.println("这里没有 Runnable 洋葱，异常就在这一行——这就是手写的好处。");
        }
        System.out.println("\n读法：LangChain4j 的 AiServices 会把这几步藏进接口代理。拆不回来就不要写精通。");
    }

    static String run(ScriptedChatModel model, PromptTemplate intent, PromptTemplate answer, String question) {
        String routed = model.chat(ChatRequest.builder()
                .messages(SystemMessage.from(intent.apply(Map.of("question", question)).text()),
                        UserMessage.from(question))
                .build()).aiMessage().text();
        if (!Kit.isJsonObject(routed)) {
            throw new IllegalArgumentException("不是 JSON: " + routed);
        }
        String fact = Kit.runTool(routed);
        return model.chat(ChatRequest.builder()
                .messages(SystemMessage.from(answer.apply(Map.of("question", question, "tool_result", fact)).text()),
                        UserMessage.from(question))
                .build()).aiMessage().text();
    }
}
