package com.aiassist.mediassist.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 会话元信息（混合方案的 conversations 集合）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    /**
     * 对话ID（ULID/UUIDv7/时间戳+随机数），全局唯一
     */
    @Field("memory_id")
    @Indexed(unique = true)
    private String memoryId;

    /**
     * 发起会话的用户 IP（如需隔离/审计）
     */
    @Field("user_ip")
    @Indexed
    private String userIp;

    /**
     * 会话创建时间（BSON Date）。可与 memoryId 绑定，冗余以便排序/筛选
     */
    @Field("created_time")
    @Indexed
    private LocalDateTime createdTime;

    /**
     * 最近一条消息的发送时间（用于列表排序）
     */
    @Field("last_send_time")
    @Indexed
    private LocalDateTime lastSendTime;
}


