package com.aiassist.mediassist.store;

import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.dto.entity.Message;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoChatMemoryStore 单元测试
 * 
 * 测试MongoDB聊天记忆存储的核心功能：
 * - 消息的存储和检索
 * - 会话的创建和管理
 * - LangChain4j消息格式转换
 * - 数据持久化
 */
@SpringBootTest
@ActiveProfiles("dev")
class MongoChatMemoryStoreTest {

    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    private String testMemoryId;
    private String testUserIp = "192.168.1.100";

    @BeforeEach
    void setUp() {
        // 生成唯一的测试memoryId
        testMemoryId = "test_" + UUID.randomUUID().toString().substring(0, 8);
        
        // 清理测试数据
        cleanupTestData();
    }

    /**
     * 测试基本的消息存储和检索功能
     */
    @Test
    void testBasicMessageStorageAndRetrieval() {
        // 准备测试数据
        List<ChatMessage> originalMessages = List.of(
            UserMessage.from("你好，我是用户"),
            AiMessage.from("您好！我是AI助手"),
            UserMessage.from("请介绍一下你自己"),
            AiMessage.from("我是一个医疗咨询AI助手，可以为您提供健康相关的建议")
        );

        // 存储消息
        mongoChatMemoryStore.updateMessages(testMemoryId, originalMessages);

        // 检索消息
        List<ChatMessage> retrievedMessages = mongoChatMemoryStore.getMessages(testMemoryId);

        // 验证结果
        assertNotNull(retrievedMessages, "检索的消息不应为null");
        assertEquals(4, retrievedMessages.size(), "消息数量应该匹配");
        
        // 验证消息内容和顺序
        assertEquals("你好，我是用户", getMessageText(retrievedMessages.get(0)));
        assertEquals("您好！我是AI助手", getMessageText(retrievedMessages.get(1)));
        assertEquals("请介绍一下你自己", getMessageText(retrievedMessages.get(2)));
        assertEquals("我是一个医疗咨询AI助手，可以为您提供健康相关的建议", getMessageText(retrievedMessages.get(3)));
        
        // 验证消息类型
        assertTrue(retrievedMessages.get(0) instanceof UserMessage);
        assertTrue(retrievedMessages.get(1) instanceof AiMessage);
        assertTrue(retrievedMessages.get(2) instanceof UserMessage);
        assertTrue(retrievedMessages.get(3) instanceof AiMessage);
    }

    /**
     * 测试会话自动创建功能
     */
    @Test
    void testConversationAutoCreation() {
        // 添加消息时应该自动创建会话
        mongoChatMemoryStore.addMessage(testMemoryId, "测试消息", "AI回复");

        // 验证会话是否创建
        Conversation conversation = mongoTemplate.findOne(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId)),
            Conversation.class
        );
        
        assertNotNull(conversation, "应该自动创建会话");
        assertEquals(testMemoryId, conversation.getMemoryId());
        assertNotNull(conversation.getCreatedTime());
        assertNotNull(conversation.getLastSendTime());
    }

    /**
     * 测试手动创建会话功能
     */
    @Test
    void testManualConversationCreation() {
        // 手动创建会话
        Conversation created = mongoChatMemoryStore.createConversation(testMemoryId, testUserIp);
        
        // 验证创建结果
        assertNotNull(created, "创建的会话不应为null");
        assertEquals(testMemoryId, created.getMemoryId());
        assertEquals(testUserIp, created.getUserIp());
        assertNotNull(created.getCreatedTime());
        assertNotNull(created.getLastSendTime());
        
        // 验证数据库中的记录
        Conversation fromDb = mongoTemplate.findOne(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId)),
            Conversation.class
        );
        
        assertNotNull(fromDb, "数据库中应该有会话记录");
        assertEquals(testMemoryId, fromDb.getMemoryId());
        assertEquals(testUserIp, fromDb.getUserIp());
    }

    /**
     * 测试消息添加功能
     */
    @Test
    void testAddMessage() {
        String userMessage = "我最近感觉头疼";
        String aiResponse = "头疼可能有多种原因，建议您详细描述症状";
        
        // 添加消息
        mongoChatMemoryStore.addMessage(testMemoryId, userMessage, aiResponse);
        
        // 验证消息是否正确存储
        List<ChatMessage> messages = mongoChatMemoryStore.getMessages(testMemoryId);
        
        assertEquals(2, messages.size(), "应该有2条消息（用户+AI）");
        assertEquals(userMessage, getMessageText(messages.get(0)));
        assertEquals(aiResponse, getMessageText(messages.get(1)));
        
        // 验证MongoDB中的原始数据
        List<Message> mongoMessages = mongoTemplate.findAll(Message.class);
        Message savedMessage = mongoMessages.stream()
            .filter(m -> testMemoryId.equals(m.getMemoryId()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(savedMessage, "MongoDB中应该有消息记录");
        assertEquals(testMemoryId, savedMessage.getMemoryId());
        assertEquals(Long.valueOf(1L), savedMessage.getTurnIndex());
        assertEquals(userMessage, savedMessage.getContent().getPrompt());
        assertEquals(aiResponse, savedMessage.getContent().getCompletion());
        assertNotNull(savedMessage.getSendTime());
    }

    /**
     * 测试消息删除功能
     */
    @Test
    void testDeleteMessages() {
        // 先添加一些消息
        mongoChatMemoryStore.addMessage(testMemoryId, "消息1", "回复1");
        mongoChatMemoryStore.addMessage(testMemoryId, "消息2", "回复2");
        
        // 验证消息存在
        List<ChatMessage> beforeDelete = mongoChatMemoryStore.getMessages(testMemoryId);
        assertEquals(4, beforeDelete.size(), "删除前应该有4条消息");
        
        // 删除消息
        mongoChatMemoryStore.deleteMessages(testMemoryId);
        
        // 验证消息已删除
        List<ChatMessage> afterDelete = mongoChatMemoryStore.getMessages(testMemoryId);
        assertEquals(0, afterDelete.size(), "删除后应该没有消息");
        
        // 验证会话也被删除
        Conversation conversation = mongoTemplate.findOne(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId)),
            Conversation.class
        );
        assertNull(conversation, "会话也应该被删除");
    }

    /**
     * 测试多轮对话的顺序保持
     */
    @Test
    void testMultipleRoundsOrder() {
        // 模拟多轮对话
        mongoChatMemoryStore.addMessage(testMemoryId, "第一轮用户消息", "第一轮AI回复");
        mongoChatMemoryStore.addMessage(testMemoryId, "第二轮用户消息", "第二轮AI回复");
        mongoChatMemoryStore.addMessage(testMemoryId, "第三轮用户消息", "第三轮AI回复");
        
        // 检索消息
        List<ChatMessage> messages = mongoChatMemoryStore.getMessages(testMemoryId);
        
        // 验证顺序和内容
        assertEquals(6, messages.size(), "应该有6条消息");
        assertEquals("第一轮用户消息", getMessageText(messages.get(0)));
        assertEquals("第一轮AI回复", getMessageText(messages.get(1)));
        assertEquals("第二轮用户消息", getMessageText(messages.get(2)));
        assertEquals("第二轮AI回复", getMessageText(messages.get(3)));
        assertEquals("第三轮用户消息", getMessageText(messages.get(4)));
        assertEquals("第三轮AI回复", getMessageText(messages.get(5)));
        
        // 验证MongoDB中的turn_index
        List<Message> mongoMessages = mongoTemplate.find(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId))
                .with(org.springframework.data.domain.Sort.by("turn_index")),
            Message.class
        );
        
        assertEquals(3, mongoMessages.size(), "MongoDB中应该有3条回合记录");
        assertEquals(Long.valueOf(1L), mongoMessages.get(0).getTurnIndex());
        assertEquals(Long.valueOf(2L), mongoMessages.get(1).getTurnIndex());
        assertEquals(Long.valueOf(3L), mongoMessages.get(2).getTurnIndex());
    }

    /**
     * 测试空消息处理
     */
    @Test
    void testEmptyMessageHandling() {
        // 测试空消息列表
        mongoChatMemoryStore.updateMessages(testMemoryId, List.of());
        List<ChatMessage> messages = mongoChatMemoryStore.getMessages(testMemoryId);
        assertEquals(0, messages.size(), "空消息列表应该返回空结果");
        
        // 测试不存在的memoryId
        List<ChatMessage> nonExistentMessages = mongoChatMemoryStore.getMessages("non_existent_id");
        assertEquals(0, nonExistentMessages.size(), "不存在的memoryId应该返回空结果");
    }

    /**
     * 测试数据持久化
     */
    @Test
    void testDataPersistence() {
        // 添加消息
        mongoChatMemoryStore.addMessage(testMemoryId, "持久化测试", "数据应该保存到MongoDB");
        
        // 直接查询MongoDB验证数据
        List<Message> mongoMessages = mongoTemplate.find(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").is(testMemoryId)),
            Message.class
        );
        
        assertEquals(1, mongoMessages.size(), "MongoDB中应该有1条记录");
        
        Message message = mongoMessages.get(0);
        assertEquals(testMemoryId, message.getMemoryId());
        assertEquals("持久化测试", message.getContent().getPrompt());
        assertEquals("数据应该保存到MongoDB", message.getContent().getCompletion());
        assertNotNull(message.getSendTime());
        assertTrue(message.getSendTime().isBefore(LocalDateTime.now().plusMinutes(1)));
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
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").regex("^test_")),
            Message.class
        );
        
        // 清理会话
        mongoTemplate.remove(
            Query.query(org.springframework.data.mongodb.core.query.Criteria.where("memory_id").regex("^test_")),
            Conversation.class
        );
    }
}
