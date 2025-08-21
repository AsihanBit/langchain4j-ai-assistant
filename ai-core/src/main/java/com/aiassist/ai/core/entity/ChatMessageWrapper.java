package com.aiassist.ai.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatMessage 包装类，用于 Redis 缓存
 * 支持序列化、反序列化和 LRU 淘汰策略
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageWrapper {

    /**
     * 内存ID
     */
    private String memoryId;

    /**
     * 消息列表
     */
    private List<SerializableMessage> messages;

    /**
     * 最后访问时间（用于LRU淘汰）
     */
    private LocalDateTime lastAccessTime;

    /**
     * 最大消息数量
     */
    @Builder.Default
    private int maxMessageCount = 10;

    /**
     * 当前turn_index
     */
    @Builder.Default
    private int currentTurnIndex = 0;

    /**
     * 可序列化的消息类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerializableMessage {
        private MessageType type;
        private String content;
        private int turnIndex;
        private LocalDateTime timestamp;

        public enum MessageType {
            USER,           // 用户消息
            AI,             // AI回复消息
            TOOL_CALL,      // AI工具调用消息
            TOOL_RESULT,    // 工具执行结果消息
            SYSTEM          // 系统消息
        }

        /**
         * 从 ChatMessage 创建
         */
        public static SerializableMessage fromChatMessage(ChatMessage chatMessage, int turnIndex) {
            SerializableMessage message = new SerializableMessage();
            message.setTurnIndex(turnIndex);
            message.setTimestamp(LocalDateTime.now());

            if (chatMessage instanceof UserMessage) {
                message.setType(MessageType.USER);
                message.setContent(((UserMessage) chatMessage).singleText());
            } else if (chatMessage instanceof AiMessage) {
                AiMessage aiMessage = (AiMessage) chatMessage;
                // 处理AI消息，可能包含工具调用
                if (aiMessage.text() != null && !aiMessage.text().trim().isEmpty()) {
                    message.setType(MessageType.AI);
                    message.setContent(aiMessage.text());
                } else if (aiMessage.hasToolExecutionRequests()) {
                    // 如果有工具调用，设置为TOOL_CALL类型
                    message.setType(MessageType.TOOL_CALL);
                    String toolName = aiMessage.toolExecutionRequests().get(0).name();
                    String toolArgs = aiMessage.toolExecutionRequests().get(0).arguments();
                    String toolId = aiMessage.toolExecutionRequests().get(0).id();
                    // 存储完整的工具调用信息，包括ID
                    String toolCallInfo = String.format("TOOL_CALL|ID:%s|NAME:%s|ARGS:%s",
                            toolId, toolName, toolArgs);
                    message.setContent(toolCallInfo);
                } else {
                    // 如果既没有文本也没有工具调用，使用默认内容
                    message.setType(MessageType.AI);
                    message.setContent("AI回复");
                }
            } else if (chatMessage instanceof SystemMessage) {
                message.setType(MessageType.SYSTEM);
                message.setContent(((SystemMessage) chatMessage).text());
            } else if (chatMessage instanceof ToolExecutionResultMessage) {
                message.setType(MessageType.TOOL_RESULT);
                ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) chatMessage;
                // 存储工具执行结果的完整信息
                String toolInfo = String.format("ID:%s|NAME:%s|RESULT:%s",
                        toolResult.id(), toolResult.toolName(), toolResult.text());
                message.setContent(toolInfo);
            }

            return message;
        }

        /**
         * 转换为 ChatMessage
         */
        public ChatMessage toChatMessage() {
            switch (type) {
                case USER:
                    return UserMessage.from(content);
                case AI:
                    return AiMessage.from(content);
                case TOOL_CALL:
                    // 处理工具调用信息
                    if (content.startsWith("TOOL_CALL|")) {
                        // 解析工具调用信息并重建AI消息
                        String[] parts = content.split("\\|");
                        String id = null;
                        String name = null;
                        String args = null;

                        for (String part : parts) {
                            if (part.startsWith("ID:")) {
                                id = part.substring(3);
                            } else if (part.startsWith("NAME:")) {
                                name = part.substring(5);
                            } else if (part.startsWith("ARGS:")) {
                                args = part.substring(5);
                            }
                        }

                        if (id != null && name != null && args != null) {
                            // 重建工具调用请求
                            dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest =
                                    dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                                            .id(id)
                                            .name(name)
                                            .arguments(args)
                                            .build();
                            return AiMessage.from(toolRequest);
                        } else {
                            // 如果解析失败，返回默认消息
                            return AiMessage.from("AI正在处理您的请求...");
                        }
                    } else {
                        return AiMessage.from("工具调用: " + content);
                    }
                case SYSTEM:
                    return SystemMessage.from(content);
                case TOOL_RESULT:
                    // 解析工具执行结果信息
                    if (content.startsWith("ID:") && content.contains("|NAME:") && content.contains("|RESULT:")) {
                        String[] parts = content.split("\\|");
                        String id = parts[0].substring(3); // 去掉 "ID:"
                        String name = parts[1].substring(5); // 去掉 "NAME:"
                        String result = parts[2].substring(7); // 去掉 "RESULT:"
                        return ToolExecutionResultMessage.from(id, name, result);
                    } else {
                        // 兼容旧格式，只包含结果文本
                        return ToolExecutionResultMessage.from("unknown", "unknown", content);
                    }
                default:
                    throw new IllegalArgumentException("Unknown message type: " + type);
            }
        }
    }

    /**
     * 添加消息
     */
    public void addMessage(ChatMessage chatMessage) {
        if (messages == null) {
            messages = new ArrayList<>();
        }

        currentTurnIndex++;
        SerializableMessage message = SerializableMessage.fromChatMessage(chatMessage, currentTurnIndex);
        messages.add(message);

        // LRU 淘汰：如果超过最大数量，删除最早的消息
        while (messages.size() > maxMessageCount) {
            messages.remove(0);
        }

        lastAccessTime = LocalDateTime.now();
    }

    /**
     * 获取所有 ChatMessage
     */
    @JsonIgnore
    public List<ChatMessage> getChatMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> chatMessages = new ArrayList<>();
        for (SerializableMessage message : messages) {
            chatMessages.add(message.toChatMessage());
        }
        return chatMessages;
    }

    /**
     * 从 ChatMessage 列表创建包装类
     */
    public static ChatMessageWrapper fromChatMessages(String memoryId, List<ChatMessage> chatMessages, int maxCount) {
        ChatMessageWrapper wrapper = ChatMessageWrapper.builder()
                .memoryId(memoryId)
                .maxMessageCount(maxCount)
                .lastAccessTime(LocalDateTime.now())
                .build();

        if (chatMessages != null) {
            wrapper.messages = new ArrayList<>();

            // 处理消息，保持原有的turn_index逻辑
            // SystemMessage应该在turn_index=0，其他消息按顺序排列
            int maxTurnIndex = 0;

            for (ChatMessage chatMessage : chatMessages) {
                int turnIndex;
                if (chatMessage instanceof SystemMessage) {
                    turnIndex = 0; // SystemMessage固定在turn_index=0
                } else {
                    maxTurnIndex++;
                    turnIndex = maxTurnIndex;
                }

                SerializableMessage message = SerializableMessage.fromChatMessage(chatMessage, turnIndex);
                wrapper.messages.add(message);
            }

            // 设置当前的turn_index为最大的非SystemMessage的turn_index
            wrapper.currentTurnIndex = maxTurnIndex;
        }

        return wrapper;
    }

    /**
     * 从 ChatMessage 列表创建包装类，支持指定起始turn_index
     */
    public static ChatMessageWrapper fromChatMessages(String memoryId, List<ChatMessage> chatMessages, int maxCount, int startTurnIndex) {
        ChatMessageWrapper wrapper = ChatMessageWrapper.builder()
                .memoryId(memoryId)
                .maxMessageCount(maxCount)
                .lastAccessTime(LocalDateTime.now())
                .build();

        if (chatMessages != null) {
            wrapper.messages = new ArrayList<>();
            int turnIndex = startTurnIndex;
            for (ChatMessage chatMessage : chatMessages) {
                SerializableMessage message = SerializableMessage.fromChatMessage(chatMessage, turnIndex++);
                wrapper.messages.add(message);
            }
            // 设置当前的turn_index为最后一个消息的turn_index
            wrapper.currentTurnIndex = turnIndex - 1;
        }

        return wrapper;
    }

    /**
     * 更新访问时间
     */
    public void updateAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
    }

    /**
     * 获取消息数量
     */
    @JsonIgnore
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }

    /**
     * 清空消息
     */
    public void clearMessages() {
        if (messages != null) {
            messages.clear();
        }
        currentTurnIndex = 0;
        lastAccessTime = LocalDateTime.now();
    }
}
