package com.agentbook.ch17;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.stream.Collectors;

/**
 * 教学脚本模型：按 Prompt 里已有的原文抽取回答。生产里换成 OpenAiChatModel 等。
 */
public final class ScriptedChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        String blob = chatRequest.messages().stream().map(ScriptedChatModel::textOf).collect(Collectors.joining("\n"));
        String question = "";
        if (!chatRequest.messages().isEmpty()) {
            question = textOf(chatRequest.messages().get(chatRequest.messages().size() - 1));
        }
        return ChatResponse.builder().aiMessage(AiMessage.from(reply(blob, question))).build();
    }

    static String reply(String blob, String question) {
        if (blob.contains("只输出 JSON")) {
            if (blob.contains("TZ123") || blob.contains("ORDER-99")) {
                return "{\"tool\":\"get_order\",\"order_id\":\"ORDER-99\"}";
            }
            return "{\"tool\":\"get_order\",\"order_id\":\"ORDER-88\"}";
        }
        if (blob.contains("【无资料】")) {
            return "【不足】资料不够，不能编天数或单号状态。";
        }
        if ((question.contains("年假") || question.contains("食堂")) && !blob.replace(question, "").contains("年假")) {
            return "【不足】资料不够，不能编天数或单号状态。";
        }
        String lower = blob.toLowerCase();
        if (lower.contains("refundable") && lower.contains("false")) {
            return "根据工具，该单已发货，对话里不能直接退款，须走售后。";
        }
        if (lower.contains("refundable") && lower.contains("true")) {
            return "根据工具，该单未发货，用户本人可申请整单退款。";
        }
        if (blob.contains("10 个工作日")) {
            return "根据资料，事假每年累计不超过 10 个工作日。";
        }
        if (blob.contains("未发货") && blob.contains("ORDER-88")) {
            return "根据资料，ORDER-88 未发货时可申请整单退款。";
        }
        if (blob.contains("TZ123") || (blob.contains("已发货") && blob.contains("ORDER-99"))) {
            return "根据资料，TZ123 对应 ORDER-99，已发货须走售后。";
        }
        return "【不足】资料里没有可引用的句子。";
    }

    private static String textOf(ChatMessage message) {
        if (message instanceof UserMessage user) {
            return user.singleText();
        }
        if (message instanceof SystemMessage system) {
            return system.text();
        }
        if (message instanceof AiMessage ai) {
            return ai.text();
        }
        return String.valueOf(message);
    }
}
