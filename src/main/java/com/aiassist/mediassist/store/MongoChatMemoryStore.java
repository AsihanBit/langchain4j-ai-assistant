package com.aiassist.mediassist.store;

import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.dto.entity.Message;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB 实现的 LangChain4j ChatMemoryStore
 * 
 * 负责将 LangChain4j 的消息格式与 MongoDB 的混合存储方案进行转换
 */
@Slf4j
@Component
public class MongoChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 获取指定 memoryId 的所有消息
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();
        log.info("🔍 [MEMORY] 获取聊天记忆: memoryId={}", memoryIdStr);
        
        // 添加调用栈信息来追踪谁在调用这个方法
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("🔍 [MEMORY] 调用栈: {}", stackTrace.length > 3 ? stackTrace[3].toString() : "unknown");

        try {
            // 按 turn_index 排序查询消息
            Query query = Query.query(Criteria.where("memory_id").is(memoryIdStr))
                    .with(Sort.by(Sort.Direction.ASC, "turn_index"));
            List<Message> messages = mongoTemplate.find(query, Message.class);

            // 转换为 LangChain4j 消息格式
            List<ChatMessage> chatMessages = new ArrayList<>();
            for (Message message : messages) {
                // 每个 Message 包含 prompt 和 completion，需要分别转换
                if (message.getContent() != null) {
                    if (message.getContent().getPrompt() != null && !message.getContent().getPrompt().trim().isEmpty()) {
                        chatMessages.add(UserMessage.from(message.getContent().getPrompt()));
                        log.info("🔍 [CONVERT] 转换用户消息: 长度={}", message.getContent().getPrompt().length());
                    }
                    if (message.getContent().getCompletion() != null && !message.getContent().getCompletion().trim().isEmpty()) {
                        // 简单地转换为AI消息，不要重构为工具消息
                        String completion = message.getContent().getCompletion();
                        log.info("🔍 [CONVERT] 转换AI回复: 长度={}", completion.length());
                        chatMessages.add(AiMessage.from(completion));
                    }
                }
            }

            log.info("🔍 [MEMORY] 从MongoDB加载了 {} 条消息", chatMessages.size());
            if (!chatMessages.isEmpty()) {
                log.info("🔍 [MEMORY] 最后一条消息: {}", chatMessages.get(chatMessages.size() - 1).toString().substring(0, Math.min(100, chatMessages.get(chatMessages.size() - 1).toString().length())));
            }
            return chatMessages;

        } catch (Exception e) {
            log.error("获取聊天记忆失败: memoryId={}", memoryIdStr, e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新指定 memoryId 的消息列表（替换所有消息）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String memoryIdStr = memoryId.toString();
        log.info("💾 [MEMORY] 更新聊天记忆: memoryId={}, 消息数量={}", memoryIdStr, messages.size());
        
        // 添加调用栈信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("💾 [MEMORY] 调用栈: {}", stackTrace.length > 3 ? stackTrace[3].toString() : "unknown");
        
        if (!messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            log.info("💾 [MEMORY] 最后一条消息类型: {}, 内容: {}", 
                lastMessage.type(), 
                lastMessage.toString().substring(0, Math.min(150, lastMessage.toString().length())));
        }

        try {
            log.info("🗑️ [CLEANUP] 开始删除旧消息: memoryId={}", memoryIdStr);
            // 删除旧消息（但不删除会话记录）
            Query messageQuery = Query.query(Criteria.where("memory_id").is(memoryIdStr));
            var deleteResult = mongoTemplate.remove(messageQuery, Message.class);
            log.info("🗑️ [CLEANUP] 删除旧消息完成: memoryId={}, 删除数量={}", memoryIdStr, deleteResult.getDeletedCount());

            // 确保会话存在（仅创建会话记录，不触发其他业务逻辑）
            ensureConversationExists(memoryIdStr);

            // 将 LangChain4j 消息转换为我们的格式并保存
            saveLangChainMessages(memoryIdStr, messages);

            // 更新会话统计信息
            updateConversationStats(memoryIdStr);

            log.info("✅ [UPDATE] 更新聊天记忆成功: memoryId={}", memoryIdStr);

        } catch (Exception e) {
            log.error("❌ [UPDATE] 更新聊天记忆失败: memoryId={}", memoryIdStr, e);
            // 不重新抛出异常，避免影响LangChain4j的主流程
        }
    }

    /**
     * 删除指定 memoryId 的所有消息
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();
        log.debug("删除聊天记忆: memoryId={}", memoryIdStr);

        try {
            // 删除消息
            Query messageQuery = Query.query(Criteria.where("memory_id").is(memoryIdStr));
            mongoTemplate.remove(messageQuery, Message.class);

            // 删除会话
            Query conversationQuery = Query.query(Criteria.where("memory_id").is(memoryIdStr));
            mongoTemplate.remove(conversationQuery, Conversation.class);

            log.debug("删除聊天记忆成功: memoryId={}", memoryIdStr);

        } catch (Exception e) {
            log.error("删除聊天记忆失败: memoryId={}", memoryIdStr, e);
        }
    }

    /**
     * 添加新消息（用于流式对话）
     */
    public void addMessage(String memoryId, String userMessage, String aiResponse) {
        try {
            // 确保会话存在
            ensureConversationExists(memoryId);

            // 获取下一个 turn_index
            Integer nextTurnIndex = getNextTurnIndex(memoryId);

            // 创建消息
            Message message = Message.builder()
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .turnIndex(nextTurnIndex)
                    .content(new Message.Content(userMessage, aiResponse))
                    .sendTime(LocalDateTime.now())
                    .build();

            // 保存消息
            mongoTemplate.save(message);

            // 更新会话信息
            updateConversationStats(memoryId);

            log.debug("添加消息成功: memoryId={}, turnIndex={}", memoryId, nextTurnIndex);

        } catch (Exception e) {
            log.error("添加消息失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 创建新会话
     */
    public Conversation createConversation(String memoryId, String userIp) {
        try {
            Conversation conversation = Conversation.builder()
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .userIp(userIp)
                    .createdTime(LocalDateTime.now())
                    .lastSendTime(LocalDateTime.now())
                    .build();

            mongoTemplate.save(conversation);
            log.info("创建会话成功: memoryId={}", memoryId);
            return conversation;

        } catch (Exception e) {
            log.error("创建会话失败: memoryId={}", memoryId, e);
            throw new RuntimeException("创建会话失败", e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 确保会话存在，如果不存在则创建
     */
    private void ensureConversationExists(String memoryId) {
        log.info("🔍 [CONVERSATION] 开始检查会话是否存在: memoryId={}", memoryId);
        
        Query query = Query.query(Criteria.where("memory_id").is(memoryId));
        Conversation conversation = mongoTemplate.findOne(query, Conversation.class);

        if (conversation == null) {
            log.info("📝 [CONVERSATION] 会话不存在，开始创建: memoryId={}", memoryId);
            try {
                Conversation newConversation = Conversation.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .memoryId(memoryId)
                        .userIp("auto-created") // 标记为自动创建
                        .createdTime(java.time.LocalDateTime.now())
                        .lastSendTime(java.time.LocalDateTime.now())
                        .build();

                mongoTemplate.save(newConversation);
                log.info("✅ [CONVERSATION] 自动创建会话成功: memoryId={}", memoryId);
            } catch (Exception e) {
                log.error("❌ [CONVERSATION] 自动创建会话失败: memoryId={}", memoryId, e);
                // 不抛出异常，避免影响主流程
            }
        } else {
            log.info("✅ [CONVERSATION] 会话已存在: memoryId={}, conversationId={}", memoryId, conversation.getId());
        }
    }

    /**
     * 获取下一个 turn_index
     */
    private Integer getNextTurnIndex(String memoryId) {
        Query query = Query.query(Criteria.where("memory_id").is(memoryId))
                .with(Sort.by(Sort.Direction.DESC, "turn_index"))
                .limit(1);

        Message lastMessage = mongoTemplate.findOne(query, Message.class);
        return lastMessage != null ? lastMessage.getTurnIndex() + 1 : 1;
    }

    /**
     * 更新会话统计信息
     */
    private void updateConversationStats(String memoryId) {
        log.info("📊 [STATS] 开始更新会话统计: memoryId={}", memoryId);
        
        try {
            Query conversationQuery = Query.query(Criteria.where("memory_id").is(memoryId));
            Update update = new Update()
                    .set("last_send_time", LocalDateTime.now());

            var result = mongoTemplate.updateFirst(conversationQuery, update, Conversation.class);
            log.info("📊 [STATS] 更新会话统计完成: memoryId={}, 修改数量={}", memoryId, result.getModifiedCount());
        } catch (Exception e) {
            log.error("❌ [STATS] 更新会话统计失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 将 LangChain4j 消息列表转换并保存
     */
    private void saveLangChainMessages(String memoryId, List<ChatMessage> messages) {
        log.info("💬 [MESSAGES] 开始保存LangChain4j消息: memoryId={}, 消息总数={}", memoryId, messages.size());
        
        if (messages.isEmpty()) {
            log.info("💬 [MESSAGES] 消息列表为空，跳过保存: memoryId={}", memoryId);
            return;
        }

        List<Message> mongoMessages = new ArrayList<>();
        Integer turnIndex = 1;

        // 将消息按对话回合分组 - 采用更简单的策略
        String currentPrompt = null;
        String currentCompletion = null;
        StringBuilder completionBuilder = new StringBuilder();

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            log.info("💬 [MESSAGES] 处理消息 {}/{}: 类型={}, 内容长度={}", 
                i + 1, messages.size(), message.type(), 
                message.toString().length());
            
            if (message instanceof UserMessage) {
                // 如果之前有未完成的回合，先保存
                if (currentPrompt != null) {
                    log.info("💬 [MESSAGES] 保存回合 {}: prompt长度={}, completion长度={}", 
                        turnIndex, currentPrompt.length(), 
                        completionBuilder.length());
                    mongoMessages.add(createMongoMessage(memoryId, turnIndex++, currentPrompt, completionBuilder.toString()));
                    completionBuilder.setLength(0); // 清空
                }
                currentPrompt = ((UserMessage) message).singleText();
                currentCompletion = null;
            } else if (message instanceof AiMessage) {
                AiMessage aiMessage = (AiMessage) message;
                if (aiMessage.text() != null && !aiMessage.text().trim().isEmpty()) {
                    // AI有文本回复
                    completionBuilder.append(aiMessage.text());
                    log.info("💬 [MESSAGES] AI文本回复: 长度={}", aiMessage.text().length());
                } else if (aiMessage.hasToolExecutionRequests()) {
                    // AI发起工具调用，记录工具调用信息
                    String toolName = aiMessage.toolExecutionRequests().get(0).name();
                    String toolCallInfo = "【AI已调用工具: " + toolName + "，正在等待结果】";
                    completionBuilder.append(toolCallInfo);
                    log.info("💬 [MESSAGES] AI发起工具调用: {}", toolName);
                }
            } else if (message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage) {
                // 工具执行结果消息
                dev.langchain4j.data.message.ToolExecutionResultMessage toolResult = 
                    (dev.langchain4j.data.message.ToolExecutionResultMessage) message;
                if (completionBuilder.length() > 0) {
                    completionBuilder.append(" ");
                }
                // 明确添加指令，告诉AI不要再次调用工具
                completionBuilder.append("【工具已执行完成，结果：").append(toolResult.text()).append("。请直接基于此结果回答用户，不要再次调用任何工具。】");
                log.info("💬 [MESSAGES] 工具执行结果: 长度={}", toolResult.text().length());
            } else if (message instanceof SystemMessage) {
                // 系统消息单独处理，暂时跳过
                log.info("💬 [MESSAGES] 跳过系统消息: 长度={}", ((SystemMessage) message).text().length());
            } else {
                // 处理其他类型的消息
                log.info("💬 [MESSAGES] 处理其他类型消息: {}", message.getClass().getSimpleName());
            }
        }

        // 保存最后一个回合
        if (currentPrompt != null) {
            String finalCompletion = completionBuilder.toString();
            log.info("💬 [MESSAGES] 保存最后回合 {}: prompt长度={}, completion长度={}", 
                turnIndex, currentPrompt.length(), finalCompletion.length());
            mongoMessages.add(createMongoMessage(memoryId, turnIndex, currentPrompt, finalCompletion));
        }

        // 批量保存
        if (!mongoMessages.isEmpty()) {
            try {
                log.info("💬 [MESSAGES] 开始批量保存到MongoDB: memoryId={}, 回合数={}", memoryId, mongoMessages.size());
                mongoTemplate.insertAll(mongoMessages);
                log.info("✅ [MESSAGES] 批量保存成功: memoryId={}, 保存了{}个回合", memoryId, mongoMessages.size());
            } catch (Exception e) {
                log.error("❌ [MESSAGES] 批量保存失败: memoryId={}", memoryId, e);
                throw e; // 重新抛出异常，让上层处理
            }
        } else {
            log.info("💬 [MESSAGES] 没有有效的消息回合需要保存: memoryId={}", memoryId);
        }
    }

    /**
     * 创建 MongoDB 消息文档
     */
    private Message createMongoMessage(String memoryId, Integer turnIndex, String prompt, String completion) {
        return Message.builder()
                .id(UUID.randomUUID().toString())
                .memoryId(memoryId)
                .turnIndex(turnIndex)
                .content(new Message.Content(prompt, completion))
                .sendTime(LocalDateTime.now())
                .model(new Message.ModelInfo("gpt-4o-mini", null, null))
                .build();
    }
}