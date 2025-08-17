package com.aiassist.mediassist;

import com.aiassist.mediassist.dto.entity.Message;
import com.aiassist.mediassist.store.MongoChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class ToolCallOrderTest {

    @Autowired
    private MongoChatMemoryStore memoryStore;

    @Test
    void testToolCallMessageOrder() {
        // 创建包含工具调用的消息
        List<ChatMessage> messages = new ArrayList<>();
        
        // 1. 用户消息
        messages.add(dev.langchain4j.data.message.UserMessage.from("请获取我的IP地址"));
        
        // 2. AI消息（包含工具调用）
        messages.add(dev.langchain4j.data.message.AiMessage.from("我来帮你获取IP地址"));
        
        // 3. 工具执行结果消息
        messages.add(ToolExecutionResultMessage.from("call_1_get_user_ip_address", "get_user_ip_address", "用户IP地址: 192.168.1.111"));
        
        // 4. AI回复
        messages.add(dev.langchain4j.data.message.AiMessage.from("您的IP地址是: 192.168.1.111"));
        
        String memoryId = "test-tool-order-" + System.currentTimeMillis();
        
        // 更新消息
        memoryStore.updateMessages(memoryId, messages);
        
        // 获取消息并验证顺序
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
        
        System.out.println("✅ 工具调用消息顺序验证通过");
        System.out.println("消息列表:");
        for (int i = 0; i < retrievedMessages.size(); i++) {
            ChatMessage msg = retrievedMessages.get(i);
            System.out.printf("%d. %s: %s%n", i + 1, msg.type(), msg.toString().substring(0, Math.min(100, msg.toString().length())));
        }
    }

    @Test
    void testMultipleToolCalls() {
        // 测试多个工具调用的情况
        List<ChatMessage> messages = new ArrayList<>();
        
        // 用户消息
        messages.add(dev.langchain4j.data.message.UserMessage.from("请获取天气信息和我的位置"));
        
        // AI消息
        messages.add(dev.langchain4j.data.message.AiMessage.from("我来帮你获取这些信息"));
        
        // 第一个工具调用结果
        messages.add(ToolExecutionResultMessage.from("call_1_weather_api", "weather_api", "天气晴朗，温度25°C"));
        
        // 第二个工具调用结果
        messages.add(ToolExecutionResultMessage.from("call_2_location_api", "location_api", "当前位置: 北京市朝阳区"));
        
        // AI回复
        messages.add(dev.langchain4j.data.message.AiMessage.from("天气晴朗，温度25°C，您在北京市朝阳区"));
        
        String memoryId = "test-multiple-tools-" + System.currentTimeMillis();
        
        // 更新消息
        memoryStore.updateMessages(memoryId, messages);
        
        // 获取消息并验证
        List<ChatMessage> retrievedMessages = memoryStore.getMessages(memoryId);
        
        assertNotNull(retrievedMessages);
        assertEquals(5, retrievedMessages.size(), "应该有5条消息");
        
        // 验证工具调用结果消息的顺序
        ToolExecutionResultMessage weatherResult = (ToolExecutionResultMessage) retrievedMessages.get(2);
        ToolExecutionResultMessage locationResult = (ToolExecutionResultMessage) retrievedMessages.get(3);
        
        assertEquals("weather_api", weatherResult.toolName());
        assertEquals("location_api", locationResult.toolName());
        
        System.out.println("✅ 多个工具调用消息顺序验证通过");
    }
}
