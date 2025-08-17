package com.aiassist.mediassist;

import com.aiassist.mediassist.store.MongoChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class ToolCallIntegrationTest {

    @Autowired
    private MongoChatMemoryStore memoryStore;

    @Test
    void testNormalMessageFlow() {
        // 测试普通消息流程（不包含工具调用）
        List<ChatMessage> messages = new ArrayList<>();
        
        // 用户消息
        messages.add(dev.langchain4j.data.message.UserMessage.from("你好"));
        
        // AI回复
        messages.add(dev.langchain4j.data.message.AiMessage.from("你好！我是AI助手，有什么可以帮助你的吗？"));
        
        String memoryId = "test-normal-flow-" + System.currentTimeMillis();
        
        // 更新消息
        memoryStore.updateMessages(memoryId, messages);
        
        // 获取消息并验证
        List<ChatMessage> retrievedMessages = memoryStore.getMessages(memoryId);
        
        assertNotNull(retrievedMessages);
        assertEquals(2, retrievedMessages.size(), "应该有2条消息");
        
        // 验证消息类型
        assertTrue(retrievedMessages.get(0) instanceof dev.langchain4j.data.message.UserMessage, "第1条应该是用户消息");
        assertTrue(retrievedMessages.get(1) instanceof dev.langchain4j.data.message.AiMessage, "第2条应该是AI消息");
        
        System.out.println("✅ 普通消息流程验证通过");
        System.out.println("消息列表:");
        for (int i = 0; i < retrievedMessages.size(); i++) {
            ChatMessage msg = retrievedMessages.get(i);
            System.out.printf("%d. %s: %s%n", i + 1, msg.type(), msg.toString().substring(0, Math.min(100, msg.toString().length())));
        }
    }

    @Test
    void testToolCallFlow() {
        // 测试工具调用流程
        List<ChatMessage> messages = new ArrayList<>();
        
        // 用户消息
        messages.add(dev.langchain4j.data.message.UserMessage.from("请获取我的IP地址"));
        
        // AI消息（发起工具调用）
        dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest = 
            dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                .id("call_1_get_user_ip_address")
                .name("get_user_ip_address")
                .arguments("{}")
                .build();
        messages.add(dev.langchain4j.data.message.AiMessage.from(toolRequest));
        
        // 工具执行结果
        messages.add(ToolExecutionResultMessage.from("call_1_get_user_ip_address", "get_user_ip_address", "用户IP地址: 192.168.1.111"));
        
        // AI最终回复
        messages.add(dev.langchain4j.data.message.AiMessage.from("您的IP地址是: 192.168.1.111"));
        
        String memoryId = "test-tool-flow-" + System.currentTimeMillis();
        
        // 更新消息
        memoryStore.updateMessages(memoryId, messages);
        
        // 获取消息并验证
        List<ChatMessage> retrievedMessages = memoryStore.getMessages(memoryId);
        
        assertNotNull(retrievedMessages);
        assertEquals(4, retrievedMessages.size(), "应该有4条消息");
        
        // 验证消息类型和顺序
        assertTrue(retrievedMessages.get(0) instanceof dev.langchain4j.data.message.UserMessage, "第1条应该是用户消息");
        assertTrue(retrievedMessages.get(1) instanceof dev.langchain4j.data.message.AiMessage, "第2条应该是AI消息");
        assertTrue(retrievedMessages.get(2) instanceof ToolExecutionResultMessage, "第3条应该是工具执行结果消息");
        assertTrue(retrievedMessages.get(3) instanceof dev.langchain4j.data.message.AiMessage, "第4条应该是AI消息");
        
        // 验证工具执行结果消息的内容
        ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) retrievedMessages.get(2);
        assertEquals("call_1_get_user_ip_address", toolResult.id());
        assertEquals("get_user_ip_address", toolResult.toolName());
        assertEquals("用户IP地址: 192.168.1.111", toolResult.text());
        
        System.out.println("✅ 工具调用流程验证通过");
        System.out.println("消息列表:");
        for (int i = 0; i < retrievedMessages.size(); i++) {
            ChatMessage msg = retrievedMessages.get(i);
            System.out.printf("%d. %s: %s%n", i + 1, msg.type(), msg.toString().substring(0, Math.min(100, msg.toString().length())));
        }
    }

    @Test
    void testMultipleConversations() {
        // 测试多个会话的情况
        String memoryId1 = "test-multi-1-" + System.currentTimeMillis();
        String memoryId2 = "test-multi-2-" + System.currentTimeMillis();
        
        // 第一个会话：普通消息
        List<ChatMessage> messages1 = new ArrayList<>();
        messages1.add(dev.langchain4j.data.message.UserMessage.from("你好"));
        messages1.add(dev.langchain4j.data.message.AiMessage.from("你好！"));
        
        // 第二个会话：工具调用
        List<ChatMessage> messages2 = new ArrayList<>();
        messages2.add(dev.langchain4j.data.message.UserMessage.from("获取IP"));
        
        // AI消息（发起工具调用）
        dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest2 = 
            dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                .id("call_1_ip")
                .name("get_ip")
                .arguments("{}")
                .build();
        messages2.add(dev.langchain4j.data.message.AiMessage.from(toolRequest2));
        
        messages2.add(ToolExecutionResultMessage.from("call_1_ip", "get_ip", "IP: 127.0.0.1"));
        messages2.add(dev.langchain4j.data.message.AiMessage.from("您的IP是: 127.0.0.1"));
        
        // 更新两个会话
        memoryStore.updateMessages(memoryId1, messages1);
        memoryStore.updateMessages(memoryId2, messages2);
        
        // 验证两个会话
        List<ChatMessage> retrieved1 = memoryStore.getMessages(memoryId1);
        List<ChatMessage> retrieved2 = memoryStore.getMessages(memoryId2);
        
        assertEquals(2, retrieved1.size(), "第一个会话应该有2条消息");
        assertEquals(4, retrieved2.size(), "第二个会话应该有4条消息");
        
        System.out.println("✅ 多会话测试验证通过");
    }
}
