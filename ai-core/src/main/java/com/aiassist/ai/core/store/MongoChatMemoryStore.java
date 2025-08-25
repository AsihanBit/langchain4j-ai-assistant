package com.aiassist.ai.core.store;

import com.aiassist.ai.core.config.PromptProvider;
import com.aiassist.ai.core.entity.ChatMessageWrapper;
import com.aiassist.ai.core.entity.Conversation;
import com.aiassist.ai.core.entity.Message;
import com.aiassist.ai.core.service.ChatMessageCacheService;
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
import java.util.*;
import java.util.stream.Collectors;


/**
 * MongoDB 实现的 LangChain4j ChatMemoryStore
 * 负责将 LangChain4j 的消息格式与 MongoDB 的混合存储方案进行转换
 */
@Slf4j
@Component
public class MongoChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private PromptProvider promptProvider;

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
        log.info("=== getMessages 方法调用 ===");
        log.info("🔍 [MEMORY] 获取聊天记忆: memoryId={}, 窗口大小={}", memoryIdStr, memoryMaxSize);

        // 添加调用栈信息来追踪谁在调用这个方法
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("🔍 [STACK] 调用栈: {}", stackTrace.length > 3 ? stackTrace[3].toString() : "unknown");

        try {
            // 1. 首先从Redis缓存获取
            List<ChatMessage> cachedMessages = cacheService.getMessages(memoryIdStr);
            log.info("🔍 [CACHE] Redis缓存消息数量: {}", cachedMessages != null ? cachedMessages.size() : 0);

            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                // 如果有缓存，直接返回缓存结果
//                delIsolateToolResultChatMessage(cachedMessages); 没必要 getMessage里面检查了,会死循环
                logMessageList("getMessages 方法调用 (缓存)", cachedMessages);
                return cachedMessages;
            }

            // 2. 缓存未命中，从MongoDB获取最近的消息
            List<Message> dbMessages = getRecentMessagesFromDB(memoryIdStr);
            delIsolateToolResultMessage(dbMessages); // 从数据库获取时检查, 待整合进 updateMessage
            log.info("🔍 [MONGO] MongoDB最近消息数量: {}", dbMessages.size());

            if (dbMessages.isEmpty()) {
                return new ArrayList<>();
            }

            // 3. 使用带有真实turnIndex的原始Message列表更新Redis缓存
            SystemMessage systemMessage = createSystemMessage(); // 获取默认的SystemMessage
            List<ChatMessage> chatMessages = createRedisCacheFromDBMessage(memoryIdStr, systemMessage, dbMessages);
            log.info("💾 [CACHE] 已将带有真实turnIndex的消息缓存到Redis: memoryId={} 消息数量: {}", memoryIdStr, chatMessages.size());

            logMessageList("getMessages 方法调用 (mongo)", chatMessages);

            return chatMessages;

        } catch (Exception e) {
            log.error("❌ [MEMORY] 获取聊天记忆失败: memoryId={}", memoryIdStr, e);
            return new ArrayList<>();
        }
    }

    /**
     * 去除孤立的TOOL_RESULT
     */
    private void delIsolateToolResultChatMessage(List<ChatMessage> messages) {
        // 在 getMessages 的缓存命中分支，返回之前加这段
        if (messages != null && messages.size() > 1) {
            // 清理：如果第一条非System是TOOL_RESULT，就删除直到不是TOOL_RESULT
            while (messages.size() > 1 &&
                    messages.get(1) instanceof ToolExecutionResultMessage) {
                messages.remove(1);
                log.info("⚠️ [CLEAN] 清理开头孤立的TOOL_RESULT (缓存)");
            }
        }
    }

    private void delIsolateToolResultMessage(List<Message> messages) {
        // 在 getMessages 的缓存命中分支，返回之前加这段
        if (messages != null && messages.size() > 1) {
            // 清理：如果第一条非System是TOOL_RESULT，就删除直到不是TOOL_RESULT
            while (messages.size() > 1 &&
                    messages.get(1).getMessageType() == Message.MessageType.TOOL_RESULT) {
                messages.remove(1);
                log.info("⚠️ [CLEAN] 清理开头孤立的TOOL_RESULT (数据库)");
            }
        }
    }

    /**
     * 更新指定 memoryId 的消息列表（替换所有消息）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        logMessageList("触发函数 updateMessages", messages);
        String memoryIdStr = memoryId.toString();
        log.info("=== updateMessages调用 ===");
        log.info("💾 [MEMORY] 更新聊天记忆: memoryId={}, 消息数量={}, 窗口大小={}", memoryIdStr, messages.size(), memoryMaxSize);

        // 添加调用栈信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("[STACK] 调用栈: {}", stackTrace.length > 3 ? stackTrace[3].toString() : "unknown");

        try {
            // 1. 过滤SystemMessage，只保留turn_index=0的SystemMessage
            SystemMessage systemMessage = extractSystemMessage(messages);
            List<ChatMessage> nonSystemMessages = filterNonSystemMessages(messages);

            log.info("[FILTER] 过滤后非SystemMessage数量: {}", nonSystemMessages.size());

            if (nonSystemMessages.isEmpty()) {
                // 如果没有实际消息，直接返回
                return;
            }

            ChatMessage lastMessage = nonSystemMessages.get(nonSystemMessages.size() - 1);
            // 1. 增加工具调用失败的回滚逻辑
            if ((lastMessage instanceof UserMessage || messageIsToolCall(lastMessage)) && nonSystemMessages.size() > 1) {
                ChatMessage previousMessage = nonSystemMessages.get(nonSystemMessages.size() - 2);
                if (messageIsToolCall(previousMessage)) {
                    log.info("[ROLLBACK] 检测到失败的TOOL_CALL，执行回滚...");
                    // TODO 回滚后, 新UserMessage turn_index 问题检查
                    // 1) 先从 nonSystemMessages 中移除那条 TOOL_CALL
                    nonSystemMessages.remove(nonSystemMessages.size() - 2);// 移除那条失败的TOOL_CALL
                    // 2) 同步回滚 wrapper（删除尾部 TOOL_CALL 并 currentTurnIndex--）
//                    rollbackIfUserAfterToolCall(wrapper, nonSystemMessages);
//                    // 3) 这次属于“最终态”：入库 + 覆盖缓存
//                    saveOnlyNewMessages(memoryIdStr, nonSystemMessages);
//                    updateRedisCacheIncrementally(memoryIdStr, systemMessage, nonSystemMessages);
//                    return;
                }
            }
            // 重新获取最后一条消息，因为它可能在回滚后已改变 TODO 检查保底必要性
            lastMessage = nonSystemMessages.get(nonSystemMessages.size() - 1);

            // 2. 识别并保存真正的新消息 核心路由判断
            if (lastMessage instanceof AiMessage && ((AiMessage) lastMessage).hasToolExecutionRequests()) {
                // TOOL_CALL：只更Redis (中间状态)
                log.info("[SKIP] 检测到TOOL_CALL，仅增量更新缓存...");
                updateRedisCacheIncrementally(memoryIdStr, systemMessage, nonSystemMessages);
                return;
            } else if (lastMessage instanceof ToolExecutionResultMessage) {
                log.info("[SAVE] 检测到TOOL_RESULT，开始原子持久化 TOOL_CALL + TOOL_RESULT...");
                // 1) 落库（saveOnlyNewMessages 内部会识别并把 TOOL_CALL+TOOL_RESULT 一起入库）
                saveOnlyNewMessages(memoryIdStr, nonSystemMessages);
                // 2) 用DB的真实turnIndex覆盖刷新Redis
                updateRedisCacheIncrementally(memoryIdStr, systemMessage, nonSystemMessages);
                return;
            } else {
                // 最终AI文本：此时才入库并用DB覆盖缓存
                saveOnlyNewMessages(memoryIdStr, nonSystemMessages);
                updateRedisCacheIncrementally(memoryIdStr, systemMessage, nonSystemMessages);
            }
            log.info("✅ [updateMessages] 完成: memoryId={}", memoryIdStr);

        } catch (Exception e) {
            log.error("❌ [updateMessages] 更新聊天记忆失败: memoryId={}", memoryIdStr, e);
            // 不重新抛出异常，避免影响LangChain4j的主流程
        }
    }

    private void updateRedisCacheIncrementally(
            String memoryId, SystemMessage systemMessage,
            List<ChatMessage> nonSystemMessages) {
        log.info("=== 增量更新Redis缓存（中间态）===");
        try {
            ChatMessageWrapper wrapper = getOrInitWrapper(memoryId, systemMessage);
            if (!nonSystemMessages.isEmpty()) {
                ChatMessage lastIncoming = nonSystemMessages.get(nonSystemMessages.size() - 1);
                List<ChatMessage> cachedChats = wrapper.getChatMessages();
                if (lastIncoming instanceof UserMessage && cachedChats.size() > 1) {
                    ChatMessage lastCached = cachedChats.get(cachedChats.size() - 1);
                    if (lastCached instanceof AiMessage &&
                            ((AiMessage) lastCached).hasToolExecutionRequests()) {
                        wrapper.getMessages().remove(wrapper.getMessages().size() - 1);
                        wrapper.setCurrentTurnIndex(Math.max(0, wrapper.getCurrentTurnIndex() - 1));
                        log.info("⚠️ [REMOVE] 移除孤立的 TOOL_CALL (结尾)");
                    }
                }
            }

            incrementUpdateWrapper(wrapper, nonSystemMessages);

            controlContextLimit(wrapper);

            cacheService.saveWrapper(memoryId, wrapper);
            log.info("✅ [updateRedisCacheIncrementally] 增量更新完成: memoryId={}, size={}, currentTurnIndex={}",
                    memoryId, wrapper.getMessages().size(), wrapper.getCurrentTurnIndex());
        } catch (Exception e) {
            log.error("❌ [updateRedisCacheIncrementally] 增量更新失败: memoryId={}", memoryId, e);
        }
    }


    // ==================== 新的辅助方法 ====================

    private ChatMessageWrapper getOrInitWrapper(String memoryId, SystemMessage sys) {
        ChatMessageWrapper w = cacheService.getCacheInfo(memoryId);
        if (w == null) {
            w = createNewWrapper(memoryId, sys);
        }
        return w;
    }

    private ChatMessageWrapper createNewWrapper(String memoryId, SystemMessage sys) {
        ChatMessageWrapper build = ChatMessageWrapper.builder()
                .memoryId(memoryId).maxMessageCount(cacheMaxSize)
                .lastAccessTime(LocalDateTime.now())
                .messages(new ArrayList<>()).currentTurnIndex(0).build();
        build.getMessages().add(ChatMessageWrapper.SerializableMessage.fromChatMessage(sys, 0));
        return build;
    }

    /**
     * 移动窗口淘汰策略
     */
    private void controlContextLimit(ChatMessageWrapper wrapper) {
        int size = wrapper.getMessages().size();
        if (size <= 1) return; // 只有System
//        if (size <= cacheMaxSize) return; 不能打开,因为每次都要检查清除开头孤立的 TOOL_RESULT

        // 保留SystemMessage + 最近的消息
        List<ChatMessageWrapper.SerializableMessage> trimmed = new ArrayList<>();
        trimmed.add(wrapper.getMessages().get(0)); // SystemMessage

//            int keep = cacheMaxSize - 1;
//            int startIndex = Math.max(1, size - keep);

        int startIndex = computeTrimStart(wrapper);

        trimmed.addAll(wrapper.getMessages().subList(startIndex, size));

        wrapper.setMessages(trimmed);

        // 更新访问时间 TODO 和 expire_time 统一
        wrapper.setLastAccessTime(LocalDateTime.now());
        log.info("💾 [controlContextLimit] 应用缓存大小限制: 保留{}条消息（包含SystemMessage）", wrapper.getMessages().size());
    }

    private void incrementUpdateWrapper(ChatMessageWrapper wrapper, List<ChatMessage> nonSystemMessages) {
//        int cacheSize = wrapper.getMessages().size();
//        int newMessagesSize = nonSystemMessages.size();
//        int toAppend = newMessagesSize - (cacheSize - 1); // 保留 systemMessage
//        if(toAppend <= 0) {
//            log.info("💾 [CACHE] 无增量，跳过");
//            return;
//        }
//        if (toAppend > 0) {
//            // 后面递增
//            int startIdx = cacheSize;
//            int curIdx = wrapper.getCurrentTurnIndex();
//            for (int i = 0; i < toAppend; i++) {
//                ChatMessage msg = nonSystemMessages.get(startIdx + i);
//                int next = ++curIdx;
//                wrapper.getMessages().add(
//                        ChatMessageWrapper.SerializableMessage.fromChatMessage(msg, next));
//            }
//        }

        int exist = (int) wrapper.getChatMessages().stream()
                .filter(m -> !(m instanceof SystemMessage)).count();
        int total = nonSystemMessages.size();
        if (total <= exist) {
            log.info("💾 [incrementUpdateWrapper] 无增量，跳过: exist={}, total={}", exist, total);
            return;
        }

        int curr = wrapper.getCurrentTurnIndex();
        for (int idx = exist; idx < total; idx++) {
            ChatMessage msg = nonSystemMessages.get(idx); // 不要用 startIdx + i
            wrapper.getMessages().add(
                    ChatMessageWrapper.SerializableMessage.fromChatMessage(msg, ++curr)
            );
        }

        wrapper.setCurrentTurnIndex(curr);

    }

    /**
     * 判断消息是不是 ToolCall
     *
     * @param message
     * @return
     */
    private boolean messageIsToolCall(ChatMessage message) {
        return message instanceof AiMessage && ((AiMessage) message).hasToolExecutionRequests();
    }

    /**
     * 从MongoDB获取最近的消息（基于窗口大小）
     */
    private List<Message> getRecentMessagesFromDB(String memoryId) {
        try {
            // 获取最近的消息，数量为窗口大小
            Query query = new Query(Criteria.where("memoryId").is(memoryId))
                    .with(Sort.by(Sort.Direction.DESC, "turnIndex"))
                    .limit(memoryMaxSize);

            List<Message> messages = mongoTemplate.find(query, Message.class);
            log.info("🔍 [MONGO] 查询到最近 {} 条消息记录", messages.size());

            // 按turnIndex正序排列
//            messages.sort(Comparator.comparingInt(Message::getTurnIndex));
            // 将降序结果倒序为正序
            Collections.reverse(messages);

            return messages;
        } catch (Exception e) {
            log.error("❌ [MONGO] 从MongoDB获取最近消息失败: memoryId={}", memoryId, e);
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
        // 从配置类中获取 chat.prompt.path 的值
        return promptProvider.systemMessage();
        // 使用 prompt 路径构建 SystemMessage
//        try {
//            String prompt = Files.readString(Paths.get(promptFilePath));
//            return SystemMessage.from(prompt);
//        } catch (Exception e) {
//            e.printStackTrace();
//            // 提供默认值以应对失败场景
//            return SystemMessage.from("你是一个助手，能够帮助用户解答相关问题。");
//        }
    }

    /**
     * 提取SystemMessage（只保留turn_index=0的）
     */
    private SystemMessage extractSystemMessage(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                log.info("  [SystemMessage] 发现SystemMessage，将保留在turn_index=0");
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
        log.info("=== saveOnlyNewMessages 识别并保存新消息 ===");

        // 从Redis缓存获取 目前 turn_index
        ChatMessageWrapper wrapper = cacheService.getCacheInfo(memoryId);
        int currentTurnIdx = -1;
        if (wrapper != null) {
            currentTurnIdx = wrapper.getCurrentTurnIndex();
            log.info("🔍 [CACHE] Redis缓存中 获取到当前 turn_index : {}", currentTurnIdx);
        } else {
            wrapper = createNewWrapper(memoryId, createSystemMessage());
            log.info("🔍 [CACHE] Redis缓存中 没有获取到 turn_index : {}", currentTurnIdx);
        }
        logMessageList("save 传入的消息", currentMessages);
        logMessageList("save 缓存中消息", wrapper.getChatMessages());

        if (currentMessages.isEmpty()) {
            log.info("💾 [SAVE] 没有消息需要处理");
            return;
        }
        // [FIX] 优化turn_index获取逻辑，优先从缓存获取，缓存失效则从DB查询
        int currentMaxTurnIndex;
        if (currentTurnIdx <= 0) { // 缓存中没有有效的turn_index，说明缓存是新生成的
            log.warn("⚠️ [SAVE] Redis缓存的turn_index无效 ({})，将从MongoDB重新查询以确保数据一致性...", currentTurnIdx);
            currentMaxTurnIndex = getCurrentMaxTurnIndex(memoryId);
        } else {
            currentMaxTurnIndex = currentTurnIdx;
        }
        log.info("💾 [SAVE] 确定当前最大turn_index为: {}", currentMaxTurnIndex);

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

        // 计算起始下标：默认从 cachedMessages.size() 开始
        int cachedCount = cachedMessages.size();
        int startIndex = cachedCount;

        // 若最后一条是 TOOL_RESULT 且缓存末尾是 TOOL_CALL，则回退 1，成对保存
        ChatMessage lastIncoming = currentMessages.get(currentMessages.size() - 1);
        boolean cacheEndsWithToolCall = cachedCount > 0
                && (cachedMessages.get(cachedCount - 1) instanceof AiMessage)
                && ((AiMessage) cachedMessages.get(cachedCount - 1)).hasToolExecutionRequests();
//        if (lastIncoming instanceof ToolExecutionResultMessage && cacheEndsWithToolCall) {
//            startIndex = Math.max(0, cachedCount - 1);
//        }
        // 特例：TOOL_RESULT 到来且缓存末尾是 TOOL_CALL -> 成对入库，沿用 Redis 中的 turn_index
        if (lastIncoming instanceof ToolExecutionResultMessage && cacheEndsWithToolCall) {
            int base = wrapper.getCurrentTurnIndex(); // 这就是 Redis 分配给 TOOL_CALL 的 turn_index

            // 先保存 TOOL_CALL（沿用 base）
            ChatMessage toolCallMsg = currentMessages.get(cachedCount - 1);
            Message m1 = createIndividualMessage(memoryId, toolCallMsg, base);
            if (m1 != null) mongoTemplate.save(m1);

            // 再保存 TOOL_RESULT（base + 1）
            ChatMessage toolResultMsg = currentMessages.get(cachedCount);
            Message m2 = createIndividualMessage(memoryId, toolResultMsg, base + 1);
            if (m2 != null) mongoTemplate.save(m2);

            return; // 成对入库完成，返回
        }

        int toSave = currentMessages.size() - startIndex;
        if (toSave <= 0) {
            log.info("💾 [SAVE] 没有新消息需要保存");
            return;
        }

        // 保存新消息（从末尾开始的新消息）
        // 从 startIndex 开始逐条保存（保证 TOOL_CALL + TOOL_RESULT 成对入库，turnIndex 连续）
        for (int i = startIndex; i < currentMessages.size(); i++) {
            ChatMessage message = currentMessages.get(i);
            int turnIndex = currentMaxTurnIndex + (i - startIndex) + 1;
            log.info("💾 [SAVE] 计算新消息的turn_index: {} + ( {} - {} ) + 1 = {}",
                    currentMaxTurnIndex, i, startIndex, turnIndex);

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
     * 使用真实turnIndex更新Redis缓存
     */
    private List<ChatMessage> createRedisCacheFromDBMessage(String memoryId, SystemMessage systemMessage, List<Message> recentMessages) {
        log.info("=== 使用真实turnIndex更新Redis缓存 ===");

        try {
            // 1. 创建缓存包装器, 添加SystemMessage（turnIndex=0）
            ChatMessageWrapper wrapper = createNewWrapper(memoryId, systemMessage);

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
            controlContextLimit(wrapper);

            // 5. 直接保存到Redis
            cacheService.saveWrapper(memoryId, wrapper);

            log.info("✅ [CACHE] Redis缓存更新成功: memoryId={}, 消息数量={}, 当前turn_index={}",
                    memoryId, wrapper.getMessages().size(), wrapper.getCurrentTurnIndex());

            return wrapper.getChatMessages();
        } catch (Exception e) {
            log.error("❌ [CACHE] 使用真实turnIndex更新Redis缓存失败: memoryId={}", memoryId, e);
            return List.of();
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
        log.info("=== 输出结束 ===");
    }

    // ==================== 私有辅助方法 ====================

    private int computeTrimStart(ChatMessageWrapper wrapper) {
        int size = wrapper.getMessages().size();
        int keep = cacheMaxSize - 1;             // 实际消息最多条数
        int start = Math.max(1, size - keep);    // 跳过 System(0)
        while (start < size &&
                wrapper.getMessages().get(start).getType()
                        == ChatMessageWrapper.SerializableMessage.MessageType.TOOL_RESULT) {
            start++;
            log.info("⚠️ [ROLLBACK] 移除孤立的 TOOL_RESULT (开头)");
        }
        return start;
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
     * 创建 MongoDB 消息文档
     */
    /*private Message createMongoMessage(String memoryId, Integer turnIndex, String prompt, String completion, Message.MessageType messageType) {
        return Message.builder()
                .id(UUID.randomUUID().toString())
                .memoryId(memoryId)
                .turnIndex(turnIndex)
                .messageType(messageType)
                .content(new Message.Content(prompt, completion))
                .sendTime(LocalDateTime.now())
                .model(new Message.ModelInfo("gpt-4o-mini", null, null))
                .build();
    }*/

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