package com.agentbook.ch17;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.input.PromptTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SequentialRag {
    public static void main(String[] args) {
        ScriptedChatModel model = new ScriptedChatModel();
        PromptTemplate prompt = PromptTemplate.from(
                "只根据资料回答。没有资料就说【不足】。\n资料：\n{{context}}\n问：{{question}}");
        Path kb = Path.of("kb");
        System.out.println("同构 LCEL：检索 → 填模板 → ChatModel。拆开就是三步函数。");

        for (String q : List.of("事假每年累计不超过几个工作日", "ORDER-88 能不能退", "食堂几点开门")) {
            String ctx = Kit.retrieve(q, kb);
            String shown = ctx.isBlank() ? "【无资料】" : ctx;
            String filled = prompt.apply(Map.of("context", shown, "question", q)).text();
            String answer = model.chat(ChatRequest.builder()
                    .messages(SystemMessage.from(filled), UserMessage.from(q))
                    .build()).aiMessage().text();
            System.out.println("\n问: " + q);
            System.out.println("检索: " + (ctx.isBlank() ? "（空）" : ctx));
            System.out.println("答: " + answer);
        }

        String q = "事假每年累计不超过几个工作日";
        String ctx = Kit.retrieve(q, kb);
        String a = model.chat(ChatRequest.builder().messages(
                SystemMessage.from(prompt.apply(Map.of("context", ctx, "question", q)).text()),
                UserMessage.from(q)).build()).aiMessage().text();
        System.out.println("\n手写三步可复现: " + a);
        System.out.println("读法：没有 | 也要把检索当成链上的一截，而不是另一种框架。");
    }
}
