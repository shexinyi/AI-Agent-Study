package com.agentbook.ch17;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.List;
import java.util.Map;

public class MainChain {
    public static void main(String[] args) {
        ScriptedChatModel model = new ScriptedChatModel();
        PromptTemplate prompt = PromptTemplate.from(
                "只根据下面制度回答。不许编数字。\n制度：事假每年累计不超过 10 个工作日。\n问：{{question}}");
        System.out.println("主链五件套：PromptTemplate → ChatModel → 取 aiMessage 文本。");
        System.out.println("Java 没有 | 运算符；LangChain4j 用 ChatRequest 把消息递进去。");

        for (String q : List.of("事假上限几天", "年假能卖吗")) {
            String filled = prompt.apply(Map.of("question", q)).text();
            String answer = model.chat(ChatRequest.builder()
                    .messages(SystemMessage.from(filled), UserMessage.from(q))
                    .build()).aiMessage().text();
            System.out.println("\n问: " + q);
            System.out.println("答: " + answer);
        }
        System.out.println("\n读法：LangChain4j 省的是消息拼装。智力仍来自模型（本章是脚本模型）。");
    }
}
