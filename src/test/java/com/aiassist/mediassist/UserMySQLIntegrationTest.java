package com.aiassist.mediassist;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.User;
import com.aiassist.mediassist.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class UserMySQLIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private OpenAiAgent openAiAgent;

    @Test
    void testCreateAndQueryUser() {
        // 测试创建用户
        String testUserName = "张三";
        String testIp = "192.168.1.100";
        
        User user = userService.createUser(testUserName, testIp);
        
        Assertions.assertNotNull(user);
        Assertions.assertNotNull(user.getId());
        Assertions.assertEquals(testUserName, user.getUserName());
        Assertions.assertEquals(testIp, user.getIpAddress());
        Assertions.assertEquals(1, user.getVisitCount());
        
        System.out.println("创建用户成功: " + user);
        
        // 测试查询用户
        User foundUser = userService.getUserByIpAddress(testIp);
        Assertions.assertNotNull(foundUser);
        Assertions.assertEquals(testUserName, foundUser.getUserName());
        
        System.out.println("查询用户成功: " + foundUser);
    }

    @Test
    void testUserVisitRecord() {
        String userName = "李四";
        String userIp = "192.168.1.101";
        
        // 第一次访问
        User user1 = userService.recordUserVisit(userName, userIp);
        Assertions.assertEquals(1, user1.getVisitCount());
        
        // 第二次访问
        User user2 = userService.recordUserVisit(userName, userIp);
        Assertions.assertEquals(2, user2.getVisitCount());
        
        System.out.println("用户访问记录测试通过，访问次数: " + user2.getVisitCount());
    }

    @Test
    void testAIUserManagement() {
        // 设置用户上下文
        UserContext.setCurrentUserIp("192.168.1.200");
        
        try {
            System.out.println("=== 测试AI用户管理 ===");
            
            // 第一次访问 - 检查身份
            String response1 = openAiAgent.chat("test-ai-user", "你好，你认识我吗？");
            System.out.println("首次访问: " + response1);
            
            // 自报姓名
            String response2 = openAiAgent.chat("test-ai-user", "我叫王五");
            System.out.println("自报姓名: " + response2);
            
            // 再次检查身份
            String response3 = openAiAgent.chat("test-ai-user", "现在你认识我了吗？");
            System.out.println("再次检查: " + response3);
            
            // 查看所有用户
            String response4 = openAiAgent.chat("test-ai-user", "你认识哪些用户？");
            System.out.println("用户列表: " + response4);
            
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void testDruidConnectionPool() {
        // 测试连接池
        System.out.println("测试Druid连接池...");
        
        int userCount = userService.countUsers();
        System.out.println("当前用户总数: " + userCount);
        
        var allUsers = userService.getAllUsers();
        System.out.println("所有用户:");
        allUsers.forEach(user -> 
            System.out.println("- " + user.getUserName() + " (" + user.getIpAddress() + ") 访问 " + user.getVisitCount() + " 次")
        );
        
        Assertions.assertTrue(userCount >= 0);
        System.out.println("Druid连接池测试通过！");
    }

    @Test 
    void testTokenUsage() {
        // 测试token使用量功能
        String userName = "Token测试用户";
        String userIp = "192.168.1.150";
        
        User user = userService.createUser(userName, userIp);
        
        // 增加token使用量
        boolean result = userService.addTokenUsage(user.getId(), 100L);
        Assertions.assertTrue(result);
        
        // 查询用户验证token使用量
        User updatedUser = userService.getUserById(user.getId());
        Assertions.assertEquals(100L, updatedUser.getTokenUsage());
        
        System.out.println("Token使用量测试通过: " + updatedUser.getTokenUsage());
    }
}
