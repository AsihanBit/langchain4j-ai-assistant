package com.aiassist.mediassist.config;

import com.aiassist.mediassist.store.MongoChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoChatMemoryConfiguration 集成测试
 * 
 * 测试ChatMemoryProvider的配置和集成：
 * - Bean的正确注入
 * - MessageWindowChatMemory的创建
 * - MongoDB存储的集成
 * - 消息窗口限制功能
 */
@SpringBootTest
@ActiveProfiles("dev")
class MongoChatMemoryConfigurationTest {

    @Autowired
    @Qualifier("chatMemoryProviderOpenAi")
    private ChatMemoryProvider chatMemoryProvider;
    
    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    private String testMemoryId;

    @BeforeEach
    void setUp() {
        testMemoryId = "config_test_" + UUID.randomUUID().toString().substring(0, 8);
        cleanupTestData();
    }

    /**
     * 测试ChatMemoryProvider Bean是否正确注入
     */
    @Test
    void testChatMemoryProviderBeanInjection() {
        assertNotNull(chatMemoryProvider, "ChatMemoryProvider应该被正确注入");
        assertNotNull(mongoChatMemoryStore, "MongoChatMemoryStore应该被正确注入");
    }

    /**
     * 测试MessageWindowChatMemory的创建
     */
    @Test
    void testMessageWindowChatMemoryCreation() {
        // 通过Provider创建ChatMemory实例
        var chatMemory = chatMemoryProvider.get(testMemoryId);
        
        assertNotNull(chatMemory, "ChatMemory实例应该被创建");
        assertTrue(chatMemory instanceof MessageWindowChatMemory, 
                  "应该创建MessageWindowChatMemory实例");
    }

    /**
     * 测试消息存储和检索的完整流程
     */
    @Test
    void testFullMessageStorageFlow() {
        // 获取ChatMemory实例
        var chatMemory = chatMemoryProvider.get(testMemoryId);
        
        // 添加消息
        chatMemory.add(UserMessage.from("配置测试：你好"));
        chatMemory.add(AiMessage.from("配置测试：您好，我是AI助手"));
        chatMemory.add(UserMessage.from("配置测试：请介绍一下自己"));
        chatMemory.add(AiMessage.from("配置测试：我是一个医疗咨询AI"));
        
        // 检索消息
        List<ChatMessage> messages = chatMemory.messages();
        
        // 验证消息
        assertEquals(4, messages.size(), "应该有4条消息");
        assertEquals("配置测试：你好", getMessageText(messages.get(0)));
        assertEquals("配置测试：您好，我是AI助手", getMessageText(messages.get(1)));
        assertEquals("配置测试：请介绍一下自己", getMessageText(messages.get(2)));
        assertEquals("配置测试：我是一个医疗咨询AI", getMessageText(messages.get(3)));
    }

    /**
     * 测试消息窗口限制功能
     */
    @Test
    void testMessageWindowLimit() {
        var chatMemory = chatMemoryProvider.get(testMemoryId);

        // 添加超过窗口限制的消息（假设限制为10条）
        for (int i = 1; i <= 15; i++) {
            chatMemory.add(UserMessage.from("用户消息 " + i));
            chatMemory.add(AiMessage.from("AI回复 " + i));
        }

        // 检索消息
        List<ChatMessage> messages = chatMemory.messages();

        // 验证消息数量不超过配置的限制
        assertTrue(messages.size() <= 10,
                  "消息数量应该不超过配置的窗口限制，实际数量: " + messages.size());

        // 验证保留的是最新的消息
        if (messages.size() > 0) {
            String lastMessage = getMessageText(messages.get(messages.size() - 1));
            assertTrue(lastMessage.contains("15"), "应该保留最新的消息");
        }
    }

    /**
     * 测试多个memoryId的隔离
     */
    @Test
    void testMemoryIdIsolation() {
        String memoryId1 = testMemoryId + "_1";
        String memoryId2 = testMemoryId + "_2";
        
        // 为不同的memoryId创建ChatMemory
        var chatMemory1 = chatMemoryProvider.get(memoryId1);
        var chatMemory2 = chatMemoryProvider.get(memoryId2);
        
        // 分别添加不同的消息
        chatMemory1.add(UserMessage.from("会话1的消息"));
        chatMemory1.add(AiMessage.from("会话1的回复"));
        
        chatMemory2.add(UserMessage.from("会话2的消息"));
        chatMemory2.add(AiMessage.from("会话2的回复"));
        
        // 验证消息隔离
        List<ChatMessage> messages1 = chatMemory1.messages();
        List<ChatMessage> messages2 = chatMemory2.messages();
        
        assertEquals(2, messages1.size(), "会话1应该有2条消息");
        assertEquals(2, messages2.size(), "会话2应该有2条消息");
        
        assertEquals("会话1的消息", getMessageText(messages1.get(0)));
        assertEquals("会话2的消息", getMessageText(messages2.get(0)));
        
        // 验证消息内容不混淆
        assertNotEquals(getMessageText(messages1.get(0)), getMessageText(messages2.get(0)), 
                       "不同会话的消息应该不同");
    }

    /**
     * 测试数据持久化到MongoDB
     */
    @Test
    void testMongoDbPersistence() {
//        var chatMemory = chatMemoryProvider.get(testMemoryId);
        ChatMemory chatMemory = chatMemoryProvider.get(testMemoryId);

        // 添加消息
        chatMemory.add(UserMessage.from("持久化测试消息"));
        chatMemory.add(AiMessage.from("持久化测试回复"));
        
        // 直接从MongoDB验证数据
        var mongoMessages = mongoTemplate.find(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId)),
            com.aiassist.mediassist.dto.entity.Message.class
        );
        
        assertEquals(1, mongoMessages.size(), "MongoDB中应该有1条记录（一个回合）");
        
        var message = mongoMessages.get(0);
        assertEquals(testMemoryId, message.getMemoryId());
        assertEquals("持久化测试消息", message.getContent().getPrompt());
        assertEquals("持久化测试回复", message.getContent().getCompletion());
    }

    /**
     * 测试ChatMemory的清除功能
     */
    @Test
    void testChatMemoryClear() {
        var chatMemory = chatMemoryProvider.get(testMemoryId);
        
        // 添加消息
        chatMemory.add(UserMessage.from("将被清除的消息"));
        chatMemory.add(AiMessage.from("将被清除的回复"));
        
        // 验证消息存在
        assertEquals(2, chatMemory.messages().size(), "清除前应该有2条消息");
        
        // 清除消息
        chatMemory.clear();
        
        // 验证消息已清除
        assertEquals(0, chatMemory.messages().size(), "清除后应该没有消息");
        
        // 验证MongoDB中的数据也被清除
        var mongoMessages = mongoTemplate.find(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId)),
            com.aiassist.mediassist.dto.entity.Message.class
        );
        
        assertEquals(0, mongoMessages.size(), "MongoDB中也应该没有消息");
    }

    /**
     * 测试ChatMemory的获取ID功能
     */
    @Test
    void testChatMemoryId() {
        var chatMemory = chatMemoryProvider.get(testMemoryId);
        
        // 验证ID
        assertEquals(testMemoryId, chatMemory.id(), "ChatMemory的ID应该匹配");
    }

    /**
     * 获取消息文本内容的辅助方法
     */
    private String getMessageText(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        }
        return "";
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        // 清理消息
        mongoTemplate.remove(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").regex("^config_test_")),
            com.aiassist.mediassist.dto.entity.Message.class
        );
        
        // 清理会话
        mongoTemplate.remove(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").regex("^config_test_")),
            com.aiassist.mediassist.dto.entity.Conversation.class
        );
    }
}
