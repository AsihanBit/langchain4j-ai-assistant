package com.aiassist.mediassist.dto.req.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ChatReq {
    private String memoryId;
    private String message;
}
