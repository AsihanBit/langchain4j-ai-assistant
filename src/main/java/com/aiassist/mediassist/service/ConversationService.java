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
 * 
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
        
        // 创建会话
        Conversation conversation = mongoChatMemoryStore.createConversation(memoryId, userIp);
        
        log.info("创建新会话成功: memoryId={}, userIp={}", memoryId, userIp);
        return memoryId;
    }

    /**
     * 获取用户的会话列表
     */
    public List<Conversation> getUserConversations(String userIp) {
        Query query = Query.query(Criteria.where("user_ip").is(userIp))
                .with(Sort.by(Sort.Direction.DESC, "last_send_time"));
        
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
     * 删除会话
     */
    public void deleteConversation(String memoryId) {
        mongoChatMemoryStore.deleteMessages(memoryId);
        log.info("删除会话: memoryId={}", memoryId);
    }

    /**
     * 获取会话统计信息
     */
    public ConversationStats getStats() {
        try {
            long totalConversations = mongoTemplate.count(new Query(), Conversation.class);
            long todayConversations = mongoTemplate.count(
                    Query.query(Criteria.where("created_time").gte(LocalDateTime.now().toLocalDate().atStartOfDay())), 
                    Conversation.class);
            
            return ConversationStats.builder()
                    .totalConversations(totalConversations)
                    .todayConversations(todayConversations)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取会话统计失败", e);
            return ConversationStats.builder().build();
        }
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
     * 会话统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationStats {
        private long totalConversations;
        private long todayConversations;
    }
}