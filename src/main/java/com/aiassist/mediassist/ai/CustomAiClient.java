package com.aiassist.mediassist.ai;

import com.aiassist.mediassist.dto.req.OpenAiReq;
import com.aiassist.mediassist.dto.res.OpenAiRes;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class CustomAiClient {
    // RestTemplate 客户端
    private final RestTemplate restTemplate = new RestTemplate();
    // API 的基础 URL
    private final String aiUrl = "https:// /v1/chat/completions";
    // API Key
    private final String apiKey = "";

    public String sendText(String userText){
        // 1 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // 设置 Content-Type 为 application/json
        headers.set("Authorization", "Bearer " + apiKey);  // 设置 Authorization Bearer Token
        // 2 构建请求体
        OpenAiReq requestBody = new OpenAiReq(
                "gpt-4o-mini", // 模型名称
                Arrays.asList(
                        new OpenAiReq.Message("system", "你是一个有帮助的助手。"),
                        new OpenAiReq.Message("user", userText)
                )
        );
        // 3 创建请求实体
        HttpEntity<OpenAiReq> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            // 4 发送 POST 请求到 OpenAI 的 API
            ResponseEntity<OpenAiRes> response = restTemplate.exchange(
                    aiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    OpenAiRes.class
            );
            // 5 解析响应数据
            OpenAiRes responseBody = response.getBody();
            if (responseBody != null && responseBody.getChoices() != null && !responseBody.getChoices().isEmpty()) {
                // 提取第一个 Choice 的回复内容
                OpenAiRes.Choice firstChoice = responseBody.getChoices().get(0);
                return firstChoice.getMessage().getContent();
            } else {
                return "AI 返回了空的响应内容。";
            }
        } catch (Exception e) {
            // 捕获异常，打印错误日志，并返回友好的消息
            e.printStackTrace();
            return "调用 AI 接口时发生错误：" + e.getMessage();
        }
    }
}
