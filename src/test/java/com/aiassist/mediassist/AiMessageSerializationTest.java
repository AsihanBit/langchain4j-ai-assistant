package com.aiassist.mediassist;

import com.aiassist.mediassist.dto.entity.ChatMessageWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiMessageSerializationTest {

    @Test
    void testAiMessageWithToolCallSerialization() {
        // 创建包含工具调用的AI消息
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call_123")
                .name("get_user_ip_address")
                .arguments("{}")
                .build();
        
        AiMessage aiMessageWithTool = AiMessage.from(toolRequest);
        
        // 测试序列化
        ChatMessageWrapper.SerializableMessage serializedMessage = 
            ChatMessageWrapper.SerializableMessage.fromChatMessage(aiMessageWithTool, 1);
        
        assertNotNull(serializedMessage);
        assertEquals(ChatMessageWrapper.SerializableMessage.MessageType.AI, serializedMessage.getType());
        assertNotNull(serializedMessage.getContent());
        assertTrue(serializedMessage.getContent().startsWith("TOOL_CALL|"));
        assertTrue(serializedMessage.getContent().contains("ID:call_123"));
        assertTrue(serializedMessage.getContent().contains("NAME:get_user_ip_address"));
        assertTrue(serializedMessage.getContent().contains("ARGS:{}"));
        
        // 测试反序列化
        ChatMessage deserializedMessage = serializedMessage.toChatMessage();
        assertNotNull(deserializedMessage);
        assertTrue(deserializedMessage instanceof AiMessage);
        
        System.out.println("✅ AI消息工具调用序列化测试通过");
        System.out.println("序列化内容: " + serializedMessage.getContent());
        System.out.println("反序列化结果: " + deserializedMessage.toString());
        
        // 验证反序列化后的消息确实包含工具调用
        assertTrue(deserializedMessage instanceof AiMessage);
        AiMessage deserializedAiMessage = (AiMessage) deserializedMessage;
        assertTrue(deserializedAiMessage.hasToolExecutionRequests());
        assertEquals("call_123", deserializedAiMessage.toolExecutionRequests().get(0).id());
        assertEquals("get_user_ip_address", deserializedAiMessage.toolExecutionRequests().get(0).name());
        assertEquals("{}", deserializedAiMessage.toolExecutionRequests().get(0).arguments());
    }

    @Test
    void testAiMessageWithTextSerialization() {
        // 创建普通文本AI消息
        AiMessage aiMessageWithText = AiMessage.from("你好！我是AI助手");
        
        // 测试序列化
        ChatMessageWrapper.SerializableMessage serializedMessage = 
            ChatMessageWrapper.SerializableMessage.fromChatMessage(aiMessageWithText, 1);
        
        assertNotNull(serializedMessage);
        assertEquals(ChatMessageWrapper.SerializableMessage.MessageType.AI, serializedMessage.getType());
        assertEquals("你好！我是AI助手", serializedMessage.getContent());
        
        // 测试反序列化
        ChatMessage deserializedMessage = serializedMessage.toChatMessage();
        assertNotNull(deserializedMessage);
        assertTrue(deserializedMessage instanceof AiMessage);
        assertEquals("你好！我是AI助手", ((AiMessage) deserializedMessage).text());
        
        System.out.println("✅ AI消息文本序列化测试通过");
    }

    @Test
    void testToolExecutionResultSerialization() {
        // 创建工具执行结果消息
        ToolExecutionResultMessage toolResult = ToolExecutionResultMessage.from(
            "call_123", "get_user_ip_address", "用户IP地址: 192.168.1.111"
        );
        
        // 测试序列化
        ChatMessageWrapper.SerializableMessage serializedMessage = 
            ChatMessageWrapper.SerializableMessage.fromChatMessage(toolResult, 1);
        
        assertNotNull(serializedMessage);
        assertEquals(ChatMessageWrapper.SerializableMessage.MessageType.TOOL_RESULT, serializedMessage.getType());
        assertTrue(serializedMessage.getContent().startsWith("ID:"));
        assertTrue(serializedMessage.getContent().contains("NAME:get_user_ip_address"));
        assertTrue(serializedMessage.getContent().contains("RESULT:用户IP地址: 192.168.1.111"));
        
        // 测试反序列化
        ChatMessage deserializedMessage = serializedMessage.toChatMessage();
        assertNotNull(deserializedMessage);
        assertTrue(deserializedMessage instanceof ToolExecutionResultMessage);
        
        ToolExecutionResultMessage deserializedToolResult = (ToolExecutionResultMessage) deserializedMessage;
        assertEquals("call_123", deserializedToolResult.id());
        assertEquals("get_user_ip_address", deserializedToolResult.toolName());
        assertEquals("用户IP地址: 192.168.1.111", deserializedToolResult.text());
        
        System.out.println("✅ 工具执行结果序列化测试通过");
    }

    @Test
    void testCompleteMessageFlow() {
        // 测试完整的消息流程
        List<ChatMessage> messages = new ArrayList<>();
        
        // 用户消息
        messages.add(UserMessage.from("我的IP地址是多少？"));
        
        // AI消息（包含工具调用）
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call_123")
                .name("get_user_ip_address")
                .arguments("{}")
                .build();
        messages.add(AiMessage.from(toolRequest));
        
        // 工具执行结果
        messages.add(ToolExecutionResultMessage.from("call_123", "get_user_ip_address", "用户IP地址: 192.168.1.111"));
        
        // 创建包装类
        ChatMessageWrapper wrapper = ChatMessageWrapper.fromChatMessages("test-flow", messages, 50);
        
        // 验证包装类
        assertNotNull(wrapper);
        assertEquals(3, wrapper.getMessages().size());
        
        // 获取消息并验证
        List<ChatMessage> retrievedMessages = wrapper.getChatMessages();
        assertEquals(3, retrievedMessages.size());
        
        assertTrue(retrievedMessages.get(0) instanceof UserMessage);
        assertTrue(retrievedMessages.get(1) instanceof AiMessage);
        assertTrue(retrievedMessages.get(2) instanceof ToolExecutionResultMessage);
        
        System.out.println("✅ 完整消息流程测试通过");
        System.out.println("消息列表:");
        for (int i = 0; i < retrievedMessages.size(); i++) {
            ChatMessage msg = retrievedMessages.get(i);
            System.out.printf("%d. %s: %s%n", i + 1, msg.type(), msg.toString().substring(0, Math.min(100, msg.toString().length())));
        }
    }
}
