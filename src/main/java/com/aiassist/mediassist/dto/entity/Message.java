package com.aiassist.mediassist.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息明细（混合方案的 messages 集合），一条文档代表一个“回合”（prompt+completion）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "uidx_memory_turn", def = "{ 'memory_id': 1, 'turn_index': 1 }", unique = true),
        @CompoundIndex(name = "idx_memory_send", def = "{ 'memory_id': 1, 'send_time': 1 }")
})
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
}


