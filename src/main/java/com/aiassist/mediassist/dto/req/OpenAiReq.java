package com.aiassist.mediassist.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
//@AllArgsConstructor
@NoArgsConstructor
public class OpenAiReq {

    private String model; // 模型名称
    private List<Message> messages; // 消息列表

    // 内部消息类
    @Data
    @NoArgsConstructor
    public static class Message {
        private String role;    // 消息角色 （例如 "user"、"system"、"assistant"）
        private String content; // 消息内容

        // 构造方法
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    // 构造方法
    public OpenAiReq(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
}
