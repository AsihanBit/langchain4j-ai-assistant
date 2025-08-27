package com.aiassist.ai.core.service;

import com.aiassist.ai.core.entity.Conversation;
import com.aiassist.ai.core.store.MongoChatMemoryStore;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    public Conversation createNewConversation(String userIp) {
        // 生成唯一的 memoryId (使用时间戳 + UUID)
        String memoryId = generateMemoryId();
        try {
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            String newTitle = generateDefaultTitle(now);

            // 创建会话
            Conversation conversation = Conversation.builder()
                    // TODO 统一 主键id 生成逻辑
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .title(newTitle)
                    .userIp(userIp)
                    .createdTime(now)
                    .lastSendTime(now)
                    .build();
            mongoTemplate.save(conversation);

            log.info("创建新会话成功: memoryId={}, userIp={}", memoryId, userIp);

            return conversation;
        } catch (Exception e) {
            log.error("创建会话失败: memoryId={}", memoryId, e);
            throw new RuntimeException("创建会话失败", e);
        }
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
     * 更新会话文档的 title 字段
     *
     * @param memoryId 会话的 memoryId，用于定位文档
     * @param newTitle 新的 title 的值
     * @return 更新成功返回 true，否则返回 false
     */
    public boolean updateConversationTitle(String memoryId, String newTitle) {
        // 构建查询条件：通过 memory_id 匹配目标文档
        Query query = Query.query(Criteria.where("memory_id").is(memoryId));

        // 构建更新操作：设置 title 的新值
        Update update = new Update();
        update.set("title", newTitle);

        // 执行更新操作
        UpdateResult result = mongoTemplate.updateFirst(query, update, Conversation.class);

        // 返回是否成功更新（受影响的文档数量 > 0）
        return result.getModifiedCount() > 0;
    }

    // TODO 更新会话最后发送时间

    /**
     * 删除会话
     */
    public void deleteConversation(String memoryId) {
        mongoChatMemoryStore.deleteMessages(memoryId);
        log.info("删除会话: memoryId={}", memoryId);
        // TODO messages 级联删除
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成默认标题
     */
    private String generateDefaultTitle(LocalDateTime time) {
        // 格式化时间为 "MMdd_HH:mm"，如 "0827_16:30"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd_HH:mm");
        String formattedTime = time.format(formatter);
        // 根据格式化时间生成标题 "新对话_0827_16:30"
        String newTitle = "新对话_" + formattedTime;
        return newTitle;
    }

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

            Conversation conversation = createNewConversation(userIp);
            String newMemoryId = conversation.getMemoryId();
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