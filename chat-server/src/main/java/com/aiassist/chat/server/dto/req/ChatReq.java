package com.aiassist.chat.server.dto.req;

import lombok.Data;

@Data
public class ChatReq {
    private String memoryId;
    private String message;
}
