package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.req.AgentReq;
import com.aiassist.mediassist.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private OpenAiAgent openAiAgent;

    @PostMapping("/message")
    public String chat(@RequestBody AgentReq req,
//                       @RequestParam(defaultValue = "anonymous") String userId,
                       HttpServletRequest request) {
        String userIp = IpUtils.getClientIp();
        // 调用 AI（工具现在可以通过 UserContext 获取 IP）
        return openAiAgent.chat(req.getMemoryId(), req.getMessage());
    }

    @PostMapping("/stream1")
    public void chatStream1(@RequestParam String message,
                           @RequestParam(defaultValue = "anonymous") String userId,
                           HttpServletRequest request) {

        // 设置用户上下文
        String userIp = IpUtils.getClientIp();

        // 流式响应（需要 WebFlux 支持）
        openAiAgent.chatStream(userId, message)
                .doOnNext(System.out::print)
                .blockLast();

    }


    @PostMapping("/stream")
    public String chatStream(@RequestBody AgentReq req,
                           HttpServletRequest request) {
        try {
            // 获取用户IP
            String userIp = IpUtils.getClientIp();

            // 设置用户上下文，让工具能够访问
            UserContext.setCurrentUserIp(userIp);
            UserContext.setCurrentMemoryId(req.getMemoryId());

            // 调用 AI
            return openAiAgent.chat(req.getMemoryId(), req.getMessage());

        } finally {
            // 清理 ThreadLocal
            UserContext.clear();
        }

    }

}
