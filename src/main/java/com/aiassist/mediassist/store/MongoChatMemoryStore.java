package com.aiassist.mediassist.store;

import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.dto.entity.Message;
import com.aiassist.mediassist.service.ChatMessageCacheService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aiassist.mediassist.dto.entity.ChatMessageWrapper;

/**
 * MongoDB 实现的 LangChain4j ChatMemoryStore
 * 负责将 LangChain4j 的消息格式与 MongoDB 的混合存储方案进行转换
 */
@Slf4j
@Component
public class MongoChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChatMessageCacheService cacheService;

    /**
     * 最大记忆消息窗口大小，消息淘汰的基准（不包含SystemMessage）
     */
    @Value("${chat.memory.max-size:6}")
    private int memoryMaxSize;

    /**
     * Redis缓存中的消息数量（包含1个SystemMessage + memoryMaxSize条实际消息）
     */
    @Value("${chat.cache.max-size:7}")
    private int cacheMaxSize;

    /**
     * 获取消息内容用于日志显示
     */
    private String getMessageContentForLog(ChatMessage msg) {
        try {
            if (msg instanceof UserMessage) {
                String text = ((UserMessage) msg).singleText();
                return text != null ? text.substring(0, Math.min(30, text.length())) + "..." : "null";
            } else if (msg instanceof AiMessage) {
                String text = ((AiMessage) msg).text();
                return text != null ? text.substring(0, Math.min(30, text.length())) + "..." : "null";
            } else if (msg instanceof ToolExecutionResultMessage) {
                String text = ((ToolExecutionResultMessage) msg).text();
                return text != null ? text.substring(0, Math.min(30, text.length())) + "..." : "null";
            } else if (msg instanceof SystemMessage) {
                String text = ((SystemMessage) msg).text();
                return text != null ? text.substring(0, Math.min(30, text.length())) + "..." : "null";
            } else {
                return msg.toString().substring(0, Math.min(30, msg.toString().length())) + "...";
            }
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /**
     * 获取指定 memoryId 的所有消息
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();
        log.info("=== getMessages调用 ===");
        log.info("🔍 [MEMORY] 获取聊天记忆: memoryId={}, 窗口大小={}", memoryIdStr, memoryMaxSize);

        // 添加调用栈信息来追踪谁在调用这个方法
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("🔍 [MEMORY] 调用栈: {}", stackTrace.length > 3 ? stackTrace[3].toString() : "unknown");

        try {
            // 1. 首先从Redis缓存获取
            List<ChatMessage> cachedMessages = cacheService.getMessages(memoryIdStr);
            log.info("🔍 [CACHE] Redis缓存消息数量: {}", cachedMessages != null ? cachedMessages.size() : 0);

            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                // 如果有缓存，直接返回缓存结果
                logMessageList("getMessages返回(缓存)", cachedMessages);
                return cachedMessages;
            }

            // 2. 缓存未命中，从MongoDB获取最近的消息
            List<ChatMessage> dbMessages = getRecentMessagesFromDB(memoryIdStr);
            log.info("🔍 [MONGODB] MongoDB最近消息数量: {}", dbMessages.size());

            // 3. 确保SystemMessage在turn_index=0，并构建完整消息列表
            List<ChatMessage> allMessages = buildCompleteMessageList(dbMessages);

            // 4. 更新Redis缓存
            if (!allMessages.isEmpty()) {
                cacheService.updateMessages(memoryIdStr, allMessages);
                log.info("💾 [CACHE] 已将消息缓存到Redis: memoryId={}", memoryIdStr);
            }

            log.info("🔍 [FINAL] 最终返回消息数量: {}", allMessages.size());
            logMessageList("getMessages返回(数据库)", allMessages);

            return allMessages;

        } catch (Exception e) {
            log.error("❌ [MONGODB] 获取聊天记忆失败: memoryId={}", memoryIdStr, e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新指定 memoryId 的消息列表（替换所有消息）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String memoryIdStr = memoryId.toString();
        log.info("=== updateMessages调用 ===");
        log.info("💾 [MEMORY] 更新聊天记忆: memoryId={}, 消息数量={}, 窗口大小={}", memoryIdStr, messages.size(), memoryMaxSize);

        // 添加调用栈信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("💾 [MEMORY] 调用栈: {}", stackTrace.length > 3 ? stackTrace[3].toString() : "unknown");

        logMessageList("updateMessages接收", messages);

        try {
            // 1. 过滤SystemMessage，只保留turn_index=0的SystemMessage
            SystemMessage systemMessage = extractSystemMessage(messages);
            List<ChatMessage> nonSystemMessages = filterNonSystemMessages(messages);

            log.info("💾 [MEMORY] 过滤后非SystemMessage数量: {}", nonSystemMessages.size());

            // 2. 识别并保存真正的新消息
            saveOnlyNewMessages(memoryIdStr, nonSystemMessages);

            // 4. 应用窗口淘汰策略，更新Redis缓存
            updateRedisCacheWithWindowEviction(memoryIdStr, systemMessage);

            log.info("✅ [UPDATE] updateMessages完成: memoryId={}", memoryIdStr);

        } catch (Exception e) {
            log.error("❌ [UPDATE] 更新聊天记忆失败: memoryId={}", memoryIdStr, e);
            // 不重新抛出异常，避免影响LangChain4j的主流程
        }
    }

    // ==================== 新的辅助方法 ====================

    /**
     * 从MongoDB获取最近的消息（基于窗口大小）
     */
    private List<ChatMessage> getRecentMessagesFromDB(String memoryId) {
        try {
            // 获取最近的消息，数量为窗口大小
            Query query = new Query(Criteria.where("memoryId").is(memoryId))
                    .with(Sort.by(Sort.Direction.DESC, "turnIndex"))
                    .limit(memoryMaxSize);

            List<Message> messages = mongoTemplate.find(query, Message.class);
            log.info("🔍 [MONGODB] 查询到最近 {} 条消息记录", messages.size());

            // 转换为ChatMessage并按turn_index正序排列
            List<ChatMessage> chatMessages = new ArrayList<>();
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                ChatMessage chatMessage = convertIndividualMessageToChatMessage(message);
                if (chatMessage != null) {
                    chatMessages.add(chatMessage);
                }
            }

            return chatMessages;
        } catch (Exception e) {
            log.error("❌ [MONGODB] 从MongoDB获取最近消息失败: memoryId={}", memoryId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从MongoDB获取最近的消息（返回Message对象，保持turnIndex）
     */
    private List<Message> getRecentMessagesFromDB(String memoryId, int limit) {
        try {
            // 获取最近的消息
            Query query = new Query(Criteria.where("memoryId").is(memoryId))
                    .with(Sort.by(Sort.Direction.DESC, "turnIndex"))
                    .limit(limit);

            List<Message> messages = mongoTemplate.find(query, Message.class);
            log.info("🔍 [MONGODB] 查询到最近 {} 条消息记录", messages.size());

            // 按turnIndex正序排列
            messages.sort(Comparator.comparingInt(Message::getTurnIndex));

            return messages;

        } catch (Exception e) {
            log.error("❌ [MONGODB] 获取最近消息失败: memoryId={}", memoryId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建完整的消息列表（SystemMessage + 数据库消息）
     */
    private List<ChatMessage> buildCompleteMessageList(List<ChatMessage> dbMessages) {
        List<ChatMessage> result = new ArrayList<>();

        // 添加SystemMessage到turn_index=0
        result.add(createSystemMessage());

        // 添加数据库消息
        result.addAll(dbMessages);

        log.info("🔍 [BUILD] 构建完整消息列表: SystemMessage(1) + 数据库消息({}) = 总数({})",
            dbMessages.size(), result.size());
        return result;
    }

    /**
     * 创建默认的SystemMessage
     */
    private SystemMessage createSystemMessage() {
        // todo 获取设置的 prompt
        return SystemMessage.from("你是一个专业的医疗助手，能够帮助用户解答医疗相关问题。");
    }

    /**
     * 提取SystemMessage（只保留turn_index=0的）
     */
    private SystemMessage extractSystemMessage(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                log.info("  [SYSTEM] 发现SystemMessage，将保留在turn_index=0");
                return (SystemMessage) message;
            }
        }
        // 如果没有找到，返回默认的SystemMessage
        return createSystemMessage();
    }

    /**
     * 过滤非SystemMessage
     */
    private List<ChatMessage> filterNonSystemMessages(List<ChatMessage> messages) {
        List<ChatMessage> filtered = messages.stream()
            .filter(msg -> !(msg instanceof SystemMessage))
            .collect(java.util.stream.Collectors.toList());

        log.info("💾 [FILTER] 过滤SystemMessage: 原始{}条 -> 过滤后{}条", messages.size(), filtered.size());
        return filtered;
    }

    /**
     * 获取当前最大turn_index
     */
    private int getCurrentMaxTurnIndex(String memoryId) {
        try {
            Query query = new Query(Criteria.where("memoryId").is(memoryId))
                .with(Sort.by(Sort.Direction.DESC, "turnIndex"))
                .limit(1);

            Message lastMessage = mongoTemplate.findOne(query, Message.class);
            int maxTurnIndex = lastMessage != null ? lastMessage.getTurnIndex() : 0;
            log.info("🔍 [TURN_INDEX] 当前最大turn_index: {}", maxTurnIndex);
            return maxTurnIndex;
        } catch (Exception e) {
            log.error("❌ [TURN_INDEX] 获取最大turn_index失败: memoryId={}", memoryId, e);
            return 0;
        }
    }



    /**
     * 识别并保存真正的新消息
     */
    private void saveOnlyNewMessages(String memoryId, List<ChatMessage> currentMessages) {
        log.info("=== 识别并保存新消息 ===");

        if (currentMessages.isEmpty()) {
            log.info("💾 [SAVE] 没有消息需要处理");
            return;
        }

        // 获取当前MongoDB中的最大turn_index
        // todo 这里查询mongodb可以优化成从缓存获取
        int currentMaxTurnIndex = getCurrentMaxTurnIndex(memoryId);
        log.info("💾 [SAVE] 当前MongoDB最大turn_index: {}", currentMaxTurnIndex);

        // 获取Redis缓存中的消息，用于比较
        List<ChatMessage> allCachedMessages = cacheService.getMessages(memoryId);
        List<ChatMessage> cachedMessages = new ArrayList<>();

        if (allCachedMessages != null && !allCachedMessages.isEmpty()) {
            // 过滤掉SystemMessage，只保留实际对话消息
            cachedMessages = allCachedMessages.stream()
                .filter(msg -> !(msg instanceof SystemMessage))
                .collect(Collectors.toList());
        }

        log.info("💾 [SAVE] 当前传入消息数: {}, 缓存中消息数: {}", currentMessages.size(), cachedMessages.size());

        // 识别新消息：传入的消息数量大于缓存中的消息数量
        int newMessageCount = currentMessages.size() - cachedMessages.size();

        if (newMessageCount <= 0) {
            log.info("💾 [SAVE] 没有新消息需要保存");
            return;
        }

        log.info("💾 [SAVE] 识别到 {} 条新消息", newMessageCount);

        // 保存新消息（从末尾开始的新消息）
        for (int i = cachedMessages.size(); i < currentMessages.size(); i++) {
            ChatMessage message = currentMessages.get(i);
            int turnIndex = currentMaxTurnIndex + (i - cachedMessages.size()) + 1;

            Message mongoMessage = createIndividualMessage(memoryId, message, turnIndex);
            if (mongoMessage != null) {
                mongoTemplate.save(mongoMessage);
                log.info("💾 [SAVE] 保存新消息: turn_index={}, type={}, content={}",
                    turnIndex, getMessageType(message), getMessageContentForLog(message));
            }
        }
    }



    /**
     * 获取消息类型字符串
     */
    private String getMessageType(ChatMessage message) {
        if (message instanceof UserMessage) return "USER";
        if (message instanceof AiMessage) return "AI";
        if (message instanceof ToolExecutionResultMessage) return "TOOL_RESULT";
        if (message instanceof SystemMessage) return "SYSTEM";
        return "UNKNOWN";
    }

    /**
     * 删除指定 memoryId 的所有消息
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String memoryIdStr = memoryId.toString();
        log.debug("🗑️ [MEMORY] 删除聊天记忆: memoryId={}", memoryIdStr);

        try {
            // 1. 删除Redis缓存
            cacheService.deleteMessages(memoryIdStr);
            log.debug("🗑️ [CACHE] 删除Redis缓存成功: memoryId={}", memoryIdStr);

            // 2. 删除MongoDB数据
            Query messageQuery = Query.query(Criteria.where("memoryId").is(memoryIdStr));
            mongoTemplate.remove(messageQuery, Message.class);

            Query conversationQuery = Query.query(Criteria.where("memoryId").is(memoryIdStr));
            mongoTemplate.remove(conversationQuery, Conversation.class);

            log.debug("✅ [MEMORY] 删除聊天记忆成功: memoryId={}", memoryIdStr);

        } catch (Exception e) {
            log.error("❌ [MEMORY] 删除聊天记忆失败: memoryId={}", memoryIdStr, e);
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
                    .messageType(Message.MessageType.AI) // 这是一个完整的对话回合，包含用户消息和AI回复
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

    /**
     * 应用窗口淘汰策略，更新Redis缓存（保持真实turnIndex）
     */
    private void updateRedisCacheWithWindowEviction(String memoryId, SystemMessage systemMessage) {
        log.info("=== 应用窗口淘汰策略更新Redis缓存 ===");

        try {
            // 1. 从MongoDB获取最近的消息（基于窗口大小，保持turnIndex信息）
            List<Message> recentMongoMessages = getRecentMessagesFromDB(memoryId, memoryMaxSize);
            log.info("💾 [WINDOW] 从MongoDB获取最近{}条消息", recentMongoMessages.size());

            // 2. 直接构建Redis缓存，保持真实turnIndex
            updateRedisCacheWithRealTurnIndex(memoryId, systemMessage, recentMongoMessages);

            log.info("💾 [WINDOW] 缓存更新完成");

        } catch (Exception e) {
            log.error("❌ [WINDOW] 窗口淘汰策略更新缓存失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 使用真实turnIndex更新Redis缓存
     */
    private void updateRedisCacheWithRealTurnIndex(String memoryId, SystemMessage systemMessage, List<Message> recentMessages) {
        log.info("=== 使用真实turnIndex更新Redis缓存 ===");

        try {
            // 创建缓存包装器
            ChatMessageWrapper wrapper = new ChatMessageWrapper();
            wrapper.setMemoryId(memoryId);
            wrapper.setMaxMessageCount(cacheMaxSize);
            wrapper.setLastAccessTime(LocalDateTime.now());
            wrapper.setMessages(new ArrayList<>());

            // 1. 添加SystemMessage（turnIndex=0）
            ChatMessageWrapper.SerializableMessage systemMsg =
                ChatMessageWrapper.SerializableMessage.fromChatMessage(systemMessage, 0);
            wrapper.getMessages().add(systemMsg);

            // 2. 添加MongoDB消息，保持原有turnIndex
            int maxTurnIndex = 0;
            for (Message mongoMsg : recentMessages) {
                ChatMessage chatMessage = convertIndividualMessageToChatMessage(mongoMsg);
                if (chatMessage != null) {
                    ChatMessageWrapper.SerializableMessage serializableMsg =
                        ChatMessageWrapper.SerializableMessage.fromChatMessage(chatMessage, mongoMsg.getTurnIndex());
                    wrapper.getMessages().add(serializableMsg);
                    maxTurnIndex = Math.max(maxTurnIndex, mongoMsg.getTurnIndex());
                }
            }

            // 3. 设置当前turnIndex为最大的turnIndex
            wrapper.setCurrentTurnIndex(maxTurnIndex);

            // 4. 应用缓存大小限制
            if (wrapper.getMessages().size() > cacheMaxSize) {
                // 保留SystemMessage + 最近的消息
                List<ChatMessageWrapper.SerializableMessage> trimmedMessages = new ArrayList<>();
                trimmedMessages.add(wrapper.getMessages().get(0)); // SystemMessage

                int keepCount = cacheMaxSize - 1;
                int startIndex = wrapper.getMessages().size() - keepCount;
                if (startIndex > 1) { // 跳过SystemMessage
                    trimmedMessages.addAll(wrapper.getMessages().subList(startIndex, wrapper.getMessages().size()));
                } else {
                    trimmedMessages.addAll(wrapper.getMessages().subList(1, wrapper.getMessages().size()));
                }

                wrapper.setMessages(trimmedMessages);
                log.info("💾 [CACHE] 应用缓存大小限制: 保留{}条消息（包含SystemMessage）", wrapper.getMessages().size());
            }

            // 5. 直接保存到Redis
            cacheService.saveWrapper(memoryId, wrapper);

            log.info("✅ [CACHE] Redis缓存更新成功: memoryId={}, 消息数量={}, 当前turn_index={}",
                    memoryId, wrapper.getMessages().size(), wrapper.getCurrentTurnIndex());

            // 6. 输出缓存详情
            log.info("💾 [CACHE] Redis中的消息详情:");
            for (int i = 0; i < wrapper.getMessages().size(); i++) {
                ChatMessageWrapper.SerializableMessage msg = wrapper.getMessages().get(i);
                String content = msg.getContent();
                String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                log.info("  [{}] 类型: {}, turnIndex: {}, 内容: {}", i, msg.getType(), msg.getTurnIndex(), preview);
            }

        } catch (Exception e) {
            log.error("❌ [CACHE] 使用真实turnIndex更新Redis缓存失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 输出消息列表详情
     */
    private void logMessageList(String prefix, List<ChatMessage> messages) {
        log.info("=== {} 消息列表 ===", prefix);
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            String type = getMessageType(msg);
            String content = getMessageContentForLog(msg);
            log.info("[{}] type={}, content={}", i, type, content);
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
                        .id(UUID.randomUUID().toString())
                        .memoryId(memoryId)
                        .userIp("auto-created") // 标记为自动创建
                        .createdTime(LocalDateTime.now())
                        .lastSendTime(LocalDateTime.now())
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
        // 🔍 简单处理：如果第一条消息是AiMessage，则跳过
        List<ChatMessage> processableMessages = new ArrayList<>(messages);
        if (!processableMessages.isEmpty() && processableMessages.get(0) instanceof AiMessage) {
            log.warn("⚠️ [MESSAGES] 跳过第一条孤立的AiMessage");
            processableMessages.remove(0);

            // 如果移除后列表为空，直接返回
            if (processableMessages.isEmpty()) {
                log.info("💬 [MESSAGES] 移除孤立消息后列表为空，跳过保存");
                return;
            }
        }

        List<Message> mongoMessages = new ArrayList<>();
        Integer turnIndex = 1;

        // 将消息按对话回合分组 - 采用更简单的策略
        String currentPrompt = null;
        StringBuilder completionBuilder = new StringBuilder();
        for (int i = 0; i < processableMessages.size(); i++) {
            ChatMessage message = processableMessages.get(i);
            log.info("💬 [MESSAGES] 处理消息 {}/{}: 类型={}, 内容长度={}",
                    i + 1, processableMessages.size(), message.type(),
                    message.toString().length());

            if (message instanceof UserMessage) {
                // 如果之前有未完成的回合，先保存
                if (currentPrompt != null && completionBuilder.length() > 0) {
                    String completion = completionBuilder.toString(); // .trim()
                    log.info("💬 [MESSAGES] 保存回合 {}: prompt长度={}, completion长度={}",
                            turnIndex, currentPrompt.length(),
                            completionBuilder.length());
                    mongoMessages.add(createMongoMessage(memoryId, turnIndex++, currentPrompt, completion, Message.MessageType.AI));
                    completionBuilder.setLength(0); // 清空
                }
                currentPrompt = ((UserMessage) message).singleText();
            } else if (message instanceof AiMessage) {
                // 检查是否有对应的UserMessage
                if (currentPrompt == null) {
                    log.warn("⚠️ [MESSAGES] 检测到孤立的AiMessage，跳过处理（这应该被预处理过滤掉）");
                    continue;
                }

                AiMessage aiMessage = (AiMessage) message;
                if (aiMessage.text() != null && !aiMessage.text().trim().isEmpty()) {
                    // AI有文本回复
                    if (completionBuilder.length() > 0) {
                        completionBuilder.append(" ");
                    }
                    completionBuilder.append(aiMessage.text());
                    log.info("💬 [MESSAGES] AI文本回复: 长度={}", aiMessage.text().length());
                } else if (aiMessage.hasToolExecutionRequests()) {
                    // AI发起工具调用，记录工具调用信息
                    String toolName = aiMessage.toolExecutionRequests().get(0).name();
                    String toolArgs = aiMessage.toolExecutionRequests().get(0).arguments();
                    // AI发起工具调用，记录工具调用信息
                    String toolCallInfo = String.format("【AI调用工具: %s(%s)】", toolName,
                            toolArgs.length() > 100 ? toolArgs.substring(0, 100) + "..." : toolArgs);

                    if (completionBuilder.length() > 0) {
                        completionBuilder.append(" ");
                    }
                    completionBuilder.append(toolCallInfo);
                    log.info("💬 [MESSAGES] AI发起工具调用: {} 参数长度={}", toolName, toolArgs.length());
                }
            } else if (message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage) {
                if (currentPrompt == null) {
                    log.warn("⚠️ [MESSAGES] 检测到孤立的工具结果消息，跳过处理");
                    continue;
                }

                // 工具执行结果消息 - 需要单独保存，不能合并到completion中
                dev.langchain4j.data.message.ToolExecutionResultMessage toolResult =
                        (dev.langchain4j.data.message.ToolExecutionResultMessage) message;
                
                // 先保存当前的回合（如果有的话）
                if (currentPrompt != null && completionBuilder.length() > 0) {
                    String completion = completionBuilder.toString();
                    log.info("💬 [MESSAGES] 保存回合 {}: prompt长度={}, completion长度={}",
                            turnIndex, currentPrompt.length(), completion.length());
                    mongoMessages.add(createMongoMessage(memoryId, turnIndex++, currentPrompt, completion, Message.MessageType.AI));
                    completionBuilder.setLength(0);
                }
                
                // 创建工具调用记录
                Message.ToolCall toolCall = new Message.ToolCall();
                toolCall.setToolName(toolResult.toolName());
                toolCall.setResult(toolResult.text());
                toolCall.setTimestamp(LocalDateTime.now());
                
                // 创建包含工具调用的消息
                Message toolMessage = Message.builder()
                        .id(UUID.randomUUID().toString())
                        .memoryId(memoryId)
                        .turnIndex(turnIndex++)
                        .messageType(Message.MessageType.TOOL_RESULT)
                        .content(new Message.Content("", "工具执行结果"))
                        .sendTime(LocalDateTime.now())
                        .toolCalls(List.of(toolCall))
                        .build();
                
                mongoMessages.add(toolMessage);
                log.info("💬 [MESSAGES] 保存工具执行结果: {} -> {}", toolResult.toolName(), toolResult.text().substring(0, Math.min(50, toolResult.text().length())));
            } else if (message instanceof SystemMessage) {
                // 系统消息单独处理，暂时跳过
                log.info("💬 [MESSAGES] 跳过系统消息: 长度={}", ((SystemMessage) message).text().length());
            } else {
                // 处理其他类型的消息
                log.info("💬 [MESSAGES] 处理其他类型消息: {}", message.getClass().getSimpleName());
            }
        }

        // 保存最后一个回合
        if (currentPrompt != null) { //  completionBuilder 不能做长度 .toString()==null判断
            String finalCompletion = completionBuilder.toString(); // .trim()
//            log.info("💬 [MESSAGES] 保存最后回合 {}: prompt长度={}, completion长度={}",
//                    turnIndex, currentPrompt.length(), finalCompletion.length());
//            mongoMessages.add(createMongoMessage(memoryId, turnIndex, currentPrompt, finalCompletion));

            // 🔧 允许保存只有prompt的回合（用于处理用户刚发送消息但AI还未回复的情况）
            if (!finalCompletion.trim().isEmpty()) {
                log.info("💬 [MESSAGES] 保存完整回合 {}: prompt长度={}, completion长度={}",
                        turnIndex, currentPrompt.length(), finalCompletion.length());
                mongoMessages.add(createMongoMessage(memoryId, turnIndex, currentPrompt, finalCompletion, Message.MessageType.AI));
            } else {
                log.info("💬 [MESSAGES] 保存仅prompt回合 {}: prompt长度={}, completion为空",
                        turnIndex, currentPrompt.length());
                // 🌟 关键修改：允许保存空的completion，这种情况下是用户消息
                mongoMessages.add(createMongoMessage(memoryId, turnIndex, currentPrompt, "", Message.MessageType.USER));
            }

        } else {
            log.warn("⚠️ [MESSAGES] 最后回合completion为空，跳过保存");
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
    private Message createMongoMessage(String memoryId, Integer turnIndex, String prompt, String completion, Message.MessageType messageType) {
        return Message.builder()
                .id(UUID.randomUUID().toString())
                .memoryId(memoryId)
                .turnIndex(turnIndex)
                .messageType(messageType)
                .content(new Message.Content(prompt, completion))
                .sendTime(LocalDateTime.now())
                .model(new Message.ModelInfo("gpt-4o-mini", null, null))
                .build();
    }

    /**
     * 创建独立的消息记录（每个消息类型一条记录）
     */
    private Message createIndividualMessage(String memoryId, ChatMessage chatMessage, int turnIndex) {
        LocalDateTime now = LocalDateTime.now();

        if (chatMessage instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) chatMessage;
            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .turnIndex(turnIndex)
                    .messageType(Message.MessageType.USER)
                    .content(new Message.Content(userMessage.singleText(), ""))
                    .sendTime(now)
                    .build();

        } else if (chatMessage instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage) chatMessage;

            if (aiMessage.hasToolExecutionRequests()) {
                // AI工具调用消息
                String toolCallInfo = formatToolCallInfo(aiMessage.toolExecutionRequests().get(0));
                return Message.builder()
                        .id(UUID.randomUUID().toString())
                        .memoryId(memoryId)
                        .turnIndex(turnIndex)
                        .messageType(Message.MessageType.TOOL_CALL)
                        .content(new Message.Content("", toolCallInfo))
                        .sendTime(now)
                        .build();
            } else {
                // 普通AI回复消息
                return Message.builder()
                        .id(UUID.randomUUID().toString())
                        .memoryId(memoryId)
                        .turnIndex(turnIndex)
                        .messageType(Message.MessageType.AI)
                        .content(new Message.Content("", aiMessage.text()))
                        .sendTime(now)
                        .build();
            }

        } else if (chatMessage instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) chatMessage;
            String toolResultInfo = formatToolResultInfo(toolResult);

            // 创建工具调用记录
            Message.ToolCall toolCall = new Message.ToolCall();
            toolCall.setToolName(toolResult.toolName());
            toolCall.setResult(toolResult.text());
            toolCall.setTimestamp(now);

            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .turnIndex(turnIndex)
                    .messageType(Message.MessageType.TOOL_RESULT)
                    .content(new Message.Content("", toolResultInfo))
                    .sendTime(now)
                    .toolCalls(List.of(toolCall))
                    .build();

        } else if (chatMessage instanceof SystemMessage) {
            SystemMessage systemMessage = (SystemMessage) chatMessage;
            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .memoryId(memoryId)
                    .turnIndex(turnIndex)
                    .messageType(Message.MessageType.SYSTEM)
                    .content(new Message.Content("", systemMessage.text()))
                    .sendTime(now)
                    .build();
        }

        log.warn("⚠️ [INDIVIDUAL] 未知的消息类型: {}", chatMessage.getClass().getSimpleName());
        return null;
    }

    /**
     * 格式化工具调用信息
     */
    private String formatToolCallInfo(dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest) {
        return String.format("TOOL_CALL|ID:%s|NAME:%s|ARGS:%s",
                toolRequest.id(), toolRequest.name(), toolRequest.arguments());
    }

    /**
     * 格式化工具结果信息
     */
    private String formatToolResultInfo(ToolExecutionResultMessage toolResult) {
        return String.format("ID:%s|NAME:%s|RESULT:%s",
                toolResult.id(), toolResult.toolName(), toolResult.text());
    }

    /**
     * 将独立的Message记录转换为ChatMessage
     */
    private ChatMessage convertIndividualMessageToChatMessage(Message message) {
        if (message.getMessageType() == null || message.getContent() == null) {
            return null;
        }

        switch (message.getMessageType()) {
            case USER:
                String userContent = message.getContent().getPrompt();
                if (userContent == null || userContent.trim().isEmpty()) {
                    userContent = message.getContent().getCompletion();
                }
                return UserMessage.from(userContent);

            case AI:
                String aiContent = message.getContent().getCompletion();
                if (aiContent == null || aiContent.trim().isEmpty()) {
                    aiContent = message.getContent().getPrompt();
                }
                return AiMessage.from(aiContent);

            case TOOL_CALL:
                String toolCallContent = message.getContent().getCompletion();
                if (toolCallContent == null || toolCallContent.trim().isEmpty()) {
                    toolCallContent = message.getContent().getPrompt();
                }
                return parseToolCallMessage(toolCallContent);

            case TOOL_RESULT:
                String toolResultContent = message.getContent().getCompletion();
                if (toolResultContent == null || toolResultContent.trim().isEmpty()) {
                    toolResultContent = message.getContent().getPrompt();
                }
                return parseToolResultMessage(toolResultContent);

            case SYSTEM:
                String systemContent = message.getContent().getCompletion();
                if (systemContent == null || systemContent.trim().isEmpty()) {
                    systemContent = message.getContent().getPrompt();
                }
                return SystemMessage.from(systemContent);

            default:
                log.warn("⚠️ [CONVERT] 未知的消息类型: {}", message.getMessageType());
                return null;
        }
    }

    /**
     * 解析工具调用消息
     */
    private ChatMessage parseToolCallMessage(String content) {
        try {
            if (content.startsWith("TOOL_CALL|")) {
                String[] parts = content.split("\\|");
                String id = null, name = null, args = null;

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
                    dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest =
                        dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                            .id(id)
                            .name(name)
                            .arguments(args)
                            .build();
                    return AiMessage.from(toolRequest);
                }
            }
        } catch (Exception e) {
            log.error("❌ [CONVERT] 解析工具调用消息失败: {}", content, e);
        }
        return AiMessage.from("工具调用: " + content);
    }

    /**
     * 解析工具结果消息
     */
    private ChatMessage parseToolResultMessage(String content) {
        try {
            if (content.startsWith("ID:")) {
                String[] parts = content.split("\\|");
                String id = null, name = null, result = null;

                for (String part : parts) {
                    if (part.startsWith("ID:")) {
                        id = part.substring(3);
                    } else if (part.startsWith("NAME:")) {
                        name = part.substring(5);
                    } else if (part.startsWith("RESULT:")) {
                        result = part.substring(7);
                    }
                }

                if (id != null && name != null && result != null) {
                    return ToolExecutionResultMessage.from(id, name, result);
                }
            }
        } catch (Exception e) {
            log.error("❌ [CONVERT] 解析工具结果消息失败: {}", content, e);
        }
        return ToolExecutionResultMessage.from("unknown", "unknown", content);
    }

}