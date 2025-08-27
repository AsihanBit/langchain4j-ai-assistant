package com.aiassist.chat.server.controller;

import com.aiassist.ai.core.ai.OpenAiClient;
import com.aiassist.chat.server.dto.req.ChatReq;
import com.aiassist.chat.server.dto.res.ConversationsRes;
import com.aiassist.chat.server.dto.res.CreateConversationRes;
import com.aiassist.chat.server.dto.res.GenerateTitleRes;
import com.aiassist.chat.server.result.Result;
import com.aiassist.ai.core.entity.Conversation;
import com.aiassist.ai.core.service.ConversationService;
import com.aiassist.chat.core.context.UserContext;
import com.aiassist.chat.core.utils.IpUtils;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话查询控制器
 * <p>
 * 提供根据IP和对话ID查询MongoDB中对话记录的功能
 * 独立的查询接口，专注于数据检索
 */
@Slf4j
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private OpenAiClient openAiClient;

    // ==================== 按IP查询对话 ====================

    /**
     * 根据用户IP查询所有对话
     */
    @GetMapping("/getConversationsByIp")
    public Result<ConversationsRes> getConversationsByIp() {
        try {
            String userIp = IpUtils.getClientIp();
            // TODO page, size
            // 构建查询条件
            List<Conversation> conversations = conversationService.getUserConversations(userIp);
            int totalCount = conversations.size();

//            log.info("查询完成 - 找到 {} 个对话，总共 {} 个", conversations.size(), totalCount);
            log.info("查询用户对话列表 - IP: {} 找到 {} 个对话", userIp, totalCount);

            ConversationsRes res = ConversationsRes.builder()
                    .conversations(conversations)
                    .totalCount(totalCount)
                    .build();
            return Result.success(res);

        } catch (Exception e) {
            log.error("查询用户对话列表失败 - IP: {}", UserContext.getCurrentUserIp(), e);
            throw new RuntimeException("查询失败");
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/createNew")
    public Result<CreateConversationRes> createNew(HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            Conversation newConversation = conversationService.createNewConversation(userIp);
            String memoryId = newConversation.getMemoryId();

            log.info("创建新会话 - IP: {}, memoryId: {}", userIp, memoryId);
            CreateConversationRes res = CreateConversationRes.builder()
                    .memoryId(memoryId)
                    .title(newConversation.getTitle())
                    .message("会话创建成功")
                    .build();
            return Result.success(res);

        } catch (Exception e) {
            log.error("创建新会话失败", e);
            throw new RuntimeException("创建新会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/getConversationInfo/{memoryId}")
    public Conversation getConversationInfo(@PathVariable String memoryId) {
        try {
            return conversationService.getConversation(memoryId);
        } catch (Exception e) {
            log.error("获取会话详情失败: memoryId={}", memoryId, e);
            return null;
        }
    }

    // 统计总数
//    .skip((long) page * size)
//    .limit(size);
//    long totalCount = mongoTemplate.count(
//    Query.query(Criteria.where("user_ip").is(userIp)),
//    Conversation.class
//    .totalPages((int) Math.ceil((double) totalCount / size))


    /**
     * 删除会话
     */
    @DeleteMapping("/delById/{memoryId}")
    public Result<String> deleteConversation(@PathVariable String memoryId) {
        try {
            conversationService.deleteConversation(memoryId);
            log.info("删除会话: memoryId={}", memoryId);
            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("删除会话失败: memoryId={}", memoryId, e);
            throw new RuntimeException("删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 生成新会话的标题
     */
    @PostMapping("/generateTitle")
    public Result<GenerateTitleRes> generateTitle(@RequestBody ChatReq chatReq) {
        try {
            ChatResponse chatRes = openAiClient.chat("请根据用户的第一条消息，给当前新会话创建一个标题。你只需要返回标题内容，不要返回其余的内容。以下是用户的第一条消息：" + chatReq.getMessage());
            String title = chatRes.aiMessage().text();

            log.info("生成标题 - memoryId: {}, title: {}", chatReq.getMemoryId(), title);
            // 更新数据库
            boolean changed = conversationService.updateConversationTitle(chatReq.getMemoryId(), title);

            if (!changed) {
                throw new RuntimeException("没有更新标题");
            }
            GenerateTitleRes res = GenerateTitleRes.builder()
                    .memoryId(chatReq.getMemoryId())
                    .title(title)
                    .build();

            return Result.success(res);
        } catch (Exception e) {
            log.error("生成会话标题", e);
            throw new RuntimeException("生成会话标题失败: " + e.getMessage());
        }
    }

    // ==================== 高级查询接口 ====================

    /*
      根据时间范围查询对话
      GET /api/conversation-query/conversations/by-time-range
      ISO格式: 2024-01-01T00:00:00
      page size
     */
}
