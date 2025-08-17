package com.aiassist.mediassist;

import com.aiassist.mediassist.dto.entity.ChatMessageWrapper;
import com.aiassist.mediassist.service.ChatMessageCacheService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class ChatMessageCacheTest {

    @Autowired
    private ChatMessageCacheService cacheService;

    @Test
    void testCacheBasicOperations() {
        String memoryId = "test-memory-" + System.currentTimeMillis();
        
        // 1. 测试空缓存
        List<ChatMessage> emptyResult = cacheService.getMessages(memoryId);
        assertNull(emptyResult, "空缓存应该返回null");
        
        // 2. 测试添加消息
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("你好"));
        messages.add(AiMessage.from("你好！我是AI助手，有什么可以帮助你的吗？"));
        messages.add(UserMessage.from("请介绍一下你自己"));
        messages.add(AiMessage.from("我是一个AI助手，可以回答问题、提供帮助。"));
        
        cacheService.updateMessages(memoryId, messages);
        
        // 3. 测试获取消息
        List<ChatMessage> cachedMessages = cacheService.getMessages(memoryId);
        assertNotNull(cachedMessages, "应该能获取到缓存的消息");
        assertEquals(4, cachedMessages.size(), "消息数量应该匹配");
        
        // 4. 验证消息内容
        assertTrue(cachedMessages.get(0) instanceof UserMessage, "第一条应该是用户消息");
        assertTrue(cachedMessages.get(1) instanceof AiMessage, "第二条应该是AI消息");
        
        // 5. 测试添加单条消息
        cacheService.addMessage(memoryId, UserMessage.from("谢谢"));
        List<ChatMessage> updatedMessages = cacheService.getMessages(memoryId);
        assertEquals(5, updatedMessages.size(), "添加单条消息后数量应该增加");
        
        // 6. 测试缓存存在性
        assertTrue(cacheService.exists(memoryId), "缓存应该存在");
        
        // 7. 测试删除缓存
        cacheService.deleteMessages(memoryId);
        assertFalse(cacheService.exists(memoryId), "删除后缓存应该不存在");
        
        // 8. 测试删除后获取
        List<ChatMessage> deletedResult = cacheService.getMessages(memoryId);
        assertNull(deletedResult, "删除后应该返回null");
    }

    @Test
    void testCacheWrapperSerialization() {
        String memoryId = "test-wrapper-" + System.currentTimeMillis();
        
        // 创建测试消息
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("测试用户消息"));
        messages.add(AiMessage.from("测试AI回复"));
        
        // 测试包装类创建
        ChatMessageWrapper wrapper = ChatMessageWrapper.fromChatMessages(memoryId, messages, 10);
        assertEquals(memoryId, wrapper.getMemoryId());
        assertEquals(10, wrapper.getMaxMessageCount());
        assertEquals(2, wrapper.getMessageCount());
        
        // 测试消息转换
        List<ChatMessage> convertedMessages = wrapper.getChatMessages();
        assertEquals(2, convertedMessages.size());
        assertTrue(convertedMessages.get(0) instanceof UserMessage);
        assertTrue(convertedMessages.get(1) instanceof AiMessage);
        
        // 测试添加消息
        wrapper.addMessage(UserMessage.from("新消息"));
        assertEquals(3, wrapper.getMessageCount());
        
        // 测试LRU淘汰（设置最大数量为2）
        wrapper.setMaxMessageCount(2);
        wrapper.addMessage(AiMessage.from("超过限制的消息"));
        assertEquals(2, wrapper.getMessageCount(), "应该淘汰最早的消息");
    }

    @Test
    void testCachePerformance() {
        String memoryId = "test-performance-" + System.currentTimeMillis();
        
        // 创建大量消息测试性能
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            messages.add(UserMessage.from("用户消息 " + i));
            messages.add(AiMessage.from("AI回复 " + i));
        }
        
        long startTime = System.currentTimeMillis();
        cacheService.updateMessages(memoryId, messages);
        long updateTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        List<ChatMessage> cachedMessages = cacheService.getMessages(memoryId);
        long getTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(cachedMessages);
        assertEquals(200, cachedMessages.size());
        
        System.out.println("更新缓存耗时: " + updateTime + "ms");
        System.out.println("获取缓存耗时: " + getTime + "ms");
        
        // 性能要求：更新和获取都应该在合理时间内完成
        assertTrue(updateTime < 1000, "更新缓存应该在1秒内完成");
        assertTrue(getTime < 500, "获取缓存应该在500ms内完成（首次访问可能需要建立连接）");
    }
}
