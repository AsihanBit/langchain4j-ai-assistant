package com.aiassist.ai.core.dto.res;

import lombok.Data;

import java.util.List;

@Data
public class OpenAiRes {

    private String id; // 响应 ID
    private String object; // 响应对象类型
    private long created; // 创建时间戳
    private String model; // 模型名称
    private String systemFingerprint;
    private List<Choice> choices; // 回复选项
    private Usage usage; // Token 使用情况

    // 内部类：Choice
    @Data
    public static class Choice {
        private int index; // 选项索引
        private Message message; // 消息内容
        private String finishReason; // 回复结束的原因
    }

    // 内部类：Message
    @Data
    public static class Message {
        private String role; // 消息角色
        private String content; // 消息内容
    }

    // 内部类：Usage
    @Data
    public static class Usage {
        private int promptTokens; // 提示词 Token 数量
        private int completionTokens; // 完成的 Token 数量
        private int totalTokens; // 总共的 Token 数量
    }
}
