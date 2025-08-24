package com.aiassist.ai.core.service;

import com.aiassist.ai.core.entity.ChatMessageWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * ChatMessage Redis 缓存服务
 * 负责 ChatMessage 的缓存操作，包括 LRU 淘汰策略
 */
@Slf4j
@Service
public class ChatMessageCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存过期时间（小时）
     */
    @Value("${chat.cache.expire-hours:24}")
    private int cacheExpireHours;

    /**
     * Redis缓存中的最大消息数量（包含1个SystemMessage + memoryMaxSize条实际消息）
     */
    @Value("${chat.cache.max-size:7}")
    private int maxMessageCount;

    /**
     * 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "chat:memory:";

    /**
     * 获取缓存键
     */
    private String getCacheKey(String memoryId) {
        return CACHE_KEY_PREFIX + memoryId;
    }

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

    private void logChatMessageList(List<ChatMessage> chatMessages) {
        for (int i = 0; i < chatMessages.size(); i++) {
            ChatMessage msg = chatMessages.get(i);
            String content = getMessageContentForLog(msg);
            log.info("  [{}] 类型: {}, 内容: {}", i, msg.type().toString(), content);
        }
    }

    /**
     * 比较两条消息是否相同
     */
    private boolean isSameMessage(ChatMessage msg1, ChatMessage msg2) {
        if (msg1 == null || msg2 == null) {
            return msg1 == msg2;
        }

        // 类型不同则不同
        if (!msg1.type().equals(msg2.type())) {
            return false;
        }

        try {
            // 比较消息内容
            if (msg1 instanceof UserMessage && msg2 instanceof UserMessage) {
                String text1 = ((UserMessage) msg1).singleText();
                String text2 = ((UserMessage) msg2).singleText();
                return java.util.Objects.equals(text1, text2);
            } else if (msg1 instanceof AiMessage && msg2 instanceof AiMessage) {
                String text1 = ((AiMessage) msg1).text();
                String text2 = ((AiMessage) msg2).text();
                return java.util.Objects.equals(text1, text2);
            } else if (msg1 instanceof ToolExecutionResultMessage && msg2 instanceof ToolExecutionResultMessage) {
                String text1 = ((ToolExecutionResultMessage) msg1).text();
                String text2 = ((ToolExecutionResultMessage) msg2).text();
                return java.util.Objects.equals(text1, text2);
            } else if (msg1 instanceof SystemMessage && msg2 instanceof SystemMessage) {
                String text1 = ((SystemMessage) msg1).text();
                String text2 = ((SystemMessage) msg2).text();
                return java.util.Objects.equals(text1, text2);
            } else {
                // 对于其他类型，使用toString比较
                return msg1.toString().equals(msg2.toString());
            }
        } catch (Exception e) {
            log.warn("比较消息时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从缓存获取消息
     */
    public List<ChatMessage> getMessages(String memoryId) {
        String cacheKey = getCacheKey(memoryId);
        log.debug("🔍 [CACHE] 从Redis获取消息: memoryId={}", memoryId);

        try {
            ChatMessageWrapper wrapper = (ChatMessageWrapper) redisTemplate.opsForValue().get(cacheKey);
            if (wrapper != null) {
                wrapper.updateAccessTime();
                // 更新访问时间到Redis
                redisTemplate.expire(cacheKey, Duration.ofHours(cacheExpireHours));
                log.debug("✅ [CACHE] 从Redis获取消息成功: memoryId={}, 消息数量={}", memoryId, wrapper.getMessageCount());
                return wrapper.getChatMessages();
            }
        } catch (Exception e) {
            log.error("❌ [CACHE] 从Redis获取消息失败: memoryId={}", memoryId, e);
        }

        log.debug("🔍 [CACHE] Redis中无缓存数据: memoryId={}", memoryId);
        return null;
    }

    /**
     * 更新缓存消息（增量更新）
     */
    public void updateMessages(String memoryId, List<ChatMessage> messages) {
        String cacheKey = getCacheKey(memoryId);
        log.info("💾 [CACHE] 增量更新Redis缓存: memoryId={}, 消息数量={}", memoryId, messages.size());

        // 输出传入的消息列表详情
        log.info("💾 [CACHE] 传入的消息列表详情:");
        logChatMessageList(messages);

        try {
            // 直接替换整个缓存，避免重复消息问题
            ChatMessageWrapper wrapper = ChatMessageWrapper.fromChatMessages(memoryId, messages, maxMessageCount);
            redisTemplate.opsForValue().set(cacheKey, wrapper, Duration.ofHours(cacheExpireHours));

            log.info("✅ [CACHE] 替换Redis缓存成功: memoryId={}, 消息数量={}, 当前turn_index={}",
                    memoryId, wrapper.getMessageCount(), wrapper.getCurrentTurnIndex());

            // 输出Redis中的消息详情
            log.info("💾 [CACHE] Redis中的消息详情:");
            List<ChatMessage> cachedMessages = wrapper.getChatMessages();
            logChatMessageList(cachedMessages);

        } catch (Exception e) {
            log.error("❌ [CACHE] 更新Redis缓存失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 添加单条消息到缓存
     */
    public void addMessage(String memoryId, ChatMessage message) {
        String cacheKey = getCacheKey(memoryId);
        log.debug("➕ [CACHE] 添加消息到Redis: memoryId={}", memoryId);

        try {
            ChatMessageWrapper wrapper = (ChatMessageWrapper) redisTemplate.opsForValue().get(cacheKey);
            if (wrapper == null) {
                wrapper = ChatMessageWrapper.builder()
                        .memoryId(memoryId)
                        .maxMessageCount(maxMessageCount)
                        .build();
            }

            wrapper.addMessage(message);
            redisTemplate.opsForValue().set(cacheKey, wrapper, Duration.ofHours(cacheExpireHours));
            log.debug("✅ [CACHE] 添加消息到Redis成功: memoryId={}, 当前消息数量={}", memoryId, wrapper.getMessageCount());
        } catch (Exception e) {
            log.error("❌ [CACHE] 添加消息到Redis失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 直接保存包装器到Redis（保持原有turnIndex）
     */
    public void saveWrapper(String memoryId, ChatMessageWrapper wrapper) {
        String cacheKey = getCacheKey(memoryId);
        log.info("💾 [CACHE] 直接保存包装器到Redis: memoryId={}", memoryId);

        try {
            redisTemplate.opsForValue().set(cacheKey, wrapper, Duration.ofHours(cacheExpireHours));
            log.info("✅ [CACHE] 直接保存包装器成功: memoryId={}, 消息数量={}, currentTurnIndex={}",
                    memoryId, wrapper.getMessages().size(), wrapper.getCurrentTurnIndex());
        } catch (Exception e) {
            log.error("❌ [CACHE] 直接保存包装器失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 删除缓存
     */
    public void deleteMessages(String memoryId) {
        String cacheKey = getCacheKey(memoryId);
        log.debug("🗑️ [CACHE] 删除Redis缓存: memoryId={}", memoryId);

        try {
            redisTemplate.delete(cacheKey);
            log.debug("✅ [CACHE] 删除Redis缓存成功: memoryId={}", memoryId);
        } catch (Exception e) {
            log.error("❌ [CACHE] 删除Redis缓存失败: memoryId={}", memoryId, e);
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String memoryId) {
        String cacheKey = getCacheKey(memoryId);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("❌ [CACHE] 检查缓存存在性失败: memoryId={}", memoryId, e);
            return false;
        }
    }

    /**
     * 获取缓存统计信息
     */
    public ChatMessageWrapper getCacheInfo(String memoryId) {
        String cacheKey = getCacheKey(memoryId);
        try {
            return (ChatMessageWrapper) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.error("❌ [CACHE] 获取缓存信息失败: memoryId={}", memoryId, e);
            return null;
        }
    }

    /**
     * 获取当前的turn_index
     */
    public Integer getCurrentTurnIndex(String memoryId) {
        String cacheKey = getCacheKey(memoryId);
        try {
            ChatMessageWrapper wrapper = (ChatMessageWrapper) redisTemplate.opsForValue().get(cacheKey);
            if (wrapper != null) {
                return wrapper.getCurrentTurnIndex();
            }
            return 0; // 如果没有缓存，返回0
        } catch (Exception e) {
            log.error("❌ [CACHE] 获取当前turn_index失败: memoryId={}", memoryId, e);
            return 0;
        }
    }

    /**
     * 清空所有聊天缓存（谨慎使用）
     */
    public void clearAllChatCache() {
        log.warn("⚠️ [CACHE] 清空所有聊天缓存");
        try {
            // 这里需要根据实际情况实现，可能需要使用 Redis 的 SCAN 命令
            // 为了安全起见，这里只是示例
            log.warn("⚠️ [CACHE] 清空所有聊天缓存功能需要根据实际Redis键模式实现");
        } catch (Exception e) {
            log.error("❌ [CACHE] 清空所有聊天缓存失败", e);
        }
    }

}
