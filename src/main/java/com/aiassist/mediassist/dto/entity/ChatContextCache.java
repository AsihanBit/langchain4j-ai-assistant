package com.aiassist.mediassist.dto.entity;

import dev.langchain4j.data.message.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatContextCache {
    private String memoryId;
    private List<ChatMessage> messages;  // 聊天消息的上下文列表
    private int contextSize;            // 当前上下文的数量

    // 增加消息方法（如果需要定制逻辑）
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.contextSize++;
    }
}
