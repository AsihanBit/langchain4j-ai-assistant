package com.aiassist.mediassist;

import com.aiassist.mediassist.dto.entity.ChatMessageWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolExecutionResultMessageTest {

    @Test
    void testToolExecutionResultMessageSerialization() {
        // 创建原始的ToolExecutionResultMessage
        String toolId = "tool_123";
        String toolName = "calculator";
        String toolResult = "计算结果: 42";
        
        ToolExecutionResultMessage originalMessage = ToolExecutionResultMessage.from(toolId, toolName, toolResult);
        
        // 测试序列化
        ChatMessageWrapper.SerializableMessage serializedMessage = 
            ChatMessageWrapper.SerializableMessage.fromChatMessage(originalMessage, 1);
        
        assertEquals(ChatMessageWrapper.SerializableMessage.MessageType.TOOL_RESULT, serializedMessage.getType());
        assertTrue(serializedMessage.getContent().startsWith("ID:"));
        assertTrue(serializedMessage.getContent().contains("|NAME:"));
        assertTrue(serializedMessage.getContent().contains("|RESULT:"));
        
        // 测试反序列化
        ChatMessage deserializedMessage = serializedMessage.toChatMessage();
        assertTrue(deserializedMessage instanceof ToolExecutionResultMessage);
        
        ToolExecutionResultMessage resultMessage = (ToolExecutionResultMessage) deserializedMessage;
        assertEquals(toolId, resultMessage.id());
        assertEquals(toolName, resultMessage.toolName());
        assertEquals(toolResult, resultMessage.text());
    }

    @Test
    void testToolExecutionResultMessageInWrapper() {
        String memoryId = "test-tool-memory";
        String toolId = "tool_456";
        String toolName = "weather_api";
        String toolResult = "天气晴朗，温度25°C";
        
        ToolExecutionResultMessage toolMessage = ToolExecutionResultMessage.from(toolId, toolName, toolResult);
        
        // 创建包装类
        ChatMessageWrapper wrapper = ChatMessageWrapper.builder()
                .memoryId(memoryId)
                .maxMessageCount(10)
                .build();
        
        // 添加工具执行结果消息
        wrapper.addMessage(toolMessage);
        
        // 验证消息数量
        assertEquals(1, wrapper.getMessageCount());
        
        // 获取消息并验证
        ChatMessage retrievedMessage = wrapper.getChatMessages().get(0);
        assertTrue(retrievedMessage instanceof ToolExecutionResultMessage);
        
        ToolExecutionResultMessage retrievedToolMessage = (ToolExecutionResultMessage) retrievedMessage;
        assertEquals(toolId, retrievedToolMessage.id());
        assertEquals(toolName, retrievedToolMessage.toolName());
        assertEquals(toolResult, retrievedToolMessage.text());
    }

    @Test
    void testCompatibilityWithOldFormat() {
        // 测试兼容旧格式（只包含结果文本）
        ChatMessageWrapper.SerializableMessage oldFormatMessage = new ChatMessageWrapper.SerializableMessage();
        oldFormatMessage.setType(ChatMessageWrapper.SerializableMessage.MessageType.TOOL_RESULT);
        oldFormatMessage.setContent("简单的工具结果");
        
        // 应该能够正常反序列化
        ChatMessage deserializedMessage = oldFormatMessage.toChatMessage();
        assertTrue(deserializedMessage instanceof ToolExecutionResultMessage);
        
        ToolExecutionResultMessage resultMessage = (ToolExecutionResultMessage) deserializedMessage;
        assertEquals("unknown", resultMessage.id());
        assertEquals("unknown", resultMessage.toolName());
        assertEquals("简单的工具结果", resultMessage.text());
    }
}
