package com.aiassist.mediassist.config;

import com.aiassist.mediassist.dto.entity.ChatContextCache;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisConfigurationTest {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void redisTemplate1() {
        String sessionId = "session-abcde";
        List<ChatMessage> chatMessages = List.of(
                new UserMessage("Spring Test 好用吗？"),
                new AiMessage("非常好用，特别是配合 Embedded Redis！")
        );
        String redisKey = "chat_context:" + sessionId;
        // --- 2. 执行 (Act) ---
        // 将对象存入 Redis
        redisTemplate.opsForValue().set(redisKey, chatMessages);
        // 从 Redis 中取出对象
        Object retrievedObject = redisTemplate.opsForValue().get(redisKey);
        // --- 3. 断言 (Assert) ---
        assertThat(retrievedObject).isNotNull()
                .isInstanceOf(List.class);
        List<ChatMessage> retrievedContext = (List<ChatMessage>) retrievedObject;
        assertThat(retrievedContext.get(0)).isEqualTo(chatMessages.get(0));
        assertThat(retrievedContext.get(1)).isEqualTo(chatMessages.get(1));
        assertThat(retrievedContext).hasSize(2);
        assertThat(retrievedContext.get(0)).isInstanceOf(UserMessage.class);
        assertThat(retrievedContext.get(1)).isInstanceOf(AiMessage.class);
    }

    @Test
    void redisTemplate2() {
        String sessionId = "session-cache";
        List<ChatMessage> chatMessages = List.of(
                new UserMessage("Spring Test 好用吗？"),
                new AiMessage("非常好用，特别是配合 Embedded Redis！")
        );
        ChatContextCache chatContextCache = new ChatContextCache(sessionId, chatMessages, 2);

        String redisKey = "chat_context:" + sessionId;
        // --- 2. 执行 (Act) ---
        // 将对象存入 Redis
        redisTemplate.opsForValue().set(redisKey, chatContextCache);
        // 从 Redis 中取出对象
        Object retrievedObject = redisTemplate.opsForValue().get(redisKey);
        // --- 3. 断言 (Assert) ---
        assertThat(retrievedObject).isNotNull()
                .isInstanceOf(ChatContextCache.class);
        ChatContextCache retrievedContext = (ChatContextCache) retrievedObject;
        assertThat(retrievedContext.getMessages().get(0)).isEqualTo(chatMessages.get(0));
        assertThat(retrievedContext.getMessages().get(1)).isEqualTo(chatMessages.get(1));
        assertThat(retrievedContext.getMessages()).hasSize(2);
        assertThat(retrievedContext.getMessages().get(0)).isInstanceOf(UserMessage.class);
        assertThat(retrievedContext.getMessages().get(1)).isInstanceOf(AiMessage.class);
    }

    @Test
    public void testChatContextCacheRedis() {
        String sessionId = "session-cache";
        List<ChatMessage> chatMessages = List.of(
                new UserMessage("Spring Test 好用吗？"),
                new AiMessage("非常好用，特别是配合 Embedded Redis！")
        );
        ChatContextCache chatContextCache = new ChatContextCache(sessionId, chatMessages, 2);

        String redisKey = "chat_context:" + sessionId;

        // 存储到 Redis
        redisTemplate.opsForValue().set(redisKey, chatContextCache);

        // 从 Redis 获取
        ChatContextCache retrievedContext = (ChatContextCache) redisTemplate.opsForValue().get(redisKey);

        // 断言
        assertThat(retrievedContext).isNotNull();
        assertThat(retrievedContext.getMemoryId()).isEqualTo(sessionId);
        assertThat(retrievedContext.getMessages()).hasSize(2);
        assertThat(retrievedContext.getContextSize()).isEqualTo(2);

        // 检查消息内容
        ChatMessage firstMessage = retrievedContext.getMessages().get(0);
        ChatMessage secondMessage = retrievedContext.getMessages().get(1);

        assertThat(firstMessage).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) firstMessage).singleText()).isEqualTo("Spring Test 好用吗？");

        assertThat(secondMessage).isInstanceOf(AiMessage.class);
        assertThat(((AiMessage) secondMessage).text()).isEqualTo("非常好用，特别是配合 Embedded Redis！");
    }

}