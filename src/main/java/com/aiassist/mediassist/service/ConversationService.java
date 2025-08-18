package com.aiassist.mediassist.service;

import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.store.MongoChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 会话管理服务
 * <p>
 * 提供会话的创建、查询、管理等功能
 */
@Slf4j
@Service
public class ConversationService {

    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 创建新会话
     */
    public String createNewConversation(String userIp) {
        // 生成唯一的 memoryId (使用时间戳 + UUID)
        String memoryId = generateMemoryId();
        try {
            // 创建会话
            LocalDateTime now = LocalDateTime.now();
            Conversation conversation = Conversation.builder()
                    // todo 统一 主键id 生成逻辑
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .userIp(userIp)
                    .createdTime(now)
                    .lastSendTime(now)
                    .build();
            mongoTemplate.save(conversation);

            log.info("创建新会话成功: memoryId={}, userIp={}", memoryId, userIp);
        } catch (Exception e) {
            log.error("创建会话失败: memoryId={}", memoryId, e);
            throw new RuntimeException("创建会话失败", e);
        }
        return memoryId;
    }

    /**
     * 获取用户的会话列表
     */
    public List<Conversation> getUserConversations(String userIp) {
        // 构建查询条件
        Query query = Query.query(Criteria.where("user_ip").is(userIp))
                .with(Sort.by(Sort.Direction.DESC, "created_time")); // 按创建时间倒序
        // 执行查询
        return mongoTemplate.find(query, Conversation.class);
    }

    /**
     * 获取会话详情
     */
    public Conversation getConversation(String memoryId) {
        Query query = Query.query(Criteria.where("memory_id").is(memoryId));
        return mongoTemplate.findOne(query, Conversation.class);
    }

    /**
     * 删除会话
     */
    public void deleteConversation(String memoryId) {
        mongoChatMemoryStore.deleteMessages(memoryId);
        log.info("删除会话: memoryId={}", memoryId);
        // todo messages 级联删除
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成唯一的 memoryId
     * 格式: 时间戳_UUID前8位
     */
    private String generateMemoryId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid;
    }

    /**
     * 检查会话是否存在且有效
     */
    public boolean isValidConversation(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            return false;
        }
        Query query = Query.query(Criteria.where("memory_id").is(memoryId));
        return mongoTemplate.exists(query, Conversation.class);
    }

    /**
     * 处理和验证 memoryId
     * 如果客户端提供的 memoryId 无效或为空，则自动创建新会话
     */
    public String processMemoryId(String clientMemoryId, String userIp) {
        // 如果客户端没有提供 memoryId 或者无效，创建新会话
        if (clientMemoryId == null || clientMemoryId.trim().isEmpty() ||
                !isValidConversation(clientMemoryId)) {

            String newMemoryId = createNewConversation(userIp);
            log.info("客户端memoryId无效 '{}', 自动创建新会话: {}", clientMemoryId, newMemoryId);
            return newMemoryId;
        }

        return clientMemoryId.trim();
    }
}

//            long totalConversations = mongoTemplate.count(new Query(), Conversation.class);
//            long todayConversations = mongoTemplate.count(
//                    Query.query(Criteria.where("created_time").gte(LocalDateTime.now().toLocalDate().atStartOfDay())),
//                    Conversation.class);