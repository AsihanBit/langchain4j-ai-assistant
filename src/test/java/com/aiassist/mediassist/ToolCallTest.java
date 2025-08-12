package com.aiassist.mediassist;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class ToolCallTest {

    @Autowired
    private OpenAiAgent openAiAgent;

    @Test
    void testToolCall() {
        // 设置用户上下文
        UserContext.setCurrentUserIp("192.168.1.100");
        
        try {
            System.out.println("=== 测试工具调用 ===");
            
            // 测试简单对话（不触发工具）
            String response1 = openAiAgent.chat("test-user", "你好");
            System.out.println("简单对话: " + response1);
            
            // 测试工具调用
            String response2 = openAiAgent.chat("test-user", "请告诉我我的IP地址");
            System.out.println("工具调用: " + response2);
            
            // 测试加密工具
            String response3 = openAiAgent.chat("test-user", "请帮我加密密码123456");
            System.out.println("加密工具: " + response3);
            
        } finally {
            UserContext.clear();
        }
    }
}
