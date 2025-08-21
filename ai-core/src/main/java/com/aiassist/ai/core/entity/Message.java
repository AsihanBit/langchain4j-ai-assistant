package com.aiassist.ai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天消息明细（混合方案的 messages 集合），一条文档代表一个"回合"（prompt+completion）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
//@CompoundIndexes({})
public class Message {

    @Id
    private String id;

    /**
     * 对话ID（ULID/UUIDv7/时间戳+随机数）
     */
    @Field("memory_id")
    @Indexed
    private String memoryId;

    /**
     * 对话内有序序号（递增），用于严格排序与去重
     */
    @Field("turn_index")
    private Integer turnIndex;

    /**
     * 消息类型：用于前端区分显示
     * USER - 用户消息
     * AI - AI回复消息
     * TOOL_CALL - AI工具调用消息
     * TOOL_RESULT - 工具执行结果消息
     * SYSTEM - 系统消息
     */
    @Field("message_type")
    private MessageType messageType;

    /**
     * 内容：包含 prompt 与 completion
     */
    @Field("content")
    private Content content;

    /**
     * 发送时间（BSON Date）
     */
    @Field("send_time")
    @Indexed
    private LocalDateTime sendTime;

    /**
     * 模型信息
     */
    @Field("model")
    private ModelInfo model;

    /**
     * 额外元数据（ip 等），可选
     */
    @Field("metadata")
    private Map<String, Object> metadata;

    /**
     * 工具调用记录数组，存储本回合的工具调用信息
     */
    @Field("tool_calls")
    private List<ToolCall> toolCalls;

    // ----------------- 嵌套类型 -----------------

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        USER,           // 用户消息
        AI,             // AI回复消息
        TOOL_CALL,      // AI工具调用消息
        TOOL_RESULT,    // 工具执行结果消息
        SYSTEM          // 系统消息
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String prompt;
        private String completion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        private String name;
        @Field("tokens_input")
        private Integer tokensInput;
        @Field("tokens_output")
        private Integer tokensOutput;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        /**
         * 工具名称
         */
        @Field("tool_name")
        private String toolName;

        /**
         * 工具执行结果
         */
        @Field("result")
        private String result;

        /**
         * 执行时间戳
         */
        @Field("timestamp")
        private LocalDateTime timestamp;
    }

    /**
     * 从 ChatMessage 创建 Message 实体
     */
    public static Message fromChatMessage(String memoryId, dev.langchain4j.data.message.ChatMessage chatMessage, int turnIndex) {
        Message.MessageType messageType = MessageType.USER;
        String prompt = "";
        String completion = "";
        List<ToolCall> toolCalls = null;

        if (chatMessage instanceof dev.langchain4j.data.message.UserMessage) {
            messageType = MessageType.USER;
            prompt = ((dev.langchain4j.data.message.UserMessage) chatMessage).singleText();
        } else if (chatMessage instanceof dev.langchain4j.data.message.AiMessage) {
            dev.langchain4j.data.message.AiMessage aiMessage = (dev.langchain4j.data.message.AiMessage) chatMessage;
            if (aiMessage.text() != null && !aiMessage.text().trim().isEmpty()) {
                messageType = MessageType.AI;
                completion = aiMessage.text();
            } else if (aiMessage.hasToolExecutionRequests()) {
                messageType = MessageType.TOOL_CALL;
                String toolName = aiMessage.toolExecutionRequests().get(0).name();
                String toolArgs = aiMessage.toolExecutionRequests().get(0).arguments();
                completion = String.format("调用工具: %s(%s)", toolName,
                        toolArgs.length() > 100 ? toolArgs.substring(0, 100) + "..." : toolArgs);
            }
        } else if (chatMessage instanceof dev.langchain4j.data.message.ToolExecutionResultMessage) {
            messageType = MessageType.TOOL_RESULT;
            dev.langchain4j.data.message.ToolExecutionResultMessage toolResult =
                    (dev.langchain4j.data.message.ToolExecutionResultMessage) chatMessage;
            completion = toolResult.text();

            // 创建工具调用记录
            ToolCall toolCall = new ToolCall();
            toolCall.setToolName(toolResult.toolName());
            toolCall.setResult(toolResult.text());
            toolCall.setTimestamp(LocalDateTime.now());
            toolCalls = List.of(toolCall);
        } else if (chatMessage instanceof dev.langchain4j.data.message.SystemMessage) {
            messageType = MessageType.SYSTEM;
            completion = ((dev.langchain4j.data.message.SystemMessage) chatMessage).text();
        }

        return Message.builder()
                .id(UUID.randomUUID().toString())
                .memoryId(memoryId)
                .turnIndex(turnIndex)
                .messageType(messageType)
                .content(new Content(prompt, completion))
                .sendTime(LocalDateTime.now())
                .model(new ModelInfo("gpt-4o-mini", null, null)) // TODO 动态获取
                .toolCalls(toolCalls)
                .build();
    }
}


