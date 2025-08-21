package com.aiassist.ai.core.ai;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenAiClient {
    @Autowired
    OpenAiChatModel openAiChatModel;

    public ChatResponse chat(String userMessage) {
        ChatResponse resp = openAiChatModel.chat(UserMessage.from(userMessage));

        String text = resp.aiMessage().text();
        TokenUsage usage = resp.tokenUsage();        // 可能为 null

        log.info("text={}, tokenUsage={}", text, usage);
        if (usage != null) {
            System.out.printf("prompt=%d, completion=%d, total=%d%n",
                    usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount());
        }
        return resp;
    }

    public String chatStr(String userMessage) {
        ChatResponse resp = openAiChatModel.chat(UserMessage.from(userMessage));

        String text = resp.aiMessage().text();
        TokenUsage usage = resp.tokenUsage();        // 可能为 null

        log.info("text={}, tokenUsage={}", text, usage);
        if (usage != null) {
            System.out.printf("prompt=%d, completion=%d, total=%d%n",
                    usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount());
        }
        return text;
    }
}
