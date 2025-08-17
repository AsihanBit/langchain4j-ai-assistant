package com.aiassist.mediassist.service;

import com.aiassist.mediassist.dto.entity.ChatMessageWrapper;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
     * 最大消息数量
     */
    @Value("${chat.cache.max-messages:50}")
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
     * 更新缓存消息
     */
    public void updateMessages(String memoryId, List<ChatMessage> messages) {
        String cacheKey = getCacheKey(memoryId);
        log.debug("💾 [CACHE] 更新Redis缓存: memoryId={}, 消息数量={}", memoryId, messages.size());

        try {
            ChatMessageWrapper wrapper = ChatMessageWrapper.fromChatMessages(memoryId, messages, maxMessageCount);
            redisTemplate.opsForValue().set(cacheKey, wrapper, Duration.ofHours(cacheExpireHours));
            log.debug("✅ [CACHE] 更新Redis缓存成功: memoryId={}", memoryId);
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
